const env = require('../config/env');
const { fetchDueJobs } = require('./schedulerQueue');
const { enqueueNotification } = require('./notificationQueue');

let pollingTimer = null;

async function processScheduledJobs() {
  const now = Math.floor(Date.now() / 1000);

  try {
    const jobs = await fetchDueJobs(now);

    if (jobs.length === 0) {
      return;
    }

    const enqueuePromises = [];

    jobs.forEach((job) => {
      job.recipients.forEach((recipient) => {
        const receiverKey = `${recipient.userType}:${recipient.userId}`;
        enqueuePromises.push(
          enqueueNotification({
            receiverKey,
            event: 'chat:notification',
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
    });

    await Promise.allSettled(enqueuePromises);
  } catch (error) {
    console.error('예약 알림 처리 중 오류:', error);
  }
}

function startSchedulerWorker() {
  if (pollingTimer) {
    return;
  }

  const interval = env.redis.schedulerIntervalMs;

  pollingTimer = setInterval(() => {
    processScheduledJobs().catch((error) => {
      console.error('예약 알림 워커 실패:', error);
    });
  }, interval);

  console.log(`⏰ 예약 알림 워커 시작 (interval = ${interval}ms)`);
}

function stopSchedulerWorker() {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

module.exports = {
  startSchedulerWorker,
  stopSchedulerWorker,
};
