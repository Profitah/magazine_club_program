const Redis = require('ioredis');
const crypto = require('crypto');
const env = require('../config/env');
const monitor = require('./notificationMonitor');

const redisUrl = env.redis.url
  || `redis://${env.redis.host}:${env.redis.port}`;
const queueName = env.redis.queue;

const publisher = new Redis(redisUrl);
const worker = new Redis(redisUrl);

publisher.on('error', (err) => console.error('Redis publisher error:', err));
worker.on('error', (err) => console.error('Redis worker error:', err));

function buildJob(payload) {
  const attempts = payload.attempts || 0;
  return {
    id: payload.id || crypto.randomUUID(),
    receiverKey: payload.receiverKey,
    event: payload.event || 'chat:notification',
    data: payload.data,
    attempts,
    enqueuedAt: payload.enqueuedAt || Date.now(),
  };
}

async function enqueueNotification(payload) {
  const job = buildJob(payload);
  try {
    await publisher.rpush(queueName, JSON.stringify(job));
    await monitor.recordEnqueued(job);
  } catch (error) {
    console.error('알림 큐 적재 실패:', error);
    await monitor.recordFailure(job, error.message || 'enqueue_failed');
  }
  return job.id;
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

async function retryFailedJob(jobId) {
  const failedJob = await monitor.getFailedJob(jobId);
  if (!failedJob || !failedJob.receiverKey || !failedJob.data) {
    return { success: false, message: '재시도 대상 작업을 찾을 수 없습니다.' };
  }

  const job = {
    ...failedJob,
    attempts: (failedJob.attempts || 0) + 1,
    enqueuedAt: Date.now(),
  };

  await monitor.removeFailedJob(jobId);
  const newJobId = await enqueueNotification(job);
  return { success: true, jobId: newJobId };
}

module.exports = {
  enqueueNotification,
  startNotificationWorker,
  closeQueueConnections,
  retryFailedJob,
};

