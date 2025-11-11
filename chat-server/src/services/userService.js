const pool = require('../db/pool');

async function getAdminById(adminId) {
  try {
    const [rows] = await pool.execute(
      'SELECT id, email FROM Admin WHERE id = ?',
      [adminId],
    );
    return rows[0] || null;
  } catch (error) {
    console.error('관리자 조회 실패:', error);
    return null;
  }
}

async function getMemberById(memberId) {
  try {
    const [rows] = await pool.execute(
      'SELECT id, name FROM MemberInfo WHERE id = ?',
      [memberId],
    );
    return rows[0] || null;
  } catch (error) {
    console.error('멤버 조회 실패:', error);
    return null;
  }
}

module.exports = {
  getAdminById,
  getMemberById,
};

