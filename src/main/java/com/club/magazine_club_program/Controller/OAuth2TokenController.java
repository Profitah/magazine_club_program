package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.OAuth2TokenDTO;
import com.club.magazine_club_program.Mapper.OAuth2TokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/oauth2/token")
public class OAuth2TokenController {

    private static final Logger log = LoggerFactory.getLogger(OAuth2TokenController.class);
    private final OAuth2TokenMapper oAuth2TokenMapper;

    public OAuth2TokenController(OAuth2TokenMapper oAuth2TokenMapper) {
        this.oAuth2TokenMapper = oAuth2TokenMapper;
    }

    /**
     * 현재 로그인한 사용자의 카카오 토큰 조회 (테스트용)
     * GET /oauth2/token/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyToken(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RegisteredOAuth2AuthorizedClient("kakao") OAuth2AuthorizedClient authorizedClient) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (oauth2User == null) {
                response.put("success", false);
                response.put("message", "로그인되지 않았습니다.");
                return ResponseEntity.status(401).body(response);
            }

            String principalName = oauth2User.getName();
            
            // 데이터베이스에서 토큰 조회
            OAuth2TokenDTO tokenDTO = oAuth2TokenMapper.findByPrincipalNameAndRegistrationId(
                    principalName,
                    "kakao"
            );

            if (tokenDTO != null) {
                Map<String, Object> tokenInfo = new HashMap<>();
                tokenInfo.put("principalName", tokenDTO.getPrincipalName());
                tokenInfo.put("registrationId", tokenDTO.getRegistrationId());
                tokenInfo.put("hasRefreshToken", tokenDTO.getRefreshTokenValue() != null);
                tokenInfo.put("refreshTokenIssuedAt", tokenDTO.getRefreshTokenIssuedAt());
                tokenInfo.put("refreshTokenExpiresAt", tokenDTO.getRefreshTokenExpiresAt());
                
                // Refresh token 값은 보안상 마스킹 처리 (암호화되어 저장됨)
                if (tokenDTO.getRefreshTokenValue() != null) {
                    String maskedToken = maskToken(tokenDTO.getRefreshTokenValue());
                    tokenInfo.put("refreshTokenValue", maskedToken + " (암호화됨)");
                }
                
                tokenInfo.put("note", "Access token은 메모리에만 저장됩니다 (짧은 만료 시간)");
                
                response.put("success", true);
                response.put("message", "Refresh token 조회 성공");
                response.put("token", tokenInfo);
                response.put("user", Map.of(
                    "memberId", oauth2User.getAttribute("memberId"),
                    "memberName", oauth2User.getAttribute("memberName")
                ));
            } else {
                response.put("success", false);
                response.put("message", "Refresh token을 찾을 수 없습니다.");
            }

            // OAuth2AuthorizedClient에서 Access token 확인 (메모리에 저장됨)
            if (authorizedClient != null) {
                Map<String, Object> clientInfo = new HashMap<>();
                clientInfo.put("clientRegistrationId", authorizedClient.getClientRegistration().getRegistrationId());
                if (authorizedClient.getAccessToken() != null) {
                    clientInfo.put("accessTokenIssuedAt", authorizedClient.getAccessToken().getIssuedAt());
                    clientInfo.put("accessTokenExpiresAt", authorizedClient.getAccessToken().getExpiresAt());
                    clientInfo.put("accessTokenType", authorizedClient.getAccessToken().getTokenType().getValue());
                    clientInfo.put("hasAccessToken", true);
                } else {
                    clientInfo.put("hasAccessToken", false);
                }
                clientInfo.put("hasRefreshToken", authorizedClient.getRefreshToken() != null);
                response.put("oauth2AuthorizedClient", clientInfo);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("토큰 조회 실패", e);
            response.put("success", false);
            response.put("message", "토큰 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 데이터베이스 토큰 테이블 상태 확인 (테스트용)
     * GET /oauth2/token/test
     */
    @GetMapping("/test")
    public ResponseEntity<?> testTokenStorage() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 테스트용 조회 (principalName으로 조회)
            OAuth2TokenDTO tokenDTO = oAuth2TokenMapper.findByPrincipalNameAndRegistrationId("test", "kakao");
            
            response.put("success", true);
            response.put("message", "데이터베이스 연결 성공");
            response.put("tokenTableExists", true);
            response.put("testQueryResult", tokenDTO != null ? "토큰 존재" : "토큰 없음");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("데이터베이스 테스트 실패", e);
            response.put("success", false);
            response.put("message", "데이터베이스 연결 실패: " + e.getMessage());
            response.put("tokenTableExists", false);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 만료된 토큰 삭제 (테스트/관리용)
     * DELETE /oauth2/token/expired
     */
    @DeleteMapping("/expired")
    public ResponseEntity<?> removeExpiredTokens() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int deletedCount = oAuth2TokenMapper.removeExpiredTokens();
            response.put("success", true);
            response.put("message", "만료된 토큰 삭제 완료");
            response.put("deletedCount", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("만료된 토큰 삭제 실패", e);
            response.put("success", false);
            response.put("message", "만료된 토큰 삭제 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 토큰 값 마스킹 처리
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}

