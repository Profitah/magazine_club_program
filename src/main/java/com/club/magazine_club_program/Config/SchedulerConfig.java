package com.club.magazine_club_program.Config;

import com.club.magazine_club_program.Service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerConfig {
    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);
    private final MemberService memberService;

    public SchedulerConfig(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 만료된 회원 자격 일시정지 자동 해제 (1시간마다 실행)
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1시간마다
    public void unsuspendExpiredMembers() {
        try {
            int unsuspendedCount = memberService.unsuspendExpiredMembers();
            if (unsuspendedCount > 0) {
                log.info("만료된 회원 자격 일시정지 자동 해제: {}명", unsuspendedCount);
            }
        } catch (Exception e) {
            log.error("만료된 회원 자격 일시정지 자동 해제 실패: {}", e.getMessage(), e);
        }
    }
}

