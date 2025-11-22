package com.club.magazine_club_program.Controller.Admin;

import com.club.magazine_club_program.DTO.BannedEmailDTO;
import com.club.magazine_club_program.Service.BannedEmailService;
import com.club.magazine_club_program.Service.MemberService;
import com.club.magazine_club_program.Service.ChatServerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/ban")
public class AdminBanController {

    private final BannedEmailService bannedEmailService;
    private final MemberService memberService;
    private final ChatServerService chatServerService;

    public AdminBanController(
            BannedEmailService bannedEmailService,
            MemberService memberService,
            ChatServerService chatServerService) {
        this.bannedEmailService = bannedEmailService;
        this.memberService = memberService;
        this.chatServerService = chatServerService;
    }

    /**
     * 회원 로그인 정지 (이메일 기반 영구 차단)
     * POST /admin/ban/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> banLogin(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            String email = (String) request.get("email");
            String reason = (String) request.get("reason");
            Integer memberId = request.get("memberId") != null ? (Integer) request.get("memberId") : 0;
            String adminEmail = AdminAuthUtil.getAdminEmail(session);

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
            }

            boolean success = bannedEmailService.banEmail(
                    email,
                    reason != null ? reason : "관리자에 의한 로그인 정지",
                    memberId,
                    adminEmail,
                    "LOGIN_BAN"
            );

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "로그인 정지 완료");
                response.put("email", email);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("로그인 정지 실패: 이미 차단되었거나 오류 발생");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("로그인 정지 실패: " + e.getMessage());
        }
    }

    /**
     * 회원 서비스 방출 (이메일 기반 영구 차단 + 회원 정보 삭제)
     * POST /admin/ban/service
     */
    @PostMapping("/service")
    public ResponseEntity<?> banService(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            String email = (String) request.get("email");
            String reason = (String) request.get("reason");
            Integer memberId = request.get("memberId") != null ? (Integer) request.get("memberId") : 0;
            String adminEmail = AdminAuthUtil.getAdminEmail(session);

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
            }

            boolean banSuccess = bannedEmailService.banEmail(
                    email,
                    reason != null ? reason : "관리자에 의한 서비스 방출",
                    memberId,
                    adminEmail,
                    "SERVICE_BAN"
            );

            boolean deleteSuccess = true;
            if (memberId > 0) {
                deleteSuccess = memberService.deleteMember(memberId);
            }

