const Redis = require('ioredis');
const env = require('../config/env');

const redisUrl = env.redis.url || `redis://${env.redis.host}:${env.redis.port}`;
const logClient = new Redis(redisUrl);
const deadLetterClient = new Redis(redisUrl);

const LOG_LIST_KEY = env.redis.logList;
const LOG_RETENTION = env.redis.logRetention;
const DEAD_LETTER_KEY = env.redis.deadLetter;

logClient.on('error', (err) => console.error('Redis log client error:', err));
deadLetterClient.on('error', (err) => console.error('Redis dead-letter client error:', err));

function buildLogEntry(entry) {
  return JSON.stringify({
    timestamp: Date.now(),
    ...entry,
  });
}

async function logEvent(entry) {
  await logClient.lpush(LOG_LIST_KEY, buildLogEntry(entry));
  await logClient.ltrim(LOG_LIST_KEY, 0, LOG_RETENTION - 1);
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
  await deadLetterClient.hdel(DEAD_LETTER_KEY, job.id);
  await logEvent({
    status: 'DELIVERED',
    jobId: job.id,
    receiverKey: job.receiverKey,
    event: job.event,
    attempts: job.attempts || 0,
  });
}

async function recordFailure(job, reason) {
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
}

async function getRecentLogs(limit = 100) {
  const entries = await logClient.lrange(LOG_LIST_KEY, 0, limit - 1);
  return entries.map((entry) => {
    try {
      return JSON.parse(entry);
    } catch (error) {
      return { raw: entry };
    }
  });
}

async function getFailedJobs() {
  const entries = await deadLetterClient.hgetall(DEAD_LETTER_KEY);
  return Object.entries(entries).map(([jobId, raw]) => {
    try {
      return { jobId, ...JSON.parse(raw) };
    } catch (error) {
      return { jobId, raw };
    }
  });
}

async function getFailedJob(jobId) {
  const raw = await deadLetterClient.hget(DEAD_LETTER_KEY, jobId);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    return { raw };
  }
}

async function removeFailedJob(jobId) {
  await deadLetterClient.hdel(DEAD_LETTER_KEY, jobId);
}

async function closeMonitorConnections() {
  await Promise.allSettled([logClient.quit(), deadLetterClient.quit()]);
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
