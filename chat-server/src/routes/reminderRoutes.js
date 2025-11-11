const express = require('express');
const { scheduleAssignmentReminder } = require('../services/assignmentReminderService');
const { scheduleNotificationJob } = require('../services/scheduledNotificationService');

const router = express.Router();

router.post('/assignments/reminders', async (req, res) => {
  try {
    const {
      assignmentId,
      assignmentTitle,
      dueDate,
      recipients,
      message,
      requestedBy,
    } = req.body;

    const result = await scheduleAssignmentReminder({
      assignmentId,
      assignmentTitle,
      dueDate,
      recipients,
      message,
      requestedBy,
    });

    return res.status(202).json(result);
  } catch (error) {
    console.error('미제출자 리마인더 큐 적재 실패:', error);
    return res.status(400).json({
      success: false,
      message: error.message || '미제출자 리마인더 큐 적재에 실패했습니다.',
    });
  }
});

router.post('/schedule', async (req, res) => {
  try {
    const {
      sendAt,
      recipients,
      title,
      body,
      type,
      metadata,
    } = req.body;

    const result = await scheduleNotificationJob({
      sendAt,
      recipients,
      title,
      body,
      type,
      metadata,
    });

    return res.status(202).json(result);
  } catch (error) {
    console.error('예약 알림 등록 실패:', error);
    return res.status(400).json({
      success: false,
      message: error.message || '예약 알림 등록에 실패했습니다.',
    });
  }
});

module.exports = router;
