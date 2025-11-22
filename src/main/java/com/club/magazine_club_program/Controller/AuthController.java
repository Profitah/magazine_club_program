package com.club.magazine_club_program.Controller;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 일반 사용자 인증 컨트롤러
 * 카카오/구글 로그아웃 통합 처리
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * 일반 사용자 로그아웃 (카카오/구글 통합)
     * POST /auth/logout
     * 
     * JWT 토큰 중심 인증이므로 세션 무효화는 선택사항.
     * 클라이언트에서 JWT 토큰 삭제만 하면 됨.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 세션이 존재하면 무효화 (하위 호환성)
            if (session != null) {
                String email = (String) session.getAttribute("email");
                Integer memberId = (Integer) session.getAttribute("memberId");
                
                if (email != null || memberId != null) {
                    session.invalidate();
                    log.info("로그아웃 성공 (세션 무효화): memberId={}, email={}", memberId, email);
                }
            }
            
            response.put("success", true);
            response.put("message", "로그아웃 성공");
            response.put("note", "JWT 토큰은 클라이언트에서 삭제해주세요");
            
        } catch (Exception e) {
            log.error("로그아웃 처리 중 에러 발생", e);
            response.put("success", false);
            response.put("message", "로그아웃 처리 중 오류가 발생했습니다");
        }
        
        return ResponseEntity.ok(response);
    }
}

