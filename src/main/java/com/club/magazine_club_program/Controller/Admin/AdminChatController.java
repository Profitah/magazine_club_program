package com.club.magazine_club_program.Controller.Admin;

import com.club.magazine_club_program.DTO.MemberDTO;
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
@RequestMapping("/admin/chat")
public class AdminChatController {

    private final ChatServerService chatServerService;
    private final MemberService memberService;
    private final BannedEmailService bannedEmailService;

    public AdminChatController(
            ChatServerService chatServerService,
            MemberService memberService,
            BannedEmailService bannedEmailService) {
        this.chatServerService = chatServerService;
        this.memberService = memberService;
        this.bannedEmailService = bannedEmailService;
    }

    /**
     * 채팅 차단된 회원 목록 조회 (채팅 서버에서 조회)
     * GET /admin/chat/blocked-users
     */
    @GetMapping("/blocked-users")
    public ResponseEntity<?> getBlockedUsers(HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            List<Map<String, Object>> blockedUsers = chatServerService.getBlockedUsers();
            
            for (Map<String, Object> blockedUser : blockedUsers) {
                Integer userId = (Integer) blockedUser.get("user_id");
                String userType = (String) blockedUser.get("user_type");
                
                if (userId != null && "member".equals(userType)) {
                    List<MemberDTO> members = memberService.getAllMembers();
                    MemberDTO member = members.stream()
                            .filter(m -> m.getId() == userId)
                            .findFirst()
                            .orElse(null);
                    
                    if (member != null && member.getEmail() != null) {
                        blockedUser.put("email", member.getEmail());
                        blockedUser.put("name", member.getName());
                        
                        boolean isPermanentlyBanned = bannedEmailService.isEmailBanned(member.getEmail());
                        blockedUser.put("isPermanentlyBanned", isPermanentlyBanned);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", blockedUsers.size());
            response.put("blockedUsers", blockedUsers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("차단된 회원 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 채팅 차단만 해제 (영구 차단은 유지)
     * POST /admin/chat/unblock
     */
    @PostMapping("/unblock")
    public ResponseEntity<?> unblockChat(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            Integer userId = request.get("userId") != null ? (Integer) request.get("userId") : null;
            String userType = (String) request.get("userType");
            String email = (String) request.get("email");

            if (userId == null && email != null) {
                List<MemberDTO> members = memberService.getAllMembers();
                MemberDTO member = members.stream()
                        .filter(m -> email.equals(m.getEmail()))
                        .findFirst()
                        .orElse(null);
                if (member != null) {
                    userId = member.getId();
                    userType = "member";
                }
            }

            if (userId == null || userType == null) {
                return ResponseEntity.badRequest().body("userId와 userType 또는 email을 입력해주세요.");
            }

            boolean success = chatServerService.unblockUser(userId, userType);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "채팅 차단 해제 완료");
                response.put("userId", userId);
                response.put("userType", userType);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("채팅 차단 해제 실패: 차단된 사용자를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("채팅 차단 해제 실패: " + e.getMessage());
        }
    }

    /**
     * 위반 기록 초기화 (채팅 서버)
     * POST /admin/chat/reset-violations
     */
    @PostMapping("/reset-violations")
    public ResponseEntity<?> resetViolations(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!AdminAuthUtil.isAdminAuthenticated(session)) {
            return ResponseEntity.status(401).body("관리자 인증이 필요합니다.");
        }

        try {
            Integer userId = request.get("userId") != null ? (Integer) request.get("userId") : null;
            String userType = (String) request.get("userType");
            String email = (String) request.get("email");

            if (userId == null && email != null) {
                List<MemberDTO> members = memberService.getAllMembers();
                MemberDTO member = members.stream()
                        .filter(m -> email.equals(m.getEmail()))
                        .findFirst()
                        .orElse(null);
                if (member != null) {
                    userId = member.getId();
                    userType = "member";
                }
            }

            if (userId == null || userType == null) {
                return ResponseEntity.badRequest().body("userId와 userType 또는 email을 입력해주세요.");
            }

            boolean success = chatServerService.resetViolations(userId, userType);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "위반 기록 초기화 완료");
                response.put("userId", userId);
                response.put("userType", userType);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("위반 기록 초기화 실패: 위반 기록을 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("위반 기록 초기화 실패: " + e.getMessage());
        }
    }
}

