package com.club.magazine_club_program.Service.OAuth2;

import com.club.magazine_club_program.DTO.OAuth2UserInfo;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 카카오 OAuth2 사용자 정보 추출기
 */
@Component
public class KakaoOAuth2UserInfoExtractor implements OAuth2UserInfoExtractor {

    @Override
    public String getRegistrationId() {
        return "kakao";
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {
        // 카카오 attributes 디버깅
        System.out.println("카카오 OAuth2 속성 전체: " + attributes);
        
        Object idObj = attributes.get("id");
        if (idObj == null) {
            throw new IllegalArgumentException("카카오 attributes에 'id'가 없습니다: " + attributes);
        }
        
        Long id;
        if (idObj instanceof Number) {
            id = ((Number) idObj).longValue();
        } else if (idObj instanceof String) {
            id = Long.parseLong((String) idObj);
        } else {
            throw new IllegalArgumentException("카카오 'id' 타입이 예상과 다릅니다: " + idObj.getClass() + ", value: " + idObj);
        }
        
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Collections.emptyMap());
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Collections.emptyMap());

        String email = (String) kakaoAccount.get("email");
        String nickname = (String) profile.get("nickname");
        
        System.out.println("카카오 추출 정보 - id: " + id + ", email: " + email + ", nickname: " + nickname);

        return OAuth2UserInfo.builder()
                .id(String.valueOf(id))
                .email(email)
                .nickname(nickname)
                .build();
    }
}

