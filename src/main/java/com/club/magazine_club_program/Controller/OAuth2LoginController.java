package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.Service.KakaoLoginService;
import com.club.magazine_club_program.Service.GoogleLoginService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 공통 로그인 컨트롤러
 * SecurityConfig의 defaultSuccessUrl에서 사용
 */
@RestController
@RequestMapping("/login/oauth2")
public class OAuth2LoginController {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginController.class);
    private final KakaoLoginService kakaoLoginService;
    private final GoogleLoginService googleLoginService;

    public OAuth2LoginController(
            KakaoLoginService kakaoLoginService,
            GoogleLoginService googleLoginService) {
        this.kakaoLoginService = kakaoLoginService;
        this.googleLoginService = googleLoginService;
    }

    /**
     * OAuth2 로그인 성공 핸들러 (공통)
     * 제공자에 따라 적절한 서비스로 라우팅
     * GET /login/oauth2/success
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> loginSuccess(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        
        if (oauth2User == null) {
            response.put("success", false);
            response.put("message", "로그인 실패: OAuth2User가 null입니다");
            log.warn("OAuth2 로그인 실패: OAuth2User가 null");
            return ResponseEntity.ok(response);
        }

        // OAuth2User의 attributes에서 registrationId 가져오기
        String registrationId = (String) oauth2User.getAttribute("registrationId");
        
        if (registrationId == null) {
            // registrationId가 없으면 에러 처리
            response.put("success", false);
            response.put("message", "로그인 실패: 제공자 정보를 찾을 수 없습니다");
            log.error("OAuth2 로그인 실패: registrationId를 찾을 수 없음");
            return ResponseEntity.ok(response);
        }

        // 제공자별 서비스 호출
        try {
            switch (registrationId) {
                case "kakao":
                    response = kakaoLoginService.handleLoginSuccess(oauth2User, session);
                    break;
                case "google":
                    response = googleLoginService.handleLoginSuccess(oauth2User, session);
                    break;
                default:
                    response.put("success", false);
                    response.put("message", "지원하지 않는 로그인 제공자입니다: " + registrationId);
                    log.warn("지원하지 않는 OAuth2 제공자: {}", registrationId);
            }
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: provider={}", registrationId, e);
            response.put("success", false);
            response.put("message", "로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * OAuth2 로그인 실패 핸들러 (공통)
     * GET /login/oauth2/failure
     */
    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> loginFailure(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        
        // 에러 정보가 있으면 포함
        if (error != null) {
            response.put("error", error);
            log.warn("OAuth2 로그인 실패: error={}, description={}", error, errorDescription);
        }
        
        // 사용자 친화적인 메시지 제공
        String userMessage = "소셜 로그인에 실패했습니다.";
        if (error != null) {
            switch (error) {
                case "access_denied":
                    userMessage = "로그인이 취소되었습니다.";
                    break;
                case "invalid_request":
                    userMessage = "잘못된 요청입니다.";
                    break;
                default:
                    userMessage = "소셜 로그인 중 오류가 발생했습니다: " + error;
            }
        }
        
        response.put("message", userMessage);
        if (errorDescription != null) {
            response.put("errorDescription", errorDescription);
        }
        
        return ResponseEntity.ok(response);
    }
}

