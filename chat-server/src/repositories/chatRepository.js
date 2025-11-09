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
    `SELECT COUNT(*) AS count
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
          cm.from_user_id AS fromUserId,
          cm.to_user_id AS toUserId,
          cm.from_user_type AS fromUserType,
          cm.to_user_type AS toUserType,
          cm.message,
          cm.created_at AS createdAt,
          cm.is_read AS isRead,
          CASE WHEN cm.from_user_type = 'admin' THEN a.email ELSE m.name END AS fromUserName,
          CASE WHEN cm.to_user_type = 'admin' THEN a2.email ELSE m2.name END AS toUserName
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
        cm.from_user_id AS fromUserId,
        cm.to_user_id AS toUserId,
        cm.from_user_type AS fromUserType,
        cm.to_user_type AS toUserType,
        cm.message,
        cm.created_at AS createdAt,
        cm.is_read AS isRead,
        CASE WHEN cm.from_user_type = 'admin' THEN a.email ELSE m.name END AS fromUserName,
        CASE WHEN cm.to_user_type = 'admin' THEN a2.email ELSE m2.name END AS toUserName
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

