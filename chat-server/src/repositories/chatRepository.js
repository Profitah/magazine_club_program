const pool = require('../db/pool');

async function saveMessage({ fromUserId, toUserId, fromUserType, toUserType, message }) {
  const [result] = await pool.execute(
    `INSERT INTO ChatMessage 
       (from_user_id, to_user_id, from_user_type, to_user_type, message, is_read, created_at) 
     VALUES (?, ?, ?, ?, ?, 0, NOW())`,
    [fromUserId, toUserId, fromUserType, toUserType, message],
  );
  return result.insertId;
}

async function getUnreadMessageCount(userId, userType) {
  const [rows] = await pool.execute(
    `SELECT COUNT(*) as count 
       FROM ChatMessage 
      WHERE to_user_id = ? 
        AND to_user_type = ? 
        AND (is_read = 0 OR is_read IS NULL)`,
    [userId, userType],
  );
  return rows[0]?.count || 0;
}

async function markMessagesAsRead(userId, userType, fromUserId, fromUserType) {
  const [result] = await pool.execute(
    `UPDATE ChatMessage 
        SET is_read = 1 
      WHERE to_user_id = ? 
        AND to_user_type = ? 
        AND from_user_id = ? 
        AND from_user_type = ? 
        AND (is_read = 0 OR is_read IS NULL)`,
    [userId, userType, fromUserId, fromUserType],
  );
  return result.affectedRows;
}

async function markAllMessagesAsRead(userId, userType) {
  const [result] = await pool.execute(
    `UPDATE ChatMessage 
        SET is_read = 1 
      WHERE to_user_id = ? 
        AND to_user_type = ? 
        AND (is_read = 0 OR is_read IS NULL)`,
    [userId, userType],
  );
  return result.affectedRows;
}

async function getChatHistory({ userId, userType, fromUserId, fromUserType }) {
  if (fromUserId && fromUserType) {
    const [rows] = await pool.execute(
      `SELECT 
          cm.id,
          cm.from_user_id as fromUserId,
          cm.to_user_id as toUserId,
          cm.from_user_type as fromUserType,
          cm.to_user_type as toUserType,
          cm.message,
          cm.created_at as createdAt,
          cm.is_read as isRead,
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
        WHERE ((cm.from_user_id = ? AND cm.from_user_type = ? AND cm.to_user_id = ? AND cm.to_user_type = ?)
           OR (cm.to_user_id = ? AND cm.to_user_type = ? AND cm.from_user_id = ? AND cm.from_user_type = ?))
        ORDER BY cm.created_at ASC`,
      [fromUserId, fromUserType, userId, userType, userId, userType, fromUserId, fromUserType],
    );
    return rows;
  }

  const [rows] = await pool.execute(
    `SELECT 
        cm.id,
        cm.from_user_id as fromUserId,
        cm.to_user_id as toUserId,
        cm.from_user_type as fromUserType,
        cm.to_user_type as toUserType,
        cm.message,
        cm.created_at as createdAt,
        cm.is_read as isRead,
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
    [userId, userType, userId, userType],
  );
  return rows;
}

module.exports = {
  saveMessage,
  getUnreadMessageCount,
  markMessagesAsRead,
  markAllMessagesAsRead,
  getChatHistory,
};

