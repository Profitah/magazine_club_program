package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.Util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 일반 사용자 인증 컨트롤러
 * 카카오/구글 로그아웃 통합 처리 및 토큰 갱신
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

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

    /**
     * Refresh Token으로 Access Token 갱신
     * POST /auth/refresh
     * 
     * Refresh Token을 받아서 새로운 Access Token을 발급합니다.
     * 
     * @param request Refresh Token이 포함된 요청
     * @return 새로운 Access Token 정보
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "refreshToken이 필요합니다");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Refresh Token으로 새로운 Access Token 발급
            Map<String, Object> tokenInfo = jwtUtil.refreshAccessToken(refreshToken);
            
            response.put("success", true);
            response.put("message", "Access Token 갱신 성공");
            response.putAll(tokenInfo);
            
            log.info("Access Token 갱신 성공");
            
        } catch (IllegalArgumentException e) {
            log.warn("Access Token 갱신 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            log.error("Access Token 갱신 처리 중 에러 발생", e);
            response.put("success", false);
            response.put("message", "토큰 갱신 처리 중 오류가 발생했습니다");
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}

