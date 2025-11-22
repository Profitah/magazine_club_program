const pool = require('../db/pool');

/**
 * 사용자 차단 관리 서비스
 * - 금칙어 사용 횟수 추적
 * - 자동 차단 처리
 * - 수동 해제만 가능 (자동 해제 없음)
 */
class BlockService {
  /**
   * 금칙어 사용 기록 및 즉시 차단 처리
   * 서버에서 즉시 차단하고, Jenkins는 검토/롤백용으로 사용
   * @param {number} userId - 사용자 ID
   * @param {string} userType - 사용자 타입 ('member' 또는 'admin')
   * @param {string[]} detectedWords - 감지된 금칙어 목록
   * @returns {Object} { isBlocked: boolean, violationCount: number, blockUntil: Date | null }
   */
  async recordViolation(userId, userType, detectedWords) {
    try {
      // 기존 위반 기록 조회
      const [existing] = await pool.execute(
        'SELECT violation_count FROM UserViolation WHERE user_id = ? AND user_type = ?',
        [userId, userType]
      );

      const violationCount = existing.length > 0 
        ? existing[0].violation_count + detectedWords.length 
        : detectedWords.length;

      // 위반 기록 저장/업데이트
      await pool.execute(
        `INSERT INTO UserViolation (user_id, user_type, violation_count, last_violation_at)
         VALUES (?, ?, ?, NOW())
         ON DUPLICATE KEY UPDATE 
           violation_count = VALUES(violation_count),
           last_violation_at = NOW()`,
        [userId, userType, violationCount]
      );

      // 차단 기준: 2회 이상 위반 시 즉시 차단 (서버에서 처리)
      const BLOCK_THRESHOLD = 2;
      const BLOCK_DURATION_HOURS = 24;
      let blockedUntil = null;
      let isBlocked = false;

      if (violationCount >= BLOCK_THRESHOLD) {
        // 이미 차단되어 있는지 확인
        const currentBlockStatus = await this.isUserBlocked(userId, userType);
        
        if (!currentBlockStatus.isBlocked) {
          // 즉시 차단 처리
          blockedUntil = new Date();
          blockedUntil.setHours(blockedUntil.getHours() + BLOCK_DURATION_HOURS);
          isBlocked = true;

          // 차단 기록 저장/업데이트
          await pool.execute(
            `INSERT INTO BlockedUser (user_id, user_type, blocked_until, reason, violation_count)
             VALUES (?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE 
               blocked_until = VALUES(blocked_until),
               reason = VALUES(reason),
               violation_count = VALUES(violation_count)`,
            [
              userId,
              userType,
              blockedUntil,
              `금칙어 사용 ${violationCount}회 위반 (서버 자동 차단)`,
              violationCount,
            ]
          );
          
          console.log(`🔒 사용자 즉시 차단: userId=${userId}, userType=${userType}, 위반횟수=${violationCount} (Jenkins 검토 권장)`);
        } else {
          // 이미 차단되어 있음
          isBlocked = true;
          blockedUntil = currentBlockStatus.blockUntil;
        }
      }

      return {
        isBlocked,
        violationCount,
        blockUntil: blockedUntil,
      };
    } catch (error) {
      console.error('위반 기록 저장 실패:', error);
      return {
        isBlocked: false,
        violationCount: 0,
        blockUntil: null,
      };
    }
  }

  /**
   * 사용자가 차단되어 있는지 확인
   * 수동 해제만 가능 (자동 해제 없음)
   * @param {number} userId - 사용자 ID
   * @param {string} userType - 사용자 타입
   * @returns {Object} { isBlocked: boolean, blockUntil: Date | null, reason: string | null }
   */
  async isUserBlocked(userId, userType) {
    try {
      // 자동 해제 없음: blocked_until 시간과 관계없이 차단 상태 확인
      const [rows] = await pool.execute(
        `SELECT blocked_until, reason 
         FROM BlockedUser 
         WHERE user_id = ? AND user_type = ?`,
        [userId, userType]
      );

      if (rows.length === 0) {
        return { isBlocked: false, blockUntil: null, reason: null };
      }

      const { blocked_until, reason } = rows[0];
      return {
        isBlocked: true,
        blockUntil: blocked_until ? new Date(blocked_until) : null,
        reason,
      };
    } catch (error) {
      console.error('차단 상태 확인 실패:', error);
      return { isBlocked: false, blockUntil: null, reason: null };
    }
  }

  /**
   * 사용자 차단 해제 (관리자용)
   */
  async unblockUser(userId, userType) {
    try {
      await pool.execute(
        'DELETE FROM BlockedUser WHERE user_id = ? AND user_type = ?',
        [userId, userType]
      );
      // 위반 횟수도 초기화
      await pool.execute(
        'UPDATE UserViolation SET violation_count = 0 WHERE user_id = ? AND user_type = ?',
        [userId, userType]
      );
      return { success: true };
    } catch (error) {
      console.error('차단 해제 실패:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * 만료된 차단 자동 해제 기능 제거됨
   * 이제 수동 해제만 가능합니다.
   * @deprecated 자동 해제 기능이 제거되었습니다. unblockUser()를 사용하세요.
   */
  async unblockExpiredUsers() {
    console.warn('⚠️  자동 해제 기능이 제거되었습니다. 수동 해제만 가능합니다.');
    return { success: false, error: '자동 해제 기능이 제거되었습니다. 수동 해제만 가능합니다.' };
  }

  /**
   * 사용자 위반 기록 조회
   */
  async getUserViolationHistory(userId, userType) {
    try {
      const [rows] = await pool.execute(
        `SELECT violation_count, last_violation_at 
         FROM UserViolation 
         WHERE user_id = ? AND user_type = ?`,
        [userId, userType]
      );
      return rows.length > 0 ? rows[0] : null;
    } catch (error) {
      console.error('위반 기록 조회 실패:', error);
      return null;
    }
  }

  /**
   * 위반 기록 초기화
   */
  async resetViolations(userId, userType) {
    try {
      const [result] = await pool.execute(
        'UPDATE UserViolation SET violation_count = 0 WHERE user_id = ? AND user_type = ?',
        [userId, userType]
      );
      
      if (result.affectedRows > 0) {
        console.log(`✅ 위반 기록 초기화: userId=${userId}, userType=${userType}`);
        return { success: true, message: '위반 기록이 초기화되었습니다.' };
      } else {
        // 위반 기록이 없으면 삭제 시도 (없어도 성공으로 처리)
        return { success: true, message: '위반 기록이 없습니다.' };
      }
    } catch (error) {
      console.error('위반 기록 초기화 실패:', error);
      return { success: false, error: error.message };
    }
  }
}

module.exports = new BlockService();

