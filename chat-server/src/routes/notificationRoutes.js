const express = require('express');
const notificationMonitor = require('../queue/notificationMonitor');
const { retryFailedJob } = require('../queue/notificationQueue');

const router = express.Router();

router.get('/logs', async (req, res) => {
  try {
    const limit = Number.parseInt(req.query.limit, 10) || 100;
    const logs = await notificationMonitor.getRecentLogs(limit);
    res.json({ success: true, logs });
  } catch (error) {
    console.error('알림 로그 조회 실패:', error);
    res.status(500).json({ success: false, message: '알림 로그 조회 실패' });
  }
});

router.get('/failed', async (req, res) => {
  try {
    const jobs = await notificationMonitor.getFailedJobs();
    const totalFailed = jobs.length;
    const summary = jobs.reduce((acc, job) => {
      const reason = job.reason || 'unknown';
      acc[reason] = (acc[reason] || 0) + 1;
      return acc;
    }, {});
    res.json({ success: true, jobs, totalFailed, summary });
  } catch (error) {
    console.error('실패한 알림 조회 실패:', error);
    res.status(500).json({ success: false, message: '실패한 알림 조회 실패' });
  }
});

router.post('/retry', async (req, res) => {
  try {
    const { jobId } = req.body;
    if (!jobId) {
      return res.status(400).json({ success: false, message: 'jobId는 필수입니다.' });
    }

    const result = await retryFailedJob(jobId);
    if (!result.success) {
      return res.status(404).json(result);
    }

    return res.json(result);
  } catch (error) {
    console.error('알림 재시도 실패:', error);
    return res.status(500).json({ success: false, message: '알림 재시도 실패' });
  }
});

module.exports = router;
