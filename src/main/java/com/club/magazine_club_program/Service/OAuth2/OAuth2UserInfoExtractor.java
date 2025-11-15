package com.club.magazine_club_program.Service.OAuth2;

import com.club.magazine_club_program.DTO.OAuth2UserInfo;

import java.util.Map;

/**
 * OAuth2 제공자별 사용자 정보 추출 인터페이스
 */
public interface OAuth2UserInfoExtractor {
    
    /**
     * OAuth2 제공자 식별자
     * @return "kakao", "google" 등
     */
    String getRegistrationId();
    
    /**
     * OAuth2 제공자에서 받은 attributes에서 사용자 정보 추출
     * @param attributes OAuth2 제공자로부터 받은 사용자 정보 attributes
     * @return 추출된 사용자 정보
     */
    OAuth2UserInfo extract(Map<String, Object> attributes);
}

