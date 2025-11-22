package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.BannedEmailDTO;
import com.club.magazine_club_program.Mapper.BannedEmailMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannedEmailService {
    private static final Logger log = LoggerFactory.getLogger(BannedEmailService.class);
    private final BannedEmailMapper bannedEmailMapper;

    public BannedEmailService(BannedEmailMapper bannedEmailMapper) {
        this.bannedEmailMapper = bannedEmailMapper;
    }

    /**
     * 이메일이 차단되어 있는지 확인
     */
    public boolean isEmailBanned(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        BannedEmailDTO banned = bannedEmailMapper.findByEmail(email.trim().toLowerCase());
        return banned != null;
    }

    /**
     * 회원 ID가 차단되어 있는지 확인
     */
    public boolean isMemberBanned(int memberId) {
        BannedEmailDTO banned = bannedEmailMapper.findByMemberId(memberId);
        return banned != null;
    }

    /**
     * 차단 정보 조회 (이메일로)
     */
    public BannedEmailDTO getBannedInfo(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return bannedEmailMapper.findByEmail(email.trim().toLowerCase());
    }

    /**
     * 차단 정보 조회 (회원 ID로)
     */
    public BannedEmailDTO getBannedInfoByMemberId(int memberId) {
        return bannedEmailMapper.findByMemberId(memberId);
    }

    /**
     * 이메일 차단 (영구 차단)
     */
    public boolean banEmail(String email, String reason, int memberId, String bannedBy, String banType) {
        try {
            if (email == null || email.trim().isEmpty()) {
                log.warn("차단 시도: 이메일이 비어있음");
                return false;
            }

            // 이미 차단되어 있는지 확인 (만료된 일시정지는 제외)
            if (isEmailBanned(email)) {
                log.warn("이미 차단된 이메일: {}", email);
                return false;
            }

            BannedEmailDTO bannedEmail = new BannedEmailDTO(
                    email.trim().toLowerCase(),
                    reason,
                    memberId,
                    bannedBy,
                    banType
            );

            int result = bannedEmailMapper.insertBannedEmail(bannedEmail);
            log.info("이메일 차단 완료: email={}, reason={}, banType={}, bannedBy={}", 
                    email, reason, banType, bannedBy);
            return result > 0;
        } catch (Exception e) {
            log.error("이메일 차단 실패: email={}, error={}", email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 차단 해제 (이메일로)
     */
    public boolean unbanEmail(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return false;
            }
            int result = bannedEmailMapper.deleteByEmail(email.trim().toLowerCase());
            if (result > 0) {
                log.info("이메일 차단 해제 완료: email={}", email);
            }
            return result > 0;
        } catch (Exception e) {
            log.error("이메일 차단 해제 실패: email={}, error={}", email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 차단 해제 (회원 ID로)
     */
    public boolean unbanMember(int memberId) {
        try {
            int result = bannedEmailMapper.deleteByMemberId(memberId);
            if (result > 0) {
                log.info("회원 차단 해제 완료: memberId={}", memberId);
            }
            return result > 0;
        } catch (Exception e) {
            log.error("회원 차단 해제 실패: memberId={}, error={}", memberId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 차단 해제 (ID로)
     */
    public boolean unbanById(int id) {
        try {
            int result = bannedEmailMapper.deleteById(id);
            if (result > 0) {
                log.info("차단 해제 완료: id={}", id);
            }
            return result > 0;
        } catch (Exception e) {
            log.error("차단 해제 실패: id={}, error={}", id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 모든 차단된 이메일 조회
     */
    public List<BannedEmailDTO> getAllBannedEmails() {
        return bannedEmailMapper.findAll();
    }

}

