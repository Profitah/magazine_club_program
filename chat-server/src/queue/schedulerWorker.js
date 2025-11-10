const env = require('../config/env');
const { fetchDueJobs, peekNextScheduledTimestamp } = require('./schedulerQueue');
const { enqueueNotification } = require('./notificationQueue');

let pollingTimer = null;
let currentHandlers = null;
let isRunning = false;

const defaultHandlers = {
  async onNotification(job) {
    if (!Array.isArray(job.recipients)) {
      return;
    }

    const enqueuePromises = [];

    job.recipients.forEach((recipient) => {
      if (!recipient || !recipient.userId || !recipient.userType) {
        return;
      }
      const receiverKey = `${recipient.userType}:${recipient.userId}`;
      enqueuePromises.push(
        enqueueNotification({
          receiverKey,
          event: job.event || 'chat:notification',
          data: {
            title: job.title,
            body: job.body,
            type: job.type,
            metadata: job.metadata || {},
            scheduledAt: job.scheduledAt,
          },
        })
      );
    });

    await Promise.allSettled(enqueuePromises);
  },
};

async function processScheduledJobs(handlers) {
  const now = Math.floor(Date.now() / 1000);

  try {
    const jobs = await fetchDueJobs(now);

    if (jobs.length === 0) {
      return;
    }

    const tasks = jobs.map(async (job) => {
      const jobType = job.type || 'SCHEDULED_NOTIFICATION';

      if (jobType === 'RESERVED_MESSAGE') {
        if (handlers.onReservedMessage) {
          await handlers.onReservedMessage(job);
        } else {
          console.warn('예약 메시지 작업 처리기가 없습니다.');
        }
        return;
      }

      await handlers.onNotification(job);
    });

    await Promise.allSettled(tasks);
  } catch (error) {
    console.error('예약 알림 처리 중 오류:', error);
  }
}

async function scheduleNextTick() {
  if (!currentHandlers) {
    return;
  }

  if (pollingTimer) {
    clearTimeout(pollingTimer);
  }

  const defaultIntervalMs = env.redis.schedulerIntervalMs;
  let delayMs = defaultIntervalMs;

  try {
    const nextTimestamp = await peekNextScheduledTimestamp();

    if (typeof nextTimestamp === 'number') {
      const nowMs = Date.now();
      const targetMs = nextTimestamp * 1000;
      const diff = targetMs - nowMs;

      if (diff <= 0) {
        delayMs = 0;
      } else {
        delayMs = Math.min(defaultIntervalMs, diff);
      }
    }
  } catch (error) {
    console.error('예약 알림 다음 실행 시점 계산 실패:', error);
  }

  pollingTimer = setTimeout(runSchedulerLoop, delayMs);
}

async function runSchedulerLoop() {
  if (!currentHandlers || isRunning) {
    await scheduleNextTick();
    return;
  }

  isRunning = true;

  try {
    await processScheduledJobs(currentHandlers);
  } catch (error) {
    console.error('예약 알림 워커 실행 중 오류:', error);
  } finally {
    isRunning = false;
    await scheduleNextTick();
  }
}

function startSchedulerWorker(handlers = {}) {
  if (pollingTimer) {
    return;
  }

  currentHandlers = {
    ...defaultHandlers,
    ...handlers,
  };

  console.log('⏰ 예약 알림 워커 시작 (동적 폴링 모드)');
  runSchedulerLoop().catch((error) => {
    console.error('예약 알림 워커 초기 실행 실패:', error);
  });
}

function stopSchedulerWorker() {
  if (pollingTimer) {
    clearTimeout(pollingTimer);
    pollingTimer = null;
  }
  currentHandlers = null;
  isRunning = false;
}

module.exports = {
  startSchedulerWorker,
  stopSchedulerWorker,
};
