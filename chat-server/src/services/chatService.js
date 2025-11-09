const chatRepository = require('../repositories/chatRepository');

async function recordMessage(messageData) {
  try {
    await chatRepository.saveMessage(messageData);
    return { success: true };
  } catch (error) {
    console.error('메시지 저장 실패:', error);
    return { success: false, error };
  }
}

async function fetchUnreadCount(userId, userType) {
  try {
    const count = await chatRepository.getUnreadMessageCount(userId, userType);
    return { success: true, count };
  } catch (error) {
    console.error('읽지 않은 메시지 카운트 조회 실패:', error);
    return { success: false, error, count: 0 };
  }
}

async function markConversationAsRead(userId, userType, fromUserId, fromUserType) {
  try {
    const rows = await chatRepository.markMessagesAsRead(userId, userType, fromUserId, fromUserType);
    return { success: true, rows };
  } catch (error) {
    console.error('메시지 읽음 처리 실패:', error);
    return { success: false, error, rows: 0 };
  }
}

async function markAllAsRead(userId, userType) {
  try {
    const rows = await chatRepository.markAllMessagesAsRead(userId, userType);
    return { success: true, rows };
  } catch (error) {
    console.error('전체 메시지 읽음 처리 실패:', error);
    return { success: false, error, rows: 0 };
  }
}

async function getHistory(params) {
  try {
    const messages = await chatRepository.getChatHistory(params);
    return { success: true, messages };
  } catch (error) {
    console.error('채팅 내역 조회 실패:', error);
    return { success: false, error, messages: [] };
  }
}

module.exports = {
  recordMessage,
  fetchUnreadCount,
  markConversationAsRead,
  markAllAsRead,
  getHistory,
};

