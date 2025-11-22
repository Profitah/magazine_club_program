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
 * 구글 로그인 서비스
 * 일반 사용자 로그인 시 JWT 토큰 발급
 */
@Service
public class GoogleLoginService implements OAuth2LoginService {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginService.class);
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    public GoogleLoginService(MemberService memberService, JwtUtil jwtUtil) {
        this.memberService = memberService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String getProviderId() {
        return "google";
    }

    @Override
    public Map<String, Object> handleLoginSuccess(OAuth2User oauth2User, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (oauth2User != null) {
            String email = oauth2User.getAttribute("email");
            // 구글의 경우 "name" 속성을 우선 사용
            String name = oauth2User.getAttribute("name");
            String nickname = oauth2User.getAttribute("nickname");
            
            // name이 없으면 given_name + family_name 조합 시도
            if ((name == null || name.isEmpty()) && oauth2User.getAttributes().containsKey("given_name")) {
                Object givenName = oauth2User.getAttribute("given_name");
                Object familyName = oauth2User.getAttribute("family_name");
                if (givenName != null && !givenName.toString().trim().isEmpty()) {
                    if (familyName != null && !familyName.toString().trim().isEmpty()) {
                        name = givenName.toString() + " " + familyName.toString();
                    } else {
                        name = givenName.toString();
                    }
                    log.info("구글 given_name/family_name 조합: {}", name);
                }
            }
            
            // name이 여전히 없으면 nickname 사용
            if (name == null || name.isEmpty()) {
                name = nickname;
            }
            
            Integer memberId = oauth2User.getAttribute("memberId");
            String memberName = oauth2User.getAttribute("memberName");
            
            log.info("구글 로그인 - 최종 name: {}, nickname: {}, email: {}", name, nickname, email);
            
            log.info("구글 로그인 성공 핸들러 호출: email={}, name={}, nickname={}, memberId={}, memberName={}", 
                    email, name, nickname, memberId, memberName);

            // memberId가 없으면 이메일로 회원 조회/생성 시도
            MemberDTO dbMember = null;
            Integer currentMemberId = memberId; // effectively final 변수로 복사
            if (currentMemberId != null) {
                dbMember = memberService.getAllMembers().stream()
                        .filter(m -> m.getId() == currentMemberId)
                        .findFirst()
                        .orElse(null);
            }
            
            // memberId가 없거나 DB에서 찾지 못한 경우, 이메일로 조회/생성
            if (dbMember == null && email != null && !email.isEmpty()) {
                log.info("이메일로 회원 조회/생성 시도: email={}", email);
                try {
                    // 이메일로 회원 조회
                    dbMember = memberService.findByEmail(email);
                    // 회원이 없으면 생성 (구글 로그인 정보로)
                    if (dbMember == null) {
                        // 구글의 경우 name을 우선 사용
                        String nameToUse = (name != null && !name.isEmpty()) ? name : nickname;
                        log.info("신규 회원 생성 시도: email={}, name={}", email, nameToUse);
                        dbMember = memberService.findOrCreateKakaoMember("google", nameToUse, email);
                        log.info("신규 회원 생성 완료: memberId={}, email={}, name={}", 
                                dbMember != null ? dbMember.getId() : null, email, 
                                dbMember != null ? dbMember.getName() : null);
                    }
                    
                    // memberId와 memberName 업데이트
                    if (dbMember != null) {
                        memberId = dbMember.getId();
                        memberName = dbMember.getName();
                    }
                } catch (Exception e) {
                    log.error("회원 조회/생성 중 에러 발생: email={}, error={}", email, e.getMessage(), e);
                }
            }

            // 세션에 사용자 정보 저장
            session.setAttribute("email", email);
            session.setAttribute("nickname", nickname != null ? nickname : (memberName != null ? memberName : ""));
            session.setAttribute("memberId", memberId);
            session.setAttribute("memberName", memberName);

            // 추가 정보가 필요한지 확인 (DB에 저장된 정보 기준)
            boolean needsAdditionalInfo = false;
            
            if (dbMember != null) {
                // DB에 저장된 email이 비어있는지 확인
                needsAdditionalInfo = (dbMember.getEmail() == null || dbMember.getEmail().isEmpty());
            } else {
                needsAdditionalInfo = true;
            }

            // DB에서 조회한 실제 값 사용 (null 제거)
            Map<String, Object> userInfo = new HashMap<>();
            if (dbMember != null) {
                userInfo.put("email", dbMember.getEmail() != null ? dbMember.getEmail() : "");
                userInfo.put("name", dbMember.getName() != null ? dbMember.getName() : "");
                userInfo.put("nickname", dbMember.getName() != null ? dbMember.getName() : "");
            } else {
                userInfo.put("email", email != null ? email : "");
                // 구글의 경우 name을 우선 사용
                String displayName = (name != null && !name.isEmpty()) ? name : 
                                    (nickname != null && !nickname.isEmpty() ? nickname : 
                                    (memberName != null ? memberName : ""));
                userInfo.put("name", displayName);
                userInfo.put("nickname", displayName);
            }
            
            if (memberId != null) {
                userInfo.put("memberId", memberId);
            }
            if (memberName != null) {
                userInfo.put("memberName", memberName);
            }

            response.put("success", true);
            response.put("message", needsAdditionalInfo ? "추가 정보를 입력해주세요" : "구글 로그인 성공");
            response.put("user", userInfo);
            response.put("needsAdditionalInfo", needsAdditionalInfo);
            response.put("isRegistrationComplete", !needsAdditionalInfo);
            
            // JWT 토큰 발급 (일반 사용자용)
            if (memberId != null && email != null && memberName != null) {
                try {
                    Map<String, Object> tokenInfo = jwtUtil.generateTokenPair(memberId, email, memberName);
                    response.put("token", tokenInfo);
                    log.info("구글 로그인 JWT 토큰 발급 완료: memberId={}, email={}", memberId, email);
                } catch (Exception e) {
                    log.error("구글 로그인 JWT 토큰 발급 실패: memberId={}, error={}", memberId, e.getMessage(), e);
                    // 토큰 발급 실패해도 로그인은 성공으로 처리
                }
            }
            
            log.info("구글 로그인 성공: memberId={}, name={}, email={}, needsAdditionalInfo={}", 
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
        response.put("message", "구글 로그인 실패");
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

