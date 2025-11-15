package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.Service.KakaoLoginService;
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
 * 카카오 로그인 컨트롤러
 */
@RestController
@RequestMapping("/login/kakao")
public class KakaoLoginController {

    private static final Logger log = LoggerFactory.getLogger(KakaoLoginController.class);
    private final KakaoLoginService kakaoLoginService;

    public KakaoLoginController(KakaoLoginService kakaoLoginService) {
        this.kakaoLoginService = kakaoLoginService;
    }

    /**
     * 카카오 로그인 성공 핸들러
     * GET /login/kakao/success
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> loginSuccess(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpSession session
    ) {
        Map<String, Object> response = kakaoLoginService.handleLoginSuccess(oauth2User, session);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오 로그인 실패 핸들러
     * GET /login/kakao/failure
     */
    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> loginFailure() {
        Map<String, Object> response = kakaoLoginService.handleLoginFailure();
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     * GET /login/kakao/user
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpSession session
    ) {
        Map<String, Object> response = kakaoLoginService.getCurrentUser(oauth2User, session);
        return ResponseEntity.ok(response);
    }
}