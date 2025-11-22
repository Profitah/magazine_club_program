package com.club.magazine_club_program.Controller.Admin;

import com.club.magazine_club_program.DTO.AdminDTO;
import com.club.magazine_club_program.DTO.AuthenticatorSetupDTO;
import com.club.magazine_club_program.DTO.AuthenticatorVerifyDTO;
import com.club.magazine_club_program.Mapper.AdminMapper;
import com.club.magazine_club_program.Service.AuthenticatorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AuthenticatorService authenticatorService;
    private final AdminMapper adminMapper;

    public AdminAuthController(
            AuthenticatorService authenticatorService,
            AdminMapper adminMapper) {
        this.authenticatorService = authenticatorService;
        this.adminMapper = adminMapper;
    }

    /**
     * Authenticator 초기 설정 (QR 코드 생성)
     * POST /admin/auth/setup-authenticator
     */
    @PostMapping("/setup-authenticator")
    public ResponseEntity<?> setupAuthenticator(@RequestBody AuthenticatorVerifyDTO request) {
        try {
            String email = request.getEmail();
            
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
            }

            // 1. Secret 및 QR 코드 생성
            var secretAndQrCode = authenticatorService.generateSecretAndQrCode(email, "Magazine Club");
            String secret = secretAndQrCode.getSecret();
            String qrCode = secretAndQrCode.getQrCode();

            // 2. DB에 저장 (DB 연결 실패 시에도 QR 코드는 반환)
            try {
                AdminDTO existingAdmin = adminMapper.findByEmail(email);
                if (existingAdmin != null) {
                    adminMapper.updateTotpSecret(email, secret);
                } else {
                    adminMapper.insertAdmin(email, secret);
                }
            } catch (Exception dbException) {
                // DB 저장 실패해도 QR 코드는 반환 (사용자가 QR 코드를 스캔할 수 있도록)
                System.err.println("DB 저장 실패 (QR 코드는 반환): " + dbException.getMessage());
            }

            AuthenticatorSetupDTO response = new AuthenticatorSetupDTO(email, qrCode, secret);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 상세한 오류 정보 로깅
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (e.getCause() != null) {
                String causeMsg = e.getCause().getMessage();
                errorMsg += " (원인: " + (causeMsg != null ? causeMsg : e.getCause().getClass().getSimpleName()) + ")";
            }
            // 스택 트레이스도 포함
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println("Authenticator 설정 오류:\n" + sw.toString());
            
            return ResponseEntity.badRequest().body("Authenticator 설정 실패: " + errorMsg);
        }
    }

    /**
     * Authenticator 코드 검증 및 로그인
     * POST /admin/auth/verify-authenticator
     */
    @PostMapping("/verify-authenticator")
    public ResponseEntity<?> verifyAuthenticator(@RequestBody AuthenticatorVerifyDTO request, HttpSession session) {
        try {
            String email = request.getEmail();
            String code = request.getCode();

            if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("이메일과 인증 코드를 모두 입력해주세요.");
            }

            AdminDTO admin = adminMapper.findByEmail(email);
            if (admin == null) {
                return ResponseEntity.badRequest().body("등록되지 않은 관리자입니다. 먼저 Authenticator를 설정해주세요.");
            }
            
            if (admin.getTotpSecret() == null || admin.getTotpSecret().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Authenticator가 설정되지 않았습니다. 먼저 Authenticator를 설정해주세요.");
            }

            // 디버깅: secret과 code 로깅
            System.out.println("인증 시도 - Email: " + email + ", Secret: " + admin.getTotpSecret() + ", Code: " + code);
            
            boolean verified = authenticatorService.verifyCode(admin.getTotpSecret(), code);
            System.out.println("인증 결과: " + verified);
            
            if (!verified) {
                return ResponseEntity.badRequest().body("인증 코드가 올바르지 않습니다. 코드를 다시 확인해주세요. (Secret: " + admin.getTotpSecret().substring(0, Math.min(10, admin.getTotpSecret().length())) + "...)");
            }

            session.setAttribute("adminEmail", email);
            session.setAttribute("authenticated", true);

            return ResponseEntity.ok("로그인 성공");
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (e.getCause() != null) {
                String causeMsg = e.getCause().getMessage();
                errorMsg += " (원인: " + (causeMsg != null ? causeMsg : e.getCause().getClass().getSimpleName()) + ")";
            }
            // 스택 트레이스 로깅
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println("인증 오류:\n" + sw.toString());
            
            return ResponseEntity.badRequest().body("인증 실패: " + errorMsg);
        }
    }

    /**
     * 로그아웃
     * POST /admin/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("로그아웃되었습니다.");
    }

    /**
     * 로그인 상태 확인
     * GET /admin/auth/check-auth
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

    /**
     * 채팅 서버에서 사용하는 관리자 세션 검증
     * POST /admin/auth/verify-for-chat
     */
    @PostMapping("/verify-for-chat")
    public ResponseEntity<?> verifyForChat(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "authenticated", false,
                    "message", "이메일을 입력해주세요."
            ));
        }

        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        String sessionEmail = (String) session.getAttribute("adminEmail");

        if (authenticated == null || !authenticated || sessionEmail == null || !sessionEmail.equals(email)) {
            return ResponseEntity.status(401).body(Map.of(
                    "authenticated", false,
                    "message", "세션이 만료되었거나 이메일이 일치하지 않습니다."
            ));
        }

        AdminDTO admin = adminMapper.findByEmail(email);
        if (admin == null || admin.getId() == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "authenticated", false,
                    "message", "관리자 정보를 찾을 수 없습니다."
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("adminId", admin.getId());
        response.put("email", admin.getEmail());

        return ResponseEntity.ok(response);
    }
}