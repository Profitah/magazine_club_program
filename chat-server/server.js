const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const path = require('path');

const env = require('./src/config/env');
const chatRoutes = require('./src/routes/chatRoutes');
const { registerChatSocket } = require('./src/sockets/chatSocket');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
  },
});

const userSessions = new Map();

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));
app.use('/api/chat', chatRoutes);

registerChatSocket(io, userSessions);

server.listen(env.app.port, () => {
  console.log(`채팅 서버가 포트 ${env.app.port}에서 실행 중입니다.`);
});