const Redis = require('ioredis');
const crypto = require('crypto');
const env = require('../config/env');
const monitor = require('./notificationMonitor');

const redisUrl = env.redis.url
  || `redis://${env.redis.host}:${env.redis.port}`;
const queueName = env.redis.queue;

let publisher = null;
let worker = null;
let redisAvailable = false;

// Redis 연결 초기화 (lazy)
function initRedis() {
  if (publisher && worker) {
    return;
  }

  try {
    publisher = new Redis(redisUrl, {
      maxRetriesPerRequest: 3,
      retryStrategy: (times) => {
        if (times > 3) {
          console.warn('⚠️ Redis 연결 실패 - 알림 기능이 비활성화됩니다.');
          redisAvailable = false;
          return null; // 재시도 중단
        }
        return Math.min(times * 50, 2000);
      },
      lazyConnect: true,
    });

    worker = new Redis(redisUrl, {
      maxRetriesPerRequest: 3,
      retryStrategy: (times) => {
        if (times > 3) {
          console.warn('⚠️ Redis 연결 실패 - 알림 워커가 비활성화됩니다.');
          redisAvailable = false;
          return null;
        }
        return Math.min(times * 50, 2000);
      },
      lazyConnect: true,
    });

    publisher.on('error', (err) => {
      if (err.code !== 'ECONNREFUSED') {
        console.error('Redis publisher error:', err.message);
      }
      redisAvailable = false;
    });

    worker.on('error', (err) => {
      if (err.code !== 'ECONNREFUSED') {
        console.error('Redis worker error:', err.message);
      }
      redisAvailable = false;
    });

    publisher.on('connect', () => {
      console.log('✅ Redis publisher 연결 성공');
      redisAvailable = true;
    });

    worker.on('connect', () => {
      console.log('✅ Redis worker 연결 성공');
      redisAvailable = true;
    });

    // 연결 시도
    publisher.connect().catch(() => {
      console.warn('⚠️ Redis 연결 실패 - 알림 기능이 비활성화됩니다.');
      redisAvailable = false;
    });
    worker.connect().catch(() => {
      console.warn('⚠️ Redis 연결 실패 - 알림 워커가 비활성화됩니다.');
      redisAvailable = false;
    });
  } catch (error) {
    console.warn('⚠️ Redis 초기화 실패:', error.message);
    redisAvailable = false;
  }
}

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
  if (!publisher || !worker) {
    initRedis();
  }

  const job = buildJob(payload);
  
  if (!redisAvailable || !publisher) {
    console.warn('⚠️ Redis 미사용 - 알림이 큐에 적재되지 않습니다.');
    return job.id;
  }

  try {
    await publisher.rpush(queueName, JSON.stringify(job));
    await monitor.recordEnqueued(job);
  } catch (error) {
    if (error.code !== 'ECONNREFUSED') {
      console.error('알림 큐 적재 실패:', error.message);
    }
    try {
      await monitor.recordFailure(job, error.message || 'enqueue_failed');
    } catch (monitorError) {
      // 모니터 기록 실패는 무시
    }
  }
  return job.id;
}

async function startNotificationWorker(handleJob) {
  if (!publisher || !worker) {
    initRedis();
  }

  if (!redisAvailable || !worker) {
    console.warn('⚠️ Redis 미사용 - 알림 워커가 시작되지 않습니다.');
    return;
  }

  console.log(`📬 Redis 알림 워커 시작 (queue = ${queueName})`);
  
  while (true) {
    try {
      if (!redisAvailable || !worker) {
        await new Promise((resolve) => setTimeout(resolve, 5000));
        continue;
      }

      const result = await worker.brpop(queueName, 5); // 5초 타임아웃
      if (!result || result.length < 2) {
        continue;
      }
      const rawJob = result[1];
      try {
        const job = JSON.parse(rawJob);
        await handleJob(job);
      } catch (parseError) {
        console.error('알림 작업 파싱 실패:', parseError.message);
      }
    } catch (workerError) {
      if (workerError.code === 'ECONNREFUSED' || workerError.message.includes('max retries')) {
        console.warn('⚠️ Redis 연결 실패 - 알림 워커가 일시 중지됩니다.');
        redisAvailable = false;
        await new Promise((resolve) => setTimeout(resolve, 5000));
      } else {
        console.error('알림 워커 오류:', workerError.message);
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
    }
  }
}

async function closeQueueConnections() {
  if (publisher) {
    await Promise.allSettled([publisher.quit()]);
  }
  if (worker) {
    await Promise.allSettled([worker.quit()]);
  }
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

