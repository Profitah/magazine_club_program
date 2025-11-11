const { enqueueScheduledJob } = require('../queue/schedulerQueue');

function toUnixTimestamp(dateLike) {
  if (dateLike instanceof Date) {
    return Math.floor(dateLike.getTime() / 1000);
  }

  const parsed = new Date(dateLike);
  if (Number.isNaN(parsed.getTime())) {
    throw new Error('유효한 날짜/시간이 아닙니다.');
  }

  return Math.floor(parsed.getTime() / 1000);
}

function validateRecipients(recipients) {
  if (!Array.isArray(recipients) || recipients.length === 0) {
    throw new Error('recipients 배열이 비어 있습니다.');
  }

  recipients.forEach((recipient) => {
    if (!recipient?.userId || !recipient?.userType) {
      throw new Error('수신자 정보에 userId 또는 userType이 없습니다.');
    }
  });
}

async function scheduleNotificationJob({
  sendAt,
  recipients,
  title,
  body,
  type,
  metadata = {},
}) {
  if (!sendAt) {
    throw new Error('sendAt(발송 시각)은 필수입니다.');
  }

  validateRecipients(recipients);

  if (!title) {
    throw new Error('title은 필수입니다.');
  }

  if (!body) {
    throw new Error('body는 필수입니다.');
  }

  const scheduledAt = toUnixTimestamp(sendAt);

  const job = {
    scheduledAt,
    recipients,
    title,
    body,
    type: type || 'SCHEDULED_NOTIFICATION',
    metadata,
  };

  await enqueueScheduledJob(job);

  return {
    success: true,
    scheduledAt,
    recipients: recipients.length,
  };
}

module.exports = {
  scheduleNotificationJob,
};
