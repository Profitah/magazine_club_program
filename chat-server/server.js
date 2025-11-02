const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const mysql = require('mysql2/promise');
const cors = require('cors');
const path = require('path');
const axios = require('axios');
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// MySQL 연결 풀 생성
const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

// 사용자 세션 관리 (userId -> socket.id)
const userSessions = new Map();

// Spring Boot 서버 URL (관리자 인증 확인용)
const SPRING_BOOT_URL = process.env.SPRING_BOOT_URL || 'http://localhost:8080';

// Spring Boot 세션 인증 확인 함수
async function verifyAdminSession(email, sessionCookie) {
  try {
    const response = await axios.post(
      `${SPRING_BOOT_URL}/admin/verify-for-chat`,
      { email: email },
      {
        headers: {
          'Content-Type': 'application/json',
          'Cookie': sessionCookie || ''
        },
        validateStatus: (status) => status < 500 // 500 미만은 정상 처리
      }
    );

    if (response.status === 200 && response.data.authenticated) {
      return {
        authenticated: true,
        adminId: response.data.adminId,
        email: response.data.email
      };
    }

    return { authenticated: false };
  } catch (error) {
    console.error('Spring Boot 인증 확인 실패:', error.message);
    return { authenticated: false };
  }
}


// 관리자 정보 조회
async function getAdminById(adminId) {
  try {
    const [rows] = await pool.execute(
      'SELECT id, email FROM Admin WHERE id = ?',
      [adminId]
    );
    return rows[0] || null;
  } catch (error) {
    console.error('관리자 조회 실패:', error);
    return null;
  }
}

// 일반 사용자(멤버) 정보 조회 (MemberInfo 테이블)
async function getMemberById(memberId) {
  try {
    const [rows] = await pool.execute(
      'SELECT id, name FROM MemberInfo WHERE id = ?',
      [memberId]
    );
    return rows[0] || null;
  } catch (error) {
    console.error('멤버 조회 실패:', error);
    return null;
  }
}

// 채팅 메시지 저장
async function saveMessage(messageData) {
  try {
    const [result] = await pool.execute(
      `INSERT INTO ChatMessage 
       (from_user_id, to_user_id, from_user_type, to_user_type, message, created_at) 
       VALUES (?, ?, ?, ?, ?, NOW())`,
      [
        messageData.fromUserId,
        messageData.toUserId,
        messageData.fromUserType,
        messageData.toUserType,
        messageData.message
      ]
    );
    return result.insertId;
  } catch (error) {
    console.error('메시지 저장 실패:', error);
    throw error;
  }
}

