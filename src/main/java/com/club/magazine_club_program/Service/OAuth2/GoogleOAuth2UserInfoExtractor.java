package com.club.magazine_club_program.Service.OAuth2;

import com.club.magazine_club_program.DTO.OAuth2UserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 구글 OAuth2 사용자 정보 추출기
 */
@Component
public class GoogleOAuth2UserInfoExtractor implements OAuth2UserInfoExtractor {

    @Override
    public String getRegistrationId() {
        return "google";
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {
        String id = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        
        // 디버깅: 구글에서 받은 모든 속성 로그 출력
        System.out.println("구글 OAuth2 속성 전체: " + attributes);
        System.out.println("구글 name 속성: " + name);
        System.out.println("구글 given_name 속성: " + attributes.get("given_name"));
        System.out.println("구글 family_name 속성: " + attributes.get("family_name"));
        System.out.println("구글 email 속성: " + email);
        
        // name이 없으면 given_name + family_name 조합 시도
        if ((name == null || name.trim().isEmpty()) && attributes.containsKey("given_name")) {
            String givenName = (String) attributes.get("given_name");
            String familyName = (String) attributes.get("family_name");
            if (givenName != null && !givenName.trim().isEmpty()) {
                if (familyName != null && !familyName.trim().isEmpty()) {
                    name = givenName + " " + familyName;
                } else {
                    name = givenName;
                }
                System.out.println("구글 name 조합: " + name);
            }
        }
        
        // name이 null이거나 빈 문자열이면 email 사용
        String nickname = (name != null && !name.trim().isEmpty()) ? name : email;

        return OAuth2UserInfo.builder()
                .id(id)
                .email(email)
                .nickname(nickname)
                .build();
    }
}

