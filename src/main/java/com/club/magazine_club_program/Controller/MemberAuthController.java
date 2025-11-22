package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.LoginRequest;
import com.club.magazine_club_program.DTO.RegisterRequest;
import com.club.magazine_club_program.Service.MemberAuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 일반 로그인 회원 인증 컨트롤러
 * 새로운 파일로 생성하여 기존 코드와 분리
 * - 일반 로그인 (이메일/비밀번호)
 * - 회원가입
 * - 로그아웃은 AuthController 사용 (통합)
 * - 리프레시는 AuthController 사용 (통합)
 */
@RestController
@RequestMapping("/auth/member")
public class MemberAuthController {

    private static final Logger log = LoggerFactory.getLogger(MemberAuthController.class);
    private final MemberAuthService memberAuthService;

    public MemberAuthController(MemberAuthService memberAuthService) {
        this.memberAuthService = memberAuthService;
    }

    /**
     * 일반 로그인
     * POST /auth/member/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> response = memberAuthService.login(
                request.getEmail(), 
                request.getPassword()
        );
        
        if ((Boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * 회원가입
     * POST /auth/member/register
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> response = memberAuthService.register(request);
        
        if ((Boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(400).body(response);
        }
    }
}

