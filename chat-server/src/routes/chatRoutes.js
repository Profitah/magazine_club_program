const express = require('express');
const chatService = require('../services/chatService');

const router = express.Router();

router.get('/history', async (req, res) => {
  try {
    const { userId, userType, fromUserId, fromUserType } = req.query;

    if (!userId || !userType) {
      return res.status(400).json({ error: 'userId와 userType이 필요합니다.' });
    }

    const historyResult = await chatService.getHistory({
      userId,
      userType,
      fromUserId,
      fromUserType,
    });

    if (!historyResult.success) {
      return res
        .status(500)
        .json({ error: '채팅 내역을 불러오는데 실패했습니다.' });
    }

    res.json(historyResult.messages);
  } catch (error) {
    console.error('채팅 내역 조회 실패:', error);
    res.status(500).json({ error: '채팅 내역을 불러오는데 실패했습니다.' });
  }
});

module.exports = router;

