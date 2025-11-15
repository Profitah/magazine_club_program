package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.OAuth2UserInfo;
import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.DTO.AdminDTO;
import com.club.magazine_club_program.Mapper.AdminMapper;
import com.club.magazine_club_program.Service.OAuth2.OAuth2UserInfoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final MemberService memberService;
    private final AdminMapper adminMapper;
    private final List<OAuth2UserInfoExtractor> extractors;

    public CustomOAuth2UserService(
            MemberService memberService, 
            AdminMapper adminMapper,
            List<OAuth2UserInfoExtractor> extractors) {
        this.memberService = memberService;
        this.adminMapper = adminMapper;
        this.extractors = extractors;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
            
            log.info("OAuth2 로그인 시작: registrationId={}", registrationId);

            if ("google".equals(registrationId)) {
                log.info("구글 원본 attributes 전체: {}", oAuth2User.getAttributes());
                String googleName = oAuth2User.getAttribute("name");
                String googleEmail = oAuth2User.getAttribute("email");
                String googleGivenName = oAuth2User.getAttribute("given_name");
                String googleFamilyName = oAuth2User.getAttribute("family_name");
                String googlePicture = oAuth2User.getAttribute("picture");
                
                log.info("구글 속성 - name: {}, email: {}, given_name: {}, family_name: {}, picture: {}", 
                        googleName, googleEmail, googleGivenName, googleFamilyName, googlePicture);
            }
            
            // 카카오의 경우 원본 attributes 확인
            if ("kakao".equals(registrationId)) {
                log.info("카카오 원본 attributes 전체: {}", oAuth2User.getAttributes());
            }

        OAuth2UserInfo userInfo = extractUserInfo(registrationId, oAuth2User.getAttributes());
            log.info("OAuth2 로그인 시도: registrationId={}, email={}, nickname={}", 
                    registrationId, userInfo.getEmail(), userInfo.getNickname());

        // 카카오 또는 구글 로그인인 경우 관리자 체크
        if ("kakao".equals(registrationId) || "google".equals(registrationId)) {
            // 관리자 계정으로 소셜 로그인 시도 시 차단
            if (userInfo.getEmail() != null && !userInfo.getEmail().isEmpty()) {
                AdminDTO admin = adminMapper.findByEmail(userInfo.getEmail());
                if (admin != null) {
                    log.warn("관리자 계정의 {} 로그인 시도 차단: email={}", registrationId, userInfo.getEmail());
                        OAuth2Error oauth2Error = new OAuth2Error("admin_login_forbidden", 
                                "관리자는 소셜 로그인을 사용할 수 없습니다. Authenticator 로그인을 사용해주세요.", null);
                        throw new OAuth2AuthenticationException(oauth2Error);
                }
            }

            // 카카오 또는 구글 로그인인 경우 회원 정보 생성 또는 조회
                String nameToUse = userInfo.getNickname();
                if ("google".equals(registrationId)) {
                    // 구글의 name 속성을 직접 가져오기
                    String googleName = oAuth2User.getAttribute("name");
                    if (googleName != null && !googleName.trim().isEmpty()) {
                        nameToUse = googleName;
                        log.info("구글 name 속성 직접 사용: {}", nameToUse);
                    } else {
                        // name이 없으면 given_name + family_name 조합 시도
                        String givenName = oAuth2User.getAttribute("given_name");
                        String familyName = oAuth2User.getAttribute("family_name");
                        if (givenName != null && !givenName.trim().isEmpty()) {
                            if (familyName != null && !familyName.trim().isEmpty()) {
                                nameToUse = givenName + " " + familyName;
                            } else {
                                nameToUse = givenName;
                            }
                            log.info("구글 given_name/family_name 조합 사용: {}", nameToUse);
                        } else {
                            log.warn("구글에서 name, given_name 모두 없음. email 사용: {}", userInfo.getEmail());
                        }
                    }
                }
                
            MemberDTO member = memberService.findOrCreateKakaoMember(
                        registrationId,  // provider ID (kakao 또는 google)
                        nameToUse,
                    userInfo.getEmail()
            );
            
            if (member == null) {
                log.error("{} 회원 생성/조회 실패: email={}", registrationId, userInfo.getEmail());
                    OAuth2Error oauth2Error = new OAuth2Error("member_processing_failed", "회원 정보 처리 실패", null);
                    throw new OAuth2AuthenticationException(oauth2Error);
            }
            
            // 회원 정보를 attributes에 추가
            Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
                // 카카오는 원본에 "id"가 있으므로 덮어쓰지 않음, 구글은 "sub"가 있으므로 "id" 추가
                if (!"kakao".equals(registrationId)) {
            attributes.put("id", userInfo.getId());
                }
            attributes.put("email", userInfo.getEmail());
            attributes.put("nickname", userInfo.getNickname());
                // 구글의 경우 원본 "name" 속성도 유지
                if ("google".equals(registrationId) && oAuth2User.getAttributes().containsKey("name")) {
                    attributes.put("name", oAuth2User.getAttribute("name"));
                }
            attributes.put("memberId", member.getId());
            attributes.put("memberName", member.getName());
            attributes.put("registrationId", registrationId); 
            
            log.info("회원 정보 연동 완료: memberId={}, email={}, name={}, provider={}", 
                    member.getId(), userInfo.getEmail(), member.getName(), registrationId);
            
                // userNameAttributeName 결정: 구글은 "sub", 카카오는 "id"
                String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                        .getUserInfoEndpoint().getUserNameAttributeName();
                log.debug("userNameAttributeName: {}, registrationId: {}", userNameAttributeName, registrationId);
                
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                        userNameAttributeName != null ? userNameAttributeName : "id"
            );
        }

        // 지원하지 않는 OAuth 제공자
            OAuth2Error oauth2Error = new OAuth2Error("unsupported_provider", 
                    "지원하지 않는 로그인 방식입니다: " + registrationId, null);
            throw new OAuth2AuthenticationException(oauth2Error);
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 에러 발생: registrationId={}, error={}", 
                    userRequest != null ? userRequest.getClientRegistration().getRegistrationId() : "unknown", 
                    e.getMessage(), e);
            if (e instanceof OAuth2AuthenticationException) {
                throw e;
            }
            OAuth2Error oauth2Error = new OAuth2Error("login_failed", "로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, e);
        }
    }

    /**
     * OAuth2 제공자별 사용자 정보 추출
     */
    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        try {
        return extractors.stream()
                .filter(extractor -> extractor.getRegistrationId().equals(registrationId))
                .findFirst()
                    .map(extractor -> {
                        try {
                            return extractor.extract(attributes);
                        } catch (Exception e) {
                            log.error("{} 사용자 정보 추출 실패: error={}, attributes={}", 
                                    registrationId, e.getMessage(), attributes, e);
                            OAuth2Error oauth2Error = new OAuth2Error("user_info_extraction_failed", 
                                    registrationId + " 사용자 정보 추출 실패: " + e.getMessage(), null);
                            throw new OAuth2AuthenticationException(oauth2Error, e);
                        }
                    })
                    .orElseThrow(() -> {
                        OAuth2Error oauth2Error = new OAuth2Error("unsupported_provider", 
                                "지원하지 않는 로그인 방식입니다: " + registrationId, null);
                        return new OAuth2AuthenticationException(oauth2Error);
                    });
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} 사용자 정보 추출 중 예상치 못한 에러: error={}", registrationId, e.getMessage(), e);
            OAuth2Error oauth2Error = new OAuth2Error("user_info_extraction_error", 
                    "사용자 정보 추출 중 오류가 발생했습니다: " + e.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, e);
        }
    }
}

