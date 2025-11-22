package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.Util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 카카오 로그인 서비스
 * 일반 사용자 로그인 시 JWT 토큰 발급
 */
@Service
public class KakaoLoginService implements OAuth2LoginService {

    private static final Logger log = LoggerFactory.getLogger(KakaoLoginService.class);
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    public KakaoLoginService(MemberService memberService, JwtUtil jwtUtil) {
        this.memberService = memberService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String getProviderId() {
        return "kakao";
    }

    @Override
    public Map<String, Object> handleLoginSuccess(OAuth2User oauth2User, HttpSession session) {
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
                    needsAdditionalInfo = true;
                }
            } else {
                needsAdditionalInfo = true;
            }

            // DB에서 조회한 실제 값 사용 (null 제거)
            Map<String, Object> userInfo = new HashMap<>();
            if (dbMember != null) {
                userInfo.put("email", dbMember.getEmail() != null ? dbMember.getEmail() : "");
                userInfo.put("name", dbMember.getName() != null ? dbMember.getName() : "");
            } else {
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
            response.put("isRegistrationComplete", !needsAdditionalInfo);
            
            // JWT 토큰 발급 (일반 사용자용)
            if (memberId != null && email != null && memberName != null) {
                try {
                    Map<String, Object> tokenInfo = jwtUtil.generateTokenPair(memberId, email, memberName);
                    response.put("token", tokenInfo);
                    log.info("카카오 로그인 JWT 토큰 발급 완료: memberId={}, email={}", memberId, email);
                } catch (Exception e) {
                    log.error("카카오 로그인 JWT 토큰 발급 실패: memberId={}, error={}", memberId, e.getMessage(), e);
                    // 토큰 발급 실패해도 로그인은 성공으로 처리
                }
            }
            
            log.info("카카오 로그인 성공: memberId={}, name={}, email={}, needsAdditionalInfo={}", 
                    memberId, memberName, email, needsAdditionalInfo);
        } else {
            response.put("success", false);
            response.put("message", "로그인 실패");
        }

        return response;
    }

    @Override
    public Map<String, Object> handleLoginFailure() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "카카오 로그인 실패");
        return response;
    }

    @Override
    public Map<String, Object> getCurrentUser(OAuth2User oauth2User, HttpSession session) {
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

        return response;
    }
}

