const { enqueueScheduledJob } = require('../queue/schedulerQueue');
const chatService = require('./chatService');
const { getAdminById, getMemberById } = require('./userService');
const { enqueueNotification } = require('../queue/notificationQueue');

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

function computeScheduledAt({ sendAt, delaySeconds }) {
  if (sendAt) {
    return toUnixTimestamp(sendAt);
  }

  const delay = Number(delaySeconds);
  const safeDelay = Number.isFinite(delay) && delay > 0 ? delay : 0;
  const now = Math.floor(Date.now() / 1000);
  return now + safeDelay;
}

async function reserveMessage({
  fromUserId,
  fromUserType,
  toUserId,
  toUserType,
  message,
  sendAt,
  delaySeconds,
  metadata = {},
}) {
  if (!fromUserId || !fromUserType) {
    throw new Error('fromUserId와 fromUserType은 필수입니다.');
  }
  if (!toUserId || !toUserType) {
    throw new Error('toUserId와 toUserType은 필수입니다.');
  }
  if (!message || typeof message !== 'string') {
    throw new Error('message는 필수 문자열입니다.');
  }

  const scheduledAt = computeScheduledAt({ sendAt, delaySeconds });

  if (scheduledAt <= Math.floor(Date.now() / 1000)) {
    throw new Error('예약 시간은 현재 시간 이후여야 합니다.');
  }

  const job = {
    type: 'RESERVED_MESSAGE',
    scheduledAt,
    message: {
      fromUserId,
      fromUserType,
      toUserId,
      toUserType,
      content: message,
      metadata,
    },
  };

  await enqueueScheduledJob(job);

  return {
    success: true,
    scheduledAt,
  };
}

async function resolveUserName(userId, userType) {
  if (userType === 'admin') {
    const admin = await getAdminById(userId);
    return admin ? admin.email : '관리자';
  }
  const member = await getMemberById(userId);
  return member ? member.name : '사용자';
}

async function dispatchReservedMessage(job, io, userSessions) {
  if (!job?.message) {
    console.warn('예약 메시지 작업에 message payload가 없습니다.');
    return;
  }

  const {
    fromUserId,
    fromUserType,
    toUserId,
    toUserType,
    content,
    metadata = {},
  } = job.message;

  try {
    const recordResult = await chatService.recordMessage({
      fromUserId,
      toUserId,
      fromUserType,
      toUserType,
      message: content,
    });

    if (!recordResult.success) {
      console.error('예약 메시지 저장 실패:', recordResult.error);
      return;
    }

    const fromUserName = await resolveUserName(fromUserId, fromUserType);
    const responseMessage = {
      type: 'message',
      fromUserId,
      toUserId,
      fromUserType,
      toUserType,
      fromUserName,
      message: content,
      metadata,
      timestamp: new Date().toISOString(),
    };

    const receiverKey = `${toUserType}:${toUserId}`;
    const receiverSocketId = userSessions.get(receiverKey);

    if (receiverSocketId) {
      io.to(receiverSocketId).emit('chat:message', responseMessage);
      const unreadResult = await chatService.fetchUnreadCount(toUserId, toUserType);
      io.to(receiverSocketId).emit('chat:unread-count', { count: unreadResult.count ?? 0 });
    }

    const senderKey = `${fromUserType}:${fromUserId}`;
    const senderSocketId = userSessions.get(senderKey);
    if (senderSocketId) {
      io.to(senderSocketId).emit('chat:message', responseMessage);
    }

    await enqueueNotification({
      receiverKey,
      event: 'chat:notification',
      data: {
        title: `${fromUserName}님으로부터 예약 메시지`,
        body: content,
        fromUserId,
        fromUserType,
        fromUserName,
        metadata,
        timestamp: responseMessage.timestamp,
        scheduledAt: job.scheduledAt,
        type: 'RESERVED_MESSAGE',
      },
    });
  } catch (error) {
    console.error('예약 메시지 전송 실패:', error);
  }
}

module.exports = {
  reserveMessage,
  dispatchReservedMessage,
};


