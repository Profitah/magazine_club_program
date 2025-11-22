package com.club.magazine_club_program.Controller.Admin;

import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.Service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/member")
public class AdminMemberController {

    private final MemberService memberService;

    public AdminMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 회원 자격 일시정지 (30일 후 자동 해제)
     * POST /admin/member/suspend
     */
    @PostMapping("/suspend")
    public ResponseEntity<?> suspendMember(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            Integer memberId = request.get("memberId") != null ? (Integer) request.get("memberId") : null;
            String email = (String) request.get("email");
            String adminEmail = AdminAuthUtil.getAdminEmail(session);

            if (memberId == null && email != null) {
                List<MemberDTO> members = memberService.getAllMembers();
                MemberDTO member = members.stream()
                        .filter(m -> email.equals(m.getEmail()))
                        .findFirst()
                        .orElse(null);
                if (member != null) {
                    memberId = member.getId();
                } else {
                    return ResponseEntity.badRequest().body("회원을 찾을 수 없습니다.");
                }
            }

            if (memberId == null) {
                return ResponseEntity.badRequest().body("memberId 또는 email을 입력해주세요.");
            }

            java.time.LocalDateTime suspendedUntil = java.time.LocalDateTime.now().plusDays(30);

            boolean success = memberService.suspendMember(memberId, suspendedUntil, adminEmail);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "회원 자격 일시정지 완료 (30일 후 자동 해제)");
                response.put("memberId", memberId);
                response.put("suspendedUntil", suspendedUntil.toString());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("회원 자격 일시정지 실패: 이미 정지되었거나 오류 발생");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("회원 자격 일시정지 실패: " + e.getMessage());
        }
    }

    /**
     * 회원 자격 일시정지 해제
     * POST /admin/member/unsuspend
     */
    @PostMapping("/unsuspend")
    public ResponseEntity<?> unsuspendMember(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            Integer memberId = request.get("memberId") != null ? (Integer) request.get("memberId") : null;
            String email = (String) request.get("email");

            if (memberId == null && email != null) {
                List<MemberDTO> members = memberService.getAllMembers();
                MemberDTO member = members.stream()
                        .filter(m -> email.equals(m.getEmail()))
                        .findFirst()
                        .orElse(null);
                if (member != null) {
                    memberId = member.getId();
                } else {
                    return ResponseEntity.badRequest().body("회원을 찾을 수 없습니다.");
                }
            }

            if (memberId == null) {
                return ResponseEntity.badRequest().body("memberId 또는 email을 입력해주세요.");
            }

            boolean success = memberService.unsuspendMember(memberId);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "회원 자격 일시정지 해제 완료");
                response.put("memberId", memberId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("회원 자격 일시정지 해제 실패: 정지되지 않은 회원이거나 오류 발생");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("회원 자격 일시정지 해제 실패: " + e.getMessage());
        }
    }
}

