const express = require('express');
const router = express.Router();
const profanityFilterService = require('../services/profanityFilterService');
const blockService = require('../services/blockService');

/**
 * 금칙어 관리 API (관리자용)
 * 
 * 금칙어는 파일(profanity-words.txt)로 관리됩니다.
 * 파일을 수정하면 서버가 자동으로 감지하여 DB에 동기화합니다 (최대 1분 이내).
 */

// 금칙어 목록 조회
router.get('/words', async (req, res) => {
  try {
    const words = profanityFilterService.getWords();
    res.json({ 
      success: true, 
      words: words.map(word => ({ word })),
      message: '금칙어는 profanity-words.txt 파일로 관리됩니다. 파일을 수정하면 자동으로 반영됩니다 (최대 1분 이내).'
    });
  } catch (error) {
    console.error('금칙어 목록 조회 실패:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// 수동 동기화 (즉시 반영)
router.post('/sync', async (req, res) => {
  try {
    await profanityFilterService.syncFromFile();
    const words = profanityFilterService.getWords();
    res.json({ 
      success: true, 
      message: '파일에서 금칙어 목록을 동기화했습니다.',
      wordCount: words.length
    });
  } catch (error) {
    console.error('금칙어 동기화 실패:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// 차단된 사용자 목록 조회 (자동 해제 없음: blocked_until 조건 제거)
router.get('/blocked-users', async (req, res) => {
  try {
    const pool = require('../db/pool');
    const [rows] = await pool.execute(
      `SELECT user_id, user_type, blocked_until, reason, violation_count, created_at 
       FROM BlockedUser 
       ORDER BY created_at DESC`
    );
    res.json({ success: true, blockedUsers: rows });
  } catch (error) {
    console.error('차단된 사용자 목록 조회 실패:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// 사용자 차단 해제 (Jenkins를 통해 처리 권장)
router.post('/unblock', async (req, res) => {
  try {
    const { userId, userType } = req.body;
    if (!userId || !userType) {
      return res.status(400).json({ success: false, error: 'userId와 userType이 필요합니다.' });
    }
    // 주의: 차단 해제는 Jenkins를 통해 처리하는 것이 권장됩니다.
    // Jenkins를 통해 승인 프로세스를 거치면 더 안전합니다.
    const result = await blockService.unblockUser(userId, userType);
    res.json({
      ...result,
      message: '차단 해제 완료. 프로덕션 환경에서는 Jenkins를 통해 처리하는 것이 권장됩니다.'
    });
  } catch (error) {
    console.error('차단 해제 실패:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// 사용자 위반 기록 조회
router.get('/violations/:userId/:userType', async (req, res) => {
  try {
    const { userId, userType } = req.params;
    const history = await blockService.getUserViolationHistory(parseInt(userId), userType);
    res.json({ success: true, violationHistory: history });
  } catch (error) {
    console.error('위반 기록 조회 실패:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// 위반 기록 초기화
router.post('/reset-violations', async (req, res) => {
  try {
    const { userId, userType } = req.body;
    if (!userId || !userType) {
      return res.status(400).json({ success: false, error: 'userId와 userType이 필요합니다.' });
    }
    const result = await blockService.resetViolations(userId, userType);
    res.json({
      ...result,
      message: '위반 기록 초기화 완료'
    });
  } catch (error) {
    console.error('위반 기록 초기화 실패:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

module.exports = router;

