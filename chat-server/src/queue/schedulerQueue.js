const Redis = require('ioredis');
const env = require('../config/env');

const redisUrl = env.redis.url || `redis://${env.redis.host}:${env.redis.port}`;
const scheduleSetKey = env.redis.scheduleSet;

const schedulerClient = new Redis(redisUrl);

schedulerClient.on('error', (err) => {
  console.error('Redis scheduler client error:', err);
});

async function enqueueScheduledJob(job) {
  const payload = JSON.stringify(job);
  await schedulerClient.zadd(scheduleSetKey, job.scheduledAt, payload);
}

async function fetchDueJobs(cutoffTimestamp, batchSize = 50) {
  const jobs = [];

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
      console.error('예약 작업 파싱 실패:', error, rawJob);
    }
  }

  return jobs;
}

async function peekNextScheduledTimestamp() {
  const result = await schedulerClient.zrange(scheduleSetKey, 0, 0, 'WITHSCORES');

  if (!result || result.length < 2) {
    return null;
  }

  const score = Number(result[1]);

  return Number.isFinite(score) ? score : null;
}

async function closeSchedulerConnection() {
  await schedulerClient.quit();
}

module.exports = {
  enqueueScheduledJob,
  fetchDueJobs,
  peekNextScheduledTimestamp,
  closeSchedulerConnection,
};
