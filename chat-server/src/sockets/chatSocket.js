const { verifyAdminSession } = require('../services/authService');
const { getAdminById, getMemberById } = require('../services/userService');
const chatService = require('../services/chatService');
const { enqueueNotification } = require('../queue/notificationQueue');

function registerChatSocket(io, userSessions) {
  io.on('connection', (socket) => {
    console.log('새 클라이언트 연결:', socket.id);

    socket.on('user:connect', async (data) => {
      const { userId, userType, email, sessionCookie } = data;

      if (!userId || !userType) {
        socket.emit('error', { message: 'userId와 userType이 필요합니다.' });
        return;
      }

      if (userType === 'admin') {
        if (!email) {
          socket.emit('error', { message: '관리자는 email이 필요합니다.' });
          return;
        }

        const authResult = await verifyAdminSession(email, sessionCookie);

        if (!authResult.authenticated) {
          socket.emit('error', { message: '인증되지 않은 관리자입니다. 먼저 Spring Boot에서 로그인해주세요.' });
          return;
        }

        if (authResult.adminId !== userId) {
          socket.emit('error', { message: '관리자 ID가 일치하지 않습니다.' });
          return;
        }

        console.log(`✅ 관리자 인증 확인 성공: ${email} (ID: ${userId})`);
      } else {
        try {
          const member = await getMemberById(userId);
          if (!member) {
            socket.emit('error', { message: '존재하지 않는 사용자입니다.' });
            return;
          }
        } catch (error) {
          console.error('DB 조회 실패:', error);
          socket.emit('error', { message: '사용자 정보 조회에 실패했습니다.' });
          return;
        }
      }

      const sessionKey = `${userType}:${userId}`;
      userSessions.set(sessionKey, socket.id);
      socket.userId = userId;
      socket.userType = userType;

      console.log(`사용자 연결: ${sessionKey} -> ${socket.id}`);

      const unreadResult = await chatService.fetchUnreadCount(userId, userType);
      const unreadCount = unreadResult.count ?? 0;

      socket.emit('user:connected', {
        userId,
        userType,
        unreadCount,
      });

      if (unreadCount > 0) {
        socket.emit('chat:unread-count', { count: unreadCount });
      }
    });

    socket.on('chat:send', async (data) => {
      try {
        const {
          fromUserId,
          toUserId,
          fromUserType,
          toUserType,
          message,
        } = data;

        if (!fromUserId || !toUserId || !fromUserType || !toUserType || !message) {
          socket.emit('error', { message: '필수 필드가 누락되었습니다.' });
          return;
        }

        let fromUserName;
        if (fromUserType === 'admin') {
          const admin = await getAdminById(fromUserId);
          if (!admin) {
            socket.emit('error', { message: '관리자 정보를 찾을 수 없습니다.' });
            return;
          }
          fromUserName = admin.email;
        } else {
          const member = await getMemberById(fromUserId);
          if (!member) {
            socket.emit('error', { message: '사용자 정보를 찾을 수 없습니다.' });
            return;
          }
          fromUserName = member.name;
        }

        const recordResult = await chatService.recordMessage({
          fromUserId,
          toUserId,
          fromUserType,
          toUserType,
          message,
        });

        if (!recordResult.success) {
          socket.emit('error', { message: '메시지 저장에 실패했습니다.' });
          return;
        }

        console.log('✅ 메시지 저장 성공');

        const responseMessage = {
          type: 'message',
          fromUserId,
          toUserId,
          fromUserType,
          toUserType,
          fromUserName,
          message,
          timestamp: new Date().toISOString(),
        };

        const receiverKey = `${toUserType}:${toUserId}`;
        const receiverSocketId = userSessions.get(receiverKey);

        if (receiverSocketId) {
          io.to(receiverSocketId).emit('chat:message', responseMessage);
        } else {
          console.log(`수신자 오프라인: ${receiverKey}`);
        }

        await enqueueNotification({
          receiverKey,
          event: 'chat:notification',
          data: {
            title: `${fromUserName}님으로부터 메시지`,
            body: message,
            fromUserId,
            fromUserType,
            fromUserName,
            timestamp: responseMessage.timestamp,
          },
        });

        const unreadForReceiver = await chatService.fetchUnreadCount(toUserId, toUserType);
        if (receiverSocketId) {
          io.to(receiverSocketId).emit('chat:unread-count', { count: unreadForReceiver.count ?? 0 });
        }

        socket.emit('chat:message', responseMessage);
      } catch (error) {
        console.error('메시지 전송 실패:', error);
        socket.emit('error', { message: '메시지 전송에 실패했습니다.' });
      }
    });

    socket.on('chat:history', async (data) => {
      try {
        const { userId, userType, fromUserId, fromUserType } = data;

        if (!userId || !userType) {
          socket.emit('error', { message: 'userId와 userType이 필요합니다.' });
          return;
        }

        const historyResult = await chatService.getHistory({
          userId,
          userType,
          fromUserId,
          fromUserType,
        });

        if (!historyResult.success) {
          socket.emit('error', { message: '채팅 내역을 불러오는데 실패했습니다.' });
          return;
        }

        socket.emit('chat:history', historyResult.messages);

        if (fromUserId && fromUserType) {
          await chatService.markConversationAsRead(userId, userType, fromUserId, fromUserType);
          const unreadResult = await chatService.fetchUnreadCount(userId, userType);
          socket.emit('chat:unread-count', { count: unreadResult.count ?? 0 });
        }
      } catch (error) {
        console.error('채팅 내역 조회 실패:', error);
        socket.emit('error', { message: '채팅 내역을 불러오는데 실패했습니다: ' + error.message });
      }
    });

    socket.on('chat:unread-count', async () => {
      try {
        if (!socket.userId || !socket.userType) {
          socket.emit('error', { message: '먼저 user:connect를 호출해주세요.' });
          return;
        }

        const unreadResult = await chatService.fetchUnreadCount(socket.userId, socket.userType);
        socket.emit('chat:unread-count', { count: unreadResult.count ?? 0 });
      } catch (error) {
        console.error('읽지 않은 메시지 카운트 조회 실패:', error);
        socket.emit('error', { message: '읽지 않은 메시지 카운트 조회 실패' });
      }
    });

    socket.on('chat:mark-read', async (data) => {
      try {
        if (!socket.userId || !socket.userType) {
          socket.emit('error', { message: '먼저 user:connect를 호출해주세요.' });
          return;
        }

        const { fromUserId, fromUserType } = data;

        if (fromUserId && fromUserType) {
          await chatService.markConversationAsRead(socket.userId, socket.userType, fromUserId, fromUserType);
        } else {
          await chatService.markAllAsRead(socket.userId, socket.userType);
        }

        const unreadResult = await chatService.fetchUnreadCount(socket.userId, socket.userType);
        socket.emit('chat:unread-count', { count: unreadResult.count ?? 0 });
      } catch (error) {
        console.error('메시지 읽음 처리 실패:', error);
        socket.emit('error', { message: '메시지 읽음 처리 실패' });
      }
    });

    socket.on('disconnect', () => {
      if (socket.userId && socket.userType) {
        const sessionKey = `${socket.userType}:${socket.userId}`;
        userSessions.delete(sessionKey);
        console.log(`사용자 연결 해제: ${sessionKey}`);
      }
      console.log('클라이언트 연결 해제:', socket.id);
    });
  });
}

module.exports = {
  registerChatSocket,
};