// WebSocket 연결 처리
io.on('connection', (socket) => {
  console.log('새 클라이언트 연결:', socket.id);

  // 사용자 연결 (세션 등록)
  // 관리자는 Spring Boot에서 Authenticator로 로그인되어 있어야 함
  // 일반 사용자는 별도 로그인 없이 MemberInfo 테이블의 멤버 정보 사용
  socket.on('user:connect', async (data) => {
    const { userId, userType, email, sessionCookie } = data;
    
    if (!userId || !userType) {
      socket.emit('error', { message: 'userId와 userType이 필요합니다.' });
      return;
    }

    // 관리자인 경우 Spring Boot 세션 인증 확인
    if (userType === 'admin') {
      if (!email) {
        socket.emit('error', { message: '관리자는 email이 필요합니다.' });
        return;
      }

      // Spring Boot에서 인증 상태 확인
      const authResult = await verifyAdminSession(email, sessionCookie);
      
      if (!authResult.authenticated) {
        socket.emit('error', { message: '인증되지 않은 관리자입니다. 먼저 Spring Boot에서 로그인해주세요.' });
        return;
      }

      // 인증된 관리자 ID 확인
      if (authResult.adminId !== userId) {
        socket.emit('error', { message: '관리자 ID가 일치하지 않습니다.' });
        return;
      }

      console.log(`✅ 관리자 인증 확인 성공: ${email} (ID: ${userId})`);
    } else {
      // 일반 사용자는 DB에서 확인
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

    // 세션 등록
    const sessionKey = `${userType}:${userId}`;
    userSessions.set(sessionKey, socket.id);
    socket.userId = userId;
    socket.userType = userType;

    console.log(`사용자 연결: ${sessionKey} -> ${socket.id}`);
    socket.emit('user:connected', { userId, userType });
  });

  // 채팅 메시지 전송
  socket.on('chat:send', async (data) => {
    try {
      const {
        fromUserId,
        toUserId,
        fromUserType,
        toUserType,
        message
      } = data;

      // 입력 검증
      if (!fromUserId || !toUserId || !fromUserType || !toUserType || !message) {
        socket.emit('error', { message: '필수 필드가 누락되었습니다.' });
        return;
      }

      // 발신자 이름 가져오기
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

      // 메시지 저장 (DB에 저장)
      try {
        await saveMessage({
          fromUserId,
          toUserId,
          fromUserType,
          toUserType,
          message
        });
        console.log('✅ 메시지 저장 성공');
      } catch (error) {
        console.error('메시지 저장 실패:', error);
        socket.emit('error', { message: '메시지 저장에 실패했습니다.' });
        return;
      }

      // 응답 메시지 객체 생성
      const responseMessage = {
        type: 'message',
        fromUserId,
        toUserId,
        fromUserType,
        toUserType,
        fromUserName,
        message,
        timestamp: new Date().toISOString()
      };

      // 수신자에게 메시지 전송
      const receiverKey = `${toUserType}:${toUserId}`;
      const receiverSocketId = userSessions.get(receiverKey);
      
      if (receiverSocketId) {
        io.to(receiverSocketId).emit('chat:message', responseMessage);
        console.log(`메시지 전송: ${fromUserType}:${fromUserId} -> ${toUserType}:${toUserId}`);
      } else {
        console.log(`수신자 오프라인: ${receiverKey}`);
      }

      // 발신자에게도 확인 메시지 전송
      socket.emit('chat:message', responseMessage);

    } catch (error) {
      console.error('메시지 전송 실패:', error);
      socket.emit('error', { message: '메시지 전송에 실패했습니다.' });
    }
  });

  // 채팅 내역 요청
  socket.on('chat:history', async (data) => {
    try {
      const { userId, userType } = data;

      if (!userId || !userType) {
        socket.emit('error', { message: 'userId와 userType이 필요합니다.' });
        return;
      }

      // DB에서 채팅 내역 조회
      const [messages] = await pool.execute(
        `SELECT 
          cm.id,
          cm.from_user_id as fromUserId,
          cm.to_user_id as toUserId,
          cm.from_user_type as fromUserType,
          cm.to_user_type as toUserType,
          cm.message,
          cm.created_at as createdAt,
          CASE 
            WHEN cm.from_user_type = 'admin' THEN a.email
            WHEN cm.from_user_type = 'user' THEN m.name
          END as fromUserName,
          CASE 
            WHEN cm.to_user_type = 'admin' THEN a2.email
            WHEN cm.to_user_type = 'user' THEN m2.name
          END as toUserName
        FROM ChatMessage cm
        LEFT JOIN Admin a ON cm.from_user_type = 'admin' AND cm.from_user_id = a.id
        LEFT JOIN MemberInfo m ON cm.from_user_type = 'user' AND cm.from_user_id = m.id
        LEFT JOIN Admin a2 ON cm.to_user_type = 'admin' AND cm.to_user_id = a2.id
        LEFT JOIN MemberInfo m2 ON cm.to_user_type = 'user' AND cm.to_user_id = m2.id
        WHERE (cm.from_user_id = ? AND cm.from_user_type = ?)
           OR (cm.to_user_id = ? AND cm.to_user_type = ?)
        ORDER BY cm.created_at ASC`,
        [userId, userType, userId, userType]
      );

      console.log(`✅ 채팅 내역 조회 성공`);
      socket.emit('chat:history', messages);

    } catch (error) {
      console.error('채팅 내역 조회 실패:', error);
      socket.emit('error', { message: '채팅 내역을 불러오는데 실패했습니다: ' + error.message });
    }
  });

  // 연결 해제
  socket.on('disconnect', () => {
    if (socket.userId && socket.userType) {
      const sessionKey = `${socket.userType}:${socket.userId}`;
      userSessions.delete(sessionKey);
      console.log(`사용자 연결 해제: ${sessionKey}`);
    }
    console.log('클라이언트 연결 해제:', socket.id);
  });
});

// REST API: 채팅 내역 조회 (선택사항)
app.get('/api/chat/history', async (req, res) => {
  try {
    const { userId, userType } = req.query;

    if (!userId || !userType) {
      return res.status(400).json({ error: 'userId와 userType이 필요합니다.' });
    }

    const [messages] = await pool.execute(
      `SELECT 
        cm.id,
        cm.from_user_id as fromUserId,
        cm.to_user_id as toUserId,
        cm.from_user_type as fromUserType,
        cm.to_user_type as toUserType,
        cm.message,
        cm.created_at as createdAt,
          CASE 
            WHEN cm.from_user_type = 'admin' THEN a.email
            WHEN cm.from_user_type = 'user' THEN m.name
          END as fromUserName,
          CASE 
            WHEN cm.to_user_type = 'admin' THEN a2.email
            WHEN cm.to_user_type = 'user' THEN m2.name
          END as toUserName
      FROM ChatMessage cm
        LEFT JOIN Admin a ON cm.from_user_type = 'admin' AND cm.from_user_id = a.id
        LEFT JOIN MemberInfo m ON cm.from_user_type = 'user' AND cm.from_user_id = m.id
        LEFT JOIN Admin a2 ON cm.to_user_type = 'admin' AND cm.to_user_id = a2.id
        LEFT JOIN MemberInfo m2 ON cm.to_user_type = 'user' AND cm.to_user_id = m2.id
      WHERE (cm.from_user_id = ? AND cm.from_user_type = ?)
         OR (cm.to_user_id = ? AND cm.to_user_type = ?)
      ORDER BY cm.created_at ASC`,
      [userId, userType, userId, userType]
    );

    res.json(messages);
  } catch (error) {
    console.error('채팅 내역 조회 실패:', error);
    res.status(500).json({ error: '채팅 내역을 불러오는데 실패했습니다.' });
  }
});

const PORT = process.env.CHAT_PORT || 3001;
server.listen(PORT, () => {
  console.log(`채팅 서버가 포트 ${PORT}에서 실행 중입니다.`);
});

