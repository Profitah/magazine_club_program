package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.AdminDTO;
import com.club.magazine_club_program.DTO.AuthenticatorSetupDTO;
import com.club.magazine_club_program.DTO.AuthenticatorVerifyDTO;
import com.club.magazine_club_program.Mapper.AdminMapper;
import com.club.magazine_club_program.Service.AuthenticatorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AuthenticatorService authenticatorService;
    private final AdminMapper adminMapper;

    public AdminController(AuthenticatorService authenticatorService, AdminMapper adminMapper) {
        this.authenticatorService = authenticatorService;
        this.adminMapper = adminMapper;
    }

    /**
     * Authenticator 초기 설정 (QR 코드 생성)
     * POST /admin/setup-authenticator
     * 요청 본문: {"email": "admin@example.com"}
     */
    @PostMapping("/setup-authenticator")
    public ResponseEntity<?> setupAuthenticator(@RequestBody AuthenticatorVerifyDTO request) {
        try {
            String email = request.getEmail();
            
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
            }

            // TOTP Secret 생성 및 QR 코드 생성
            var secretAndQrCode = authenticatorService.generateSecretAndQrCode(email, "Magazine Club");
            String secret = secretAndQrCode.getSecret();
            String qrCode = secretAndQrCode.getQrCode();

            // DB에 Secret 저장 (기존 관리자면 업데이트, 없으면 생성)
            AdminDTO existingAdmin = adminMapper.findByEmail(email);
            if (existingAdmin != null) {
                adminMapper.updateTotpSecret(email, secret);
            } else {
                adminMapper.insertAdmin(email, secret);
            }

            AuthenticatorSetupDTO response = new AuthenticatorSetupDTO(email, qrCode, secret);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Authenticator 설정 실패: " + e.getMessage());
        }
    }

    /**
     * Authenticator 코드 검증 및 로그인
     * POST /admin/verify-authenticator
     * 요청 본문: {"email": "admin@example.com", "code": "123456"}
     */
    @PostMapping("/verify-authenticator")
    public ResponseEntity<?> verifyAuthenticator(@RequestBody AuthenticatorVerifyDTO request, HttpSession session) {
        try {
            String email = request.getEmail();
            String code = request.getCode();

            // 입력값 검증
            if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("이메일과 인증 코드를 모두 입력해주세요.");
            }

            // 관리자 조회
            AdminDTO admin = adminMapper.findByEmail(email);
            if (admin == null || admin.getTotpSecret() == null) {
                return ResponseEntity.badRequest().body("등록되지 않은 관리자이거나 Authenticator가 설정되지 않았습니다.");
            }

            // TOTP 코드 검증
            if (!authenticatorService.verifyCode(admin.getTotpSecret(), code)) {
                return ResponseEntity.badRequest().body("인증 코드가 올바르지 않습니다.");
            }

            // 세션에 관리자 정보 저장
            session.setAttribute("adminEmail", email);
            session.setAttribute("authenticated", true);

            return ResponseEntity.ok("로그인 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("인증 실패: " + e.getMessage());
        }
    }

    /**
     * 로그아웃
     * POST /admin/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("로그아웃되었습니다.");
    }

    /**
     * 로그인 상태 확인
     * GET /admin/check-auth
     */
    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuth(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated != null && authenticated) {
            String email = (String) session.getAttribute("adminEmail");
            return ResponseEntity.ok("인증됨: " + email);
        }
        return ResponseEntity.status(401).body("인증되지 않음");
    }
}

