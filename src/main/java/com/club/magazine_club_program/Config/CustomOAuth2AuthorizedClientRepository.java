package com.club.magazine_club_program.Config;

import com.club.magazine_club_program.DTO.OAuth2TokenDTO;
import com.club.magazine_club_program.Mapper.OAuth2TokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;

@Component
public class CustomOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2AuthorizedClientRepository.class);
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final OAuth2TokenMapper oAuth2TokenMapper;

    public CustomOAuth2AuthorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService,
            OAuth2TokenMapper oAuth2TokenMapper) {
        this.authorizedClientService = authorizedClientService;
        this.oAuth2TokenMapper = oAuth2TokenMapper;
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            HttpServletRequest request) {
        
        String principalName = principal.getName();
        log.debug("토큰 조회: principalName={}, registrationId={}", principalName, clientRegistrationId);
        
        // 데이터베이스에서 먼저 조회 (세션 스토리지 대신 데이터베이스 사용)
        OAuth2TokenDTO tokenDTO = oAuth2TokenMapper.findByPrincipalNameAndRegistrationId(
                principalName, clientRegistrationId);
        
        if (tokenDTO != null) {
            // 토큰이 만료되었는지 확인
            if (tokenDTO.getAccessTokenExpiresAt() != null && 
                tokenDTO.getAccessTokenExpiresAt().isAfter(Instant.now())) {
                log.debug("데이터베이스에서 토큰 조회 성공: principalName={}", principalName);
                
                // OAuth2AuthorizedClient 재구성 (데이터베이스에서 로드)
                // 메모리 기반 서비스도 업데이트 (호환성을 위해)
                // 하지만 실제 저장소는 데이터베이스만 사용
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                        clientRegistrationId, principalName);
                
                @SuppressWarnings("unchecked")
                T result = (T) authorizedClient;
                return result;
            } else {
                log.debug("토큰 만료됨: principalName={}", principalName);
                // 만료된 토큰 삭제
                oAuth2TokenMapper.remove(principalName, clientRegistrationId);
            }
        }
        
        // 데이터베이스에 없으면 메모리에서도 확인 (fallback)
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId, principalName);
        
        @SuppressWarnings("unchecked")
        T result = (T) authorizedClient;
        return result;
    }

    @Override
    public void saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient,
            Authentication principal,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String principalName = principal.getName();
        String registrationId = authorizedClient.getClientRegistration().getRegistrationId();
        
        log.info("토큰 저장 (데이터베이스): principalName={}, registrationId={}", principalName, registrationId);
        
        // 데이터베이스에 저장 (주 저장소)
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
        
        OAuth2TokenDTO tokenDTO = new OAuth2TokenDTO();
        tokenDTO.setPrincipalName(principalName);
        tokenDTO.setRegistrationId(registrationId);
        
        if (accessToken != null) {
            tokenDTO.setAccessTokenValue(accessToken.getTokenValue());
            tokenDTO.setAccessTokenType(accessToken.getTokenType().getValue());
            tokenDTO.setAccessTokenIssuedAt(accessToken.getIssuedAt());
            tokenDTO.setAccessTokenExpiresAt(accessToken.getExpiresAt());
        }
        
        if (refreshToken != null) {
            tokenDTO.setRefreshTokenValue(refreshToken.getTokenValue());
            tokenDTO.setRefreshTokenIssuedAt(refreshToken.getIssuedAt());
            tokenDTO.setRefreshTokenExpiresAt(refreshToken.getExpiresAt());
        }
        
        oAuth2TokenMapper.save(tokenDTO);
        log.info("토큰 데이터베이스 저장 완료: principalName={}, tokenValue={}...", 
                principalName, 
                accessToken != null ? accessToken.getTokenValue().substring(0, Math.min(10, accessToken.getTokenValue().length())) : "null");
        
        // 호환성을 위해 메모리 기반 서비스에도 저장 (하지만 주 저장소는 데이터베이스)
        authorizedClientService.saveAuthorizedClient(authorizedClient, principal);
    }

    @Override
    public void removeAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String principalName = principal.getName();
        log.info("토큰 삭제 (데이터베이스): principalName={}, registrationId={}", principalName, clientRegistrationId);
        
        // 데이터베이스에서 삭제 (주 저장소)
        oAuth2TokenMapper.remove(principalName, clientRegistrationId);
        log.info("토큰 데이터베이스 삭제 완료: principalName={}", principalName);
        
        // 호환성을 위해 메모리 기반 서비스에서도 삭제
        authorizedClientService.removeAuthorizedClient(clientRegistrationId, principalName);
    }
}

