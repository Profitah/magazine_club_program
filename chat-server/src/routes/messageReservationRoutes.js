const express = require('express');
const { reserveMessage } = require('../services/messageReservationService');

const router = express.Router();

router.post('/reserve', async (req, res) => {
  try {
    const result = await reserveMessage(req.body);
    return res.status(202).json(result);
  } catch (error) {
    console.error('예약 메시지 등록 실패:', error);
    return res.status(400).json({
      success: false,
      message: error.message || '예약 메시지 등록에 실패했습니다.',
    });
  }
});

module.exports = router;


