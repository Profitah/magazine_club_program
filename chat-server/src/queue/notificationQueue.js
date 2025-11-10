const Redis = require('ioredis');
const env = require('../config/env');

const redisUrl = env.redis.url
  || `redis://${env.redis.host}:${env.redis.port}`;
const queueName = env.redis.queue;

const publisher = new Redis(redisUrl);
const worker = new Redis(redisUrl);

publisher.on('error', (err) => console.error('Redis publisher error:', err));
worker.on('error', (err) => console.error('Redis worker error:', err));

async function enqueueNotification(job) {
  try {
    await publisher.rpush(queueName, JSON.stringify(job));
  } catch (error) {
    console.error('알림 큐 적재 실패:', error);
  }
}

async function startNotificationWorker(handleJob) {
  console.log(`📬 Redis 알림 워커 시작 (queue = ${queueName})`);
  while (true) {
    try {
      const result = await worker.brpop(queueName, 0);
      if (!result || result.length < 2) {
        continue;
      }
      const rawJob = result[1];
      try {
        const job = JSON.parse(rawJob);
        await handleJob(job);
      } catch (parseError) {
        console.error('알림 작업 파싱 실패:', parseError, rawJob);
      }
    } catch (workerError) {
      console.error('알림 워커 오류:', workerError);
      await new Promise((resolve) => setTimeout(resolve, 1000));
    }
  }
}

async function closeQueueConnections() {
  await Promise.allSettled([publisher.quit(), worker.quit()]);
}

module.exports = {
  enqueueNotification,
  startNotificationWorker,
  closeQueueConnections,
};

