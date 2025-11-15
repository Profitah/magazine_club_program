package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.MemberDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * OAuth2 로그인 서비스 인터페이스
 */
public interface OAuth2LoginService {
    
    /**
     * 로그인 성공 처리
     * @param oauth2User OAuth2 사용자 정보
     * @param session HTTP 세션
     * @return 로그인 결과 응답
     */
    Map<String, Object> handleLoginSuccess(OAuth2User oauth2User, HttpSession session);
    
    /**
     * 로그인 실패 처리
     * @return 실패 응답
     */
    Map<String, Object> handleLoginFailure();
    
    /**
     * 현재 로그인한 사용자 정보 조회
     * @param oauth2User OAuth2 사용자 정보
     * @param session HTTP 세션
     * @return 사용자 정보
     */
    Map<String, Object> getCurrentUser(OAuth2User oauth2User, HttpSession session);
    
    /**
     * OAuth2 제공자 식별자
     * @return "kakao", "google" 등
     */
    String getProviderId();
}

