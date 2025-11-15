package com.club.magazine_club_program.Config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 로그인 성공 핸들러
 * 제공자별로 적절한 성공 URL로 리다이렉트
 */
@Component
public class CustomOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2AuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            Map<String, Object> attributes = oauth2User.getAttributes();
            
            // attributes에서 registrationId 가져오기
            String registrationId = (String) attributes.get("registrationId");
            
            // registrationId가 없으면 URL에서 추출 시도 (구글 로그인 특별 처리)
            if (registrationId == null) {
                String requestURI = request.getRequestURI();
                String referer = request.getHeader("Referer");
                log.debug("registrationId를 찾지 못함. requestURI={}, referer={}, attributes keys={}", 
                        requestURI, referer, attributes.keySet());
                
                // URL에서 registrationId 추출 시도
                if (requestURI != null) {
                    if (requestURI.contains("/code/google") || requestURI.contains("google")) {
                        registrationId = "google";
                    } else if (requestURI.contains("/code/kakao") || requestURI.contains("kakao")) {
                        registrationId = "kakao";
                    }
                }
                
                // Referer 헤더에서도 확인
                if (registrationId == null && referer != null) {
                    if (referer.contains("google")) {
                        registrationId = "google";
                    } else if (referer.contains("kakao")) {
                        registrationId = "kakao";
                    }
                }
                
                // attributes에서 직접 확인 (구글은 "sub" 키가 있고, 카카오는 "id" 키가 있음)
                if (registrationId == null) {
                    // 카카오는 "id" 키가 있고 "sub" 키가 없음
                    if (attributes.containsKey("id") && !attributes.containsKey("sub")) {
                        registrationId = "kakao";
                    } 
                    // 구글은 "sub" 키가 있음
                    else if (attributes.containsKey("sub")) {
                        registrationId = "google";
                    }
                }
            }
            
            // 세션에 회원 정보 저장 (attributes에서 가져오기)
            if (attributes.containsKey("memberId")) {
                request.getSession().setAttribute("memberId", attributes.get("memberId"));
            }
            if (attributes.containsKey("memberName")) {
                request.getSession().setAttribute("memberName", attributes.get("memberName"));
            }
            if (attributes.containsKey("email")) {
                request.getSession().setAttribute("email", attributes.get("email"));
            }
            
            if (registrationId != null) {
                // 제공자별 성공 URL 설정
                String targetUrl = determineTargetUrl(registrationId);
                log.info("OAuth2 로그인 성공: provider={}, redirecting to {}", registrationId, targetUrl);
                
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            } else {
                log.warn("OAuth2 로그인 성공했으나 registrationId를 찾을 수 없음. attributes: {}, requestURI: {}", 
                        attributes.keySet(), request.getRequestURI());
                // 마지막 시도: attributes 기반으로 추정
                if (attributes.containsKey("sub")) {
                    registrationId = "google";
                } else if (attributes.containsKey("id")) {
                    registrationId = "kakao";
                }
                
                if (registrationId != null) {
                    String targetUrl = determineTargetUrl(registrationId);
                    log.info("{} 로그인으로 추정하여 리다이렉트: {}", registrationId, targetUrl);
                    getRedirectStrategy().sendRedirect(request, response, targetUrl);
                    return;
                }
            }
        }
        
        // 기본 성공 URL로 리다이렉트
        super.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * 제공자별 타겟 URL 결정
     */
    private String determineTargetUrl(String registrationId) {
        return switch (registrationId) {
            case "kakao" -> "/login/kakao/success";
            case "google" -> "/login/google/success";
            default -> "/login/oauth2/success";
        };
    }
}

