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

async function fetchDueJobs(cutoffTimestamp) {
  const items = await schedulerClient.zrangebyscore(scheduleSetKey, 0, cutoffTimestamp);

  if (items.length === 0) {
    return [];
  }

  const pipeline = schedulerClient.pipeline();
  items.forEach((item) => {
    pipeline.zrem(scheduleSetKey, item);
  });
  await pipeline.exec();

  return items.map((item) => {
    try {
      return JSON.parse(item);
    } catch (error) {
      console.error('예약 작업 파싱 실패:', error, item);
      return null;
    }
  }).filter(Boolean);
}

async function closeSchedulerConnection() {
  await schedulerClient.quit();
}

module.exports = {
  enqueueScheduledJob,
  fetchDueJobs,
  closeSchedulerConnection,
};
