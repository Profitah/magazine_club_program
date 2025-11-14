package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.Service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login/kakao")
public class KakaoLoginController {

    private static final Logger log = LoggerFactory.getLogger(KakaoLoginController.class);
    private final MemberService memberService;

    public KakaoLoginController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> loginSuccess(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        
        if (oauth2User != null) {
            String email = oauth2User.getAttribute("email");
            String nickname = oauth2User.getAttribute("nickname");
            Integer memberId = oauth2User.getAttribute("memberId");
            String memberName = oauth2User.getAttribute("memberName");

            // 세션에 사용자 정보 저장
            session.setAttribute("email", email);
            session.setAttribute("nickname", nickname);
            session.setAttribute("memberId", memberId);
            session.setAttribute("memberName", memberName);

            // 추가 정보가 필요한지 확인 (DB에 저장된 정보 기준)
            // memberId가 있으면 DB에서 실제 저장된 정보 확인
            boolean needsAdditionalInfo = false;
            MemberDTO dbMember = null;
            
            if (memberId != null) {
                dbMember = memberService.getAllMembers().stream()
                        .filter(m -> m.getId() == memberId)
                        .findFirst()
                        .orElse(null);
                
                if (dbMember != null) {
                    // DB에 저장된 email이 비어있는지 확인
                    needsAdditionalInfo = (dbMember.getEmail() == null || dbMember.getEmail().isEmpty());
                } else {
                    // 회원 정보를 찾을 수 없으면 추가 정보 필요
                    needsAdditionalInfo = true;
                }
            } else {
                // memberId가 없으면 추가 정보 필요
                needsAdditionalInfo = true;
            }

            // DB에서 조회한 실제 값 사용 (null 제거)
            Map<String, Object> userInfo = new HashMap<>();
            if (dbMember != null) {
                // DB에서 조회한 실제 값 사용
                userInfo.put("email", dbMember.getEmail() != null ? dbMember.getEmail() : "");
                userInfo.put("name", dbMember.getName() != null ? dbMember.getName() : "");
            } else {
                // DB에 없으면 OAuth2User에서 가져온 값 사용 (null 제거)
                userInfo.put("email", email != null ? email : "");
                userInfo.put("nickname", nickname != null ? nickname : "");
            }
            
            if (memberId != null) {
                userInfo.put("memberId", memberId);
            }
            if (memberName != null) {
                userInfo.put("memberName", memberName);
            }

            response.put("success", true);
            response.put("message", needsAdditionalInfo ? "추가 정보를 입력해주세요" : "카카오 로그인 성공");
            response.put("user", userInfo);
            response.put("needsAdditionalInfo", needsAdditionalInfo);
            
            // 회원가입 중 플래그 (추가 정보 입력이 필요한 경우)
            response.put("isRegistrationComplete", !needsAdditionalInfo);
            
            log.info("카카오 로그인 성공: memberId={}, name={}, email={}, needsAdditionalInfo={}", 
                    memberId, memberName, email, needsAdditionalInfo);
        } else {
            response.put("success", false);
            response.put("message", "로그인 실패");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> loginFailure() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "카카오 로그인 실패");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        
        if (oauth2User != null) {
            response.put("email", oauth2User.getAttribute("email"));
            response.put("nickname", oauth2User.getAttribute("nickname"));
            response.put("memberId", oauth2User.getAttribute("memberId"));
            response.put("memberName", oauth2User.getAttribute("memberName"));
        } else if (session.getAttribute("email") != null) {
            response.put("email", session.getAttribute("email"));
            response.put("nickname", session.getAttribute("nickname"));
            response.put("memberId", session.getAttribute("memberId"));
            response.put("memberName", session.getAttribute("memberName"));
        } else {
            response.put("message", "로그인되지 않았습니다");
        }

        return ResponseEntity.ok(response);
    }
}