            Map<String, Object> response = new HashMap<>();
            if (banSuccess) {
                response.put("success", true);
                response.put("message", "서비스 방출 완료");
                response.put("email", email);
                response.put("memberDeleted", deleteSuccess);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("서비스 방출 실패: 이미 차단되었거나 오류 발생");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("서비스 방출 실패: " + e.getMessage());
        }
    }

    /**
     * 채팅 차단된 회원을 영구 차단으로 전환
     * POST /admin/ban/permanent
     */
    @PostMapping("/permanent")
    public ResponseEntity<?> banPermanent(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            String email = (String) request.get("email");
            String reason = (String) request.get("reason");
            Integer memberId = request.get("memberId") != null ? (Integer) request.get("memberId") : null;
            Integer userId = request.get("userId") != null ? (Integer) request.get("userId") : null;
            String userType = (String) request.get("userType");
            String adminEmail = AdminAuthUtil.getAdminEmail(session);

            if ((email == null || email.trim().isEmpty()) && userId != null && userType != null) {
                if ("member".equals(userType)) {
                    final Integer finalUserId = userId;
                    List<com.club.magazine_club_program.DTO.MemberDTO> members = memberService.getAllMembers();
                    com.club.magazine_club_program.DTO.MemberDTO member = members.stream()
                            .filter(m -> m.getId() == finalUserId)
                            .findFirst()
                            .orElse(null);
                    
                    if (member != null && member.getEmail() != null) {
                        email = member.getEmail();
                        if (memberId == null) {
                            memberId = member.getId();
                        }
                    } else {
                        return ResponseEntity.badRequest().body("회원 정보를 찾을 수 없거나 이메일이 없습니다.");
                    }
                } else {
                    return ResponseEntity.badRequest().body("관리자는 영구 차단할 수 없습니다.");
                }
            }

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
            }

            String finalReason = reason;
            if (userId != null && userType != null) {
                Map<String, Object> violationHistory = chatServerService.getUserViolationHistory(userId, userType);
                if (violationHistory != null) {
                    Integer violationCount = (Integer) violationHistory.get("violation_count");
                    if (violationCount != null && violationCount > 0) {
                        finalReason = (reason != null ? reason + " " : "") + 
                                     "(채팅 위반 횟수: " + violationCount + "회)";
                    }
                }
            }

            boolean success = bannedEmailService.banEmail(
                    email,
                    finalReason != null ? finalReason : "채팅 차단으로 인한 영구 차단",
                    memberId != null ? memberId : 0,
                    adminEmail,
                    "PERMANENT_BAN"
            );

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "영구 차단 완료");
                response.put("email", email);
                response.put("memberId", memberId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("영구 차단 실패: 이미 차단되었거나 오류 발생");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("영구 차단 실패: " + e.getMessage());
        }
    }

    /**
     * 통합 차단 해제 (영구 차단 + 채팅 차단 모두 해제)
     * POST /admin/ban/unban
     */
    @PostMapping("/unban")
    public ResponseEntity<?> unban(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            String email = (String) request.get("email");
            Integer id = request.get("id") != null ? (Integer) request.get("id") : null;
            Integer userId = request.get("userId") != null ? (Integer) request.get("userId") : null;
            String userType = (String) request.get("userType");
            Boolean unblockChat = request.get("unblockChat") != null ? (Boolean) request.get("unblockChat") : false;
            Boolean resetViolations = request.get("resetViolations") != null ? (Boolean) request.get("resetViolations") : false;

            Map<String, Object> result = new HashMap<>();
            boolean emailUnbanned = false;
            boolean chatUnblocked = false;
            boolean violationsReset = false;

            if (email != null && !email.trim().isEmpty()) {
                emailUnbanned = bannedEmailService.unbanEmail(email);
                if (emailUnbanned) {
                    result.put("emailUnbanned", true);
                    result.put("email", email);
                }
            } else if (id != null && id > 0) {
                BannedEmailDTO bannedEmail = bannedEmailService.getAllBannedEmails().stream()
                        .filter(b -> b.getId() == id)
                        .findFirst()
                        .orElse(null);
                if (bannedEmail != null) {
                    emailUnbanned = bannedEmailService.unbanById(id);
                    if (emailUnbanned) {
                        result.put("emailUnbanned", true);
                        result.put("email", bannedEmail.getEmail());
                        email = bannedEmail.getEmail();
                    }
                }
            } else if (userId != null && userType != null) {
                final Integer finalUserId = userId;
                List<com.club.magazine_club_program.DTO.MemberDTO> members = memberService.getAllMembers();
                com.club.magazine_club_program.DTO.MemberDTO member = members.stream()
                        .filter(m -> m.getId() == finalUserId)
                        .findFirst()
                        .orElse(null);
                
                if (member != null && member.getEmail() != null) {
                    email = member.getEmail();
                    emailUnbanned = bannedEmailService.unbanEmail(email);
                    if (emailUnbanned) {
                        result.put("emailUnbanned", true);
                        result.put("email", email);
                    }
                }
            }

            if (unblockChat || userId != null) {
                if (userId == null && email != null) {
                    final String finalEmail = email;
                    List<com.club.magazine_club_program.DTO.MemberDTO> members = memberService.getAllMembers();
                    com.club.magazine_club_program.DTO.MemberDTO member = members.stream()
                            .filter(m -> finalEmail.equals(m.getEmail()))
                            .findFirst()
                            .orElse(null);
                    if (member != null) {
                        userId = member.getId();
                        userType = "member";
                    }
                }
                
                if (userId != null && userType != null) {
                    chatUnblocked = chatServerService.unblockUser(userId, userType);
                    if (chatUnblocked) {
                        result.put("chatUnblocked", true);
                        result.put("userId", userId);
                        result.put("userType", userType);
                    }
                }
            }

            if (resetViolations && userId != null && userType != null) {
                violationsReset = chatServerService.resetViolations(userId, userType);
                if (violationsReset) {
                    result.put("violationsReset", true);
                }
            }

            if (emailUnbanned || chatUnblocked) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "차단 해제 완료");
                response.putAll(result);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("차단 해제 실패: 차단된 사용자를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("차단 해제 실패: " + e.getMessage());
        }
    }

    /**
     * 모든 차단된 이메일 조회
     * GET /admin/ban/banned-emails
     */
    @GetMapping("/banned-emails")
    public ResponseEntity<?> getAllBannedEmails(HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            List<BannedEmailDTO> bannedEmails = bannedEmailService.getAllBannedEmails();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", bannedEmails.size());
            response.put("bannedEmails", bannedEmails);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("차단된 이메일 조회 실패: " + e.getMessage());
        }
    }
}

