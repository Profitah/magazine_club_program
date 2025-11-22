const Redis = require('ioredis');
const env = require('../config/env');

const redisUrl = env.redis.url || `redis://${env.redis.host}:${env.redis.port}`;
const scheduleSetKey = env.redis.scheduleSet;

let schedulerClient = null;
let schedulerAvailable = false;

// Redis 연결 초기화 (lazy)
function initSchedulerRedis() {
  if (schedulerClient) {
    return;
  }

  try {
    schedulerClient = new Redis(redisUrl, {
      maxRetriesPerRequest: 3,
      retryStrategy: (times) => {
        if (times > 3) {
          console.warn('⚠️ Redis 연결 실패 - 스케줄러 기능이 비활성화됩니다.');
          schedulerAvailable = false;
          return null;
        }
        return Math.min(times * 50, 2000);
      },
      lazyConnect: true,
    });

    schedulerClient.on('error', (err) => {
      if (err.code !== 'ECONNREFUSED') {
        console.error('Redis scheduler client error:', err.message);
      }
      schedulerAvailable = false;
    });

    schedulerClient.on('connect', () => {
      console.log('✅ Redis scheduler 연결 성공');
      schedulerAvailable = true;
    });

    schedulerClient.connect().catch(() => {
      console.warn('⚠️ Redis 연결 실패 - 스케줄러 기능이 비활성화됩니다.');
      schedulerAvailable = false;
    });
  } catch (error) {
    console.warn('⚠️ Redis 스케줄러 초기화 실패:', error.message);
    schedulerAvailable = false;
  }
}

async function enqueueScheduledJob(job) {
  if (!schedulerClient) {
    initSchedulerRedis();
  }

  if (!schedulerAvailable || !schedulerClient) {
    console.warn('⚠️ Redis 미사용 - 예약 작업이 저장되지 않습니다.');
    return;
  }

  try {
    const payload = JSON.stringify(job);
    await schedulerClient.zadd(scheduleSetKey, job.scheduledAt, payload);
  } catch (error) {
    if (error.code !== 'ECONNREFUSED') {
      console.error('예약 작업 저장 실패:', error.message);
    }
    schedulerAvailable = false;
  }
}

async function fetchDueJobs(cutoffTimestamp, batchSize = 50) {
  if (!schedulerClient) {
    initSchedulerRedis();
  }

  if (!schedulerAvailable || !schedulerClient) {
    return [];
  }

  const jobs = [];

  try {
    while (jobs.length < batchSize) {
      const result = await schedulerClient.zpopmin(scheduleSetKey, 1);

      if (!result || result.length === 0) {
        break;
      }

      const [rawJob, score] = result;
      const scheduledAt = Number(score);

      if (!Number.isFinite(scheduledAt)) {
        console.error('예약 작업 스코어가 숫자가 아닙니다:', score);
        continue;
      }

      if (scheduledAt > cutoffTimestamp) {
        await schedulerClient.zadd(scheduleSetKey, scheduledAt, rawJob);
        break;
      }

      try {
        const parsedJob = JSON.parse(rawJob);
        jobs.push(parsedJob);
      } catch (error) {
        console.error('예약 작업 파싱 실패:', error.message);
      }
    }
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      schedulerAvailable = false;
    } else {
      console.error('예약 작업 조회 실패:', error.message);
    }
  }

  return jobs;
}

async function peekNextScheduledTimestamp() {
  if (!schedulerClient) {
    initSchedulerRedis();
  }

  if (!schedulerAvailable || !schedulerClient) {
    return null;
  }

  try {
    const result = await schedulerClient.zrange(scheduleSetKey, 0, 0, 'WITHSCORES');

    if (!result || result.length < 2) {
      return null;
    }

    const score = Number(result[1]);

    return Number.isFinite(score) ? score : null;
  } catch (error) {
    if (error.code === 'ECONNREFUSED' || error.message.includes('max retries')) {
      schedulerAvailable = false;
    }
    return null;
  }
}

async function closeSchedulerConnection() {
  if (schedulerClient) {
    await Promise.allSettled([schedulerClient.quit()]);
  }
}

module.exports = {
  enqueueScheduledJob,
  fetchDueJobs,
  peekNextScheduledTimestamp,
  closeSchedulerConnection,
};
