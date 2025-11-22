const Redis = require('ioredis');
const env = require('../config/env');

const redisUrl = env.redis.url || `redis://${env.redis.host}:${env.redis.port}`;
const LOG_LIST_KEY = env.redis.logList;
const LOG_RETENTION = env.redis.logRetention;
const DEAD_LETTER_KEY = env.redis.deadLetter;

let logClient = null;
let deadLetterClient = null;
let monitorAvailable = false;

// Redis 연결 초기화 (lazy)
function initMonitorRedis() {
  if (logClient && deadLetterClient) {
    return;
  }

  try {
    logClient = new Redis(redisUrl, {
      maxRetriesPerRequest: 3,
      retryStrategy: (times) => {
        if (times > 3) {
          monitorAvailable = false;
          return null;
        }
        return Math.min(times * 50, 2000);
      },
      lazyConnect: true,
    });

    deadLetterClient = new Redis(redisUrl, {
      maxRetriesPerRequest: 3,
      retryStrategy: (times) => {
        if (times > 3) {
          monitorAvailable = false;
          return null;
        }
        return Math.min(times * 50, 2000);
      },
      lazyConnect: true,
    });

    logClient.on('error', (err) => {
      if (err.code !== 'ECONNREFUSED') {
        console.error('Redis log client error:', err.message);
      }
      monitorAvailable = false;
    });

    deadLetterClient.on('error', (err) => {
      if (err.code !== 'ECONNREFUSED') {
        console.error('Redis dead-letter client error:', err.message);
      }
      monitorAvailable = false;
    });

    logClient.on('connect', () => {
      monitorAvailable = true;
    });

    deadLetterClient.on('connect', () => {
      monitorAvailable = true;
    });

    logClient.connect().catch(() => {
      monitorAvailable = false;
    });
    deadLetterClient.connect().catch(() => {
      monitorAvailable = false;
    });
  } catch (error) {
    monitorAvailable = false;
  }
}

function buildLogEntry(entry) {
  return JSON.stringify({
    timestamp: Date.now(),
    ...entry,
  });
}

async function logEvent(entry) {
  if (!logClient) {
    initMonitorRedis();
  }

  if (!monitorAvailable || !logClient) {
    return;
  }

  try {
    await logClient.lpush(LOG_LIST_KEY, buildLogEntry(entry));
    await logClient.ltrim(LOG_LIST_KEY, 0, LOG_RETENTION - 1);
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      monitorAvailable = false;
    }
  }
}

async function recordEnqueued(job) {
  await logEvent({
    status: 'ENQUEUED',
    jobId: job.id,
    receiverKey: job.receiverKey,
    event: job.event,
    attempts: job.attempts || 0,
  });
}

async function recordDelivered(job) {
  if (!deadLetterClient) {
    initMonitorRedis();
  }

  if (!monitorAvailable || !deadLetterClient) {
    return;
  }

  try {
    await deadLetterClient.hdel(DEAD_LETTER_KEY, job.id);
    await logEvent({
      status: 'DELIVERED',
      jobId: job.id,
      receiverKey: job.receiverKey,
      event: job.event,
      attempts: job.attempts || 0,
    });
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      monitorAvailable = false;
    }
  }
}

async function recordFailure(job, reason) {
  if (!deadLetterClient) {
    initMonitorRedis();
  }

  if (!monitorAvailable || !deadLetterClient) {
    return;
  }

  try {
    const payload = JSON.stringify({
      ...job,
      reason,
      failedAt: Date.now(),
    });
    await deadLetterClient.hset(DEAD_LETTER_KEY, job.id, payload);
    await logEvent({
      status: 'FAILED',
      jobId: job.id,
      receiverKey: job.receiverKey,
      event: job.event,
      attempts: job.attempts || 0,
      reason,
    });
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      monitorAvailable = false;
    }
  }
}

async function getRecentLogs(limit = 100) {
  if (!logClient) {
    initMonitorRedis();
  }

  if (!monitorAvailable || !logClient) {
    return [];
  }

  try {
    const entries = await logClient.lrange(LOG_LIST_KEY, 0, limit - 1);
    return entries.map((entry) => {
      try {
        return JSON.parse(entry);
      } catch (error) {
        return { raw: entry };
      }
    });
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      monitorAvailable = false;
    }
    return [];
  }
}

async function getFailedJobs() {
  if (!deadLetterClient) {
    initMonitorRedis();
  }

  if (!monitorAvailable || !deadLetterClient) {
    return [];
  }

  try {
    const entries = await deadLetterClient.hgetall(DEAD_LETTER_KEY);
    return Object.entries(entries).map(([jobId, raw]) => {
      try {
        return { jobId, ...JSON.parse(raw) };
      } catch (error) {
        return { jobId, raw };
      }
    });
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      monitorAvailable = false;
    }
    return [];
  }
}

async function getFailedJob(jobId) {
  if (!deadLetterClient) {
    initMonitorRedis();
  }

  if (!monitorAvailable || !deadLetterClient) {
    return null;
  }

  try {
    const raw = await deadLetterClient.hget(DEAD_LETTER_KEY, jobId);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw);
    } catch (error) {
      return { raw };
    }
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      monitorAvailable = false;
    }
    return null;
  }
}

async function removeFailedJob(jobId) {
  if (!deadLetterClient) {
    initMonitorRedis();
  }

  if (!monitorAvailable || !deadLetterClient) {
    return;
  }

  try {
    await deadLetterClient.hdel(DEAD_LETTER_KEY, jobId);
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      monitorAvailable = false;
    }
  }
}

async function closeMonitorConnections() {
  if (logClient) {
    await Promise.allSettled([logClient.quit()]);
  }
  if (deadLetterClient) {
    await Promise.allSettled([deadLetterClient.quit()]);
  }
}

module.exports = {
  recordEnqueued,
  recordDelivered,
  recordFailure,
  getRecentLogs,
  getFailedJobs,
  getFailedJob,
  removeFailedJob,
  closeMonitorConnections,
};
