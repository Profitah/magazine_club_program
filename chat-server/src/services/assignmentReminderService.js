const { enqueueNotification } = require('../queue/notificationQueue');

function buildReceiverKey(userType, userId) {
  return `${userType}:${userId}`;
}

function createReminderMessage({ assignmentTitle, dueDate, customMessage }) {
  if (customMessage) {
    return customMessage;
  }

  if (dueDate) {
    return `${assignmentTitle} 과제가 아직 제출되지 않았습니다. 마감 기한(${dueDate}) 전에 제출해주세요.`;
  }

  return `${assignmentTitle} 과제가 아직 제출되지 않았습니다. 빠른 제출을 부탁드립니다.`;
}

async function scheduleAssignmentReminder({
  assignmentId,
  assignmentTitle,
  dueDate,
  recipients,
  message,
  requestedBy,
}) {
  if (!assignmentId) {
    throw new Error('assignmentId는 필수입니다.');
  }

  if (!assignmentTitle) {
    throw new Error('assignmentTitle은 필수입니다.');
  }

  if (!Array.isArray(recipients) || recipients.length === 0) {
    throw new Error('recipients 배열이 비어 있습니다.');
  }

  const reminderBody = createReminderMessage({
    assignmentTitle,
    dueDate,
    customMessage: message,
  });

  const jobs = recipients
    .filter((recipient) => recipient && recipient.userId && recipient.userType)
    .map((recipient) => {
      const receiverKey = buildReceiverKey(recipient.userType, recipient.userId);

      return enqueueNotification({
        receiverKey,
        event: 'chat:notification',
        data: {
          title: `[미제출 알림] ${assignmentTitle}`,
          body: reminderBody,
          assignmentId,
          assignmentTitle,
          dueDate: dueDate || null,
          requestedBy: requestedBy || null,
          recipient: {
            userId: recipient.userId,
            userType: recipient.userType,
            name: recipient.name || null,
          },
          type: 'ASSIGNMENT_REMINDER',
        },
      });
    });

  if (jobs.length === 0) {
    throw new Error('유효한 수신자가 없습니다.');
  }

  await Promise.all(jobs);

  return {
    success: true,
    queued: jobs.length,
  };
}

module.exports = {
  scheduleAssignmentReminder,
};
