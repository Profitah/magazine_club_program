package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.OAuth2UserInfo;
import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.DTO.AdminDTO;
import com.club.magazine_club_program.Mapper.AdminMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final MemberService memberService;
    private final AdminMapper adminMapper;

    public CustomOAuth2UserService(MemberService memberService, AdminMapper adminMapper) {
        this.memberService = memberService;
        this.adminMapper = adminMapper;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = extractUserInfo(registrationId, oAuth2User.getAttributes());
        log.info("OAuth2 로그인 시도: registrationId={}, email={}", registrationId, userInfo.getEmail());

        // 카카오 로그인인 경우 관리자 체크
        if ("kakao".equals(registrationId)) {
            // 관리자 계정으로 카카오 로그인 시도 시 차단
            if (userInfo.getEmail() != null && !userInfo.getEmail().isEmpty()) {
                AdminDTO admin = adminMapper.findByEmail(userInfo.getEmail());
                if (admin != null) {
                    log.warn("관리자 계정의 카카오 로그인 시도 차단: email={}", userInfo.getEmail());
                    throw new OAuth2AuthenticationException("관리자는 카카오 로그인을 사용할 수 없습니다. Authenticator 로그인을 사용해주세요.");
                }
            }

        // 카카오 로그인인 경우 회원 정보 생성 또는 조회
            MemberDTO member = memberService.findOrCreateKakaoMember(
                    userInfo.getId(),
                    userInfo.getNickname(),
                    userInfo.getEmail()
            );
            
            if (member == null) {
                log.error("카카오 회원 생성/조회 실패: email={}", userInfo.getEmail());
                throw new OAuth2AuthenticationException("회원 정보 처리 실패");
            }
            
            // 회원 정보를 attributes에 추가
            Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
            attributes.put("id", userInfo.getId());
            attributes.put("email", userInfo.getEmail());
            attributes.put("nickname", userInfo.getNickname());
            attributes.put("memberId", member.getId());
            attributes.put("memberName", member.getName());
            
            log.info("회원 정보 연동 완료: memberId={}, email={}, name={}", 
                    member.getId(), userInfo.getEmail(), member.getName());
            
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "id"
            );
        }

        // 카카오가 아닌 경우 기존 로직 유지
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("id", userInfo.getId());
        attributes.put("email", userInfo.getEmail());
        attributes.put("nickname", userInfo.getNickname());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
    }

    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return extractKakaoUserInfo(attributes);
        }
        throw new OAuth2AuthenticationException("지원하지 않는 로그인 방식입니다: " + registrationId);
    }

    private OAuth2UserInfo extractKakaoUserInfo(Map<String, Object> attributes) {
        Long id = ((Number) attributes.get("id")).longValue();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Collections.emptyMap());
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Collections.emptyMap());

        String email = (String) kakaoAccount.get("email");
        String nickname = (String) profile.get("nickname");

        return OAuth2UserInfo.builder()
                .id(String.valueOf(id))
                .email(email)
                .nickname(nickname)
                .build();
    }
}

