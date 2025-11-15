package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.Service.GoogleLoginService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 구글 로그인 컨트롤러
 */
@RestController
@RequestMapping("/login/google")
public class GoogleLoginController {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginController.class);
    private final GoogleLoginService googleLoginService;

    public GoogleLoginController(GoogleLoginService googleLoginService) {
        this.googleLoginService = googleLoginService;
    }

    /**
     * 구글 로그인 성공 핸들러
     * GET /login/google/success
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> loginSuccess(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpSession session
    ) {
        Map<String, Object> response = googleLoginService.handleLoginSuccess(oauth2User, session);
        return ResponseEntity.ok(response);
    }

    /**
     * 구글 로그인 실패 핸들러
     * GET /login/google/failure
     */
    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> loginFailure() {
        Map<String, Object> response = googleLoginService.handleLoginFailure();
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     * GET /login/google/user
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpSession session
    ) {
        Map<String, Object> response = googleLoginService.getCurrentUser(oauth2User, session);
        return ResponseEntity.ok(response);
    }
}

