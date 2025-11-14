package com.club.magazine_club_program.Config;

import com.club.magazine_club_program.DTO.OAuth2TokenDTO;
import com.club.magazine_club_program.Mapper.OAuth2TokenMapper;
import com.club.magazine_club_program.Util.TokenEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
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
    private final TokenEncryptionUtil tokenEncryptionUtil;

    public CustomOAuth2AuthorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService,
            OAuth2TokenMapper oAuth2TokenMapper,
            TokenEncryptionUtil tokenEncryptionUtil) {
        this.authorizedClientService = authorizedClientService;
        this.oAuth2TokenMapper = oAuth2TokenMapper;
        this.tokenEncryptionUtil = tokenEncryptionUtil;
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            HttpServletRequest request) {
        
        String principalName = principal.getName();
        log.debug("토큰 조회: principalName={}, registrationId={}", principalName, clientRegistrationId);
        
        // 1. 먼저 메모리에서 Access token 확인 (짧은 만료 시간)
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId, principalName);
        
        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            // Access token이 유효하면 그대로 반환
            if (accessToken.getExpiresAt() != null && accessToken.getExpiresAt().isAfter(Instant.now())) {
                log.debug("메모리에서 Access token 조회 성공: principalName={}", principalName);
                @SuppressWarnings("unchecked")
                T result = (T) authorizedClient;
                return result;
            }
        }
        
        // 2. Access token이 만료되었거나 없으면, DB에서 Refresh token 확인
        OAuth2TokenDTO tokenDTO = oAuth2TokenMapper.findByPrincipalNameAndRegistrationId(
                principalName, clientRegistrationId);
        
        if (tokenDTO != null && tokenDTO.getRefreshTokenValue() != null) {
            // Refresh token 만료 확인
            if (tokenDTO.getRefreshTokenExpiresAt() != null && 
                tokenDTO.getRefreshTokenExpiresAt().isAfter(Instant.now())) {
                log.debug("데이터베이스에서 Refresh token 조회 성공: principalName={}", principalName);
                
                try {
                    // Refresh token 복호화
                    String decryptedRefreshToken = tokenEncryptionUtil.decrypt(tokenDTO.getRefreshTokenValue());
                    
                    // Refresh token으로 새로운 Access token 발급받기
                    // 이 부분은 Spring Security OAuth2 Client가 자동으로 처리하므로,
                    // 메모리에 있는 authorizedClient를 그대로 사용하고 refresh token을 복호화해서 전달
                    // 실제 refresh 로직은 Spring Security가 처리합니다.
                    
                    // 현재는 메모리에서 로드한 클라이언트를 반환
                    // Refresh token은 save 시 자동으로 업데이트됩니다.
                    @SuppressWarnings("unchecked")
                    T result = (T) authorizedClient;
                    return result;
                } catch (Exception e) {
                    log.error("Refresh token 복호화 실패: principalName={}", principalName, e);
                    // 복호화 실패 시 토큰 삭제
                    oAuth2TokenMapper.remove(principalName, clientRegistrationId);
                }
            } else {
                log.debug("Refresh token 만료됨: principalName={}", principalName);
                // 만료된 토큰 삭제
                oAuth2TokenMapper.remove(principalName, clientRegistrationId);
            }
        }
        
        // 3. 메모리에서도 없으면 null 반환 (재로그인 필요)
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
        
        log.info("토큰 저장: principalName={}, registrationId={}", principalName, registrationId);
        
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
        
        // 1. Access token은 메모리에만 저장 (짧은 만료 시간, 세션 종료 시 자동 삭제)
        authorizedClientService.saveAuthorizedClient(authorizedClient, principal);
        log.info("Access token 메모리 저장 완료: principalName={}, expiresAt={}", 
                principalName, 
                accessToken != null ? accessToken.getExpiresAt() : "null");
        
        // 2. Refresh token만 DB에 암호화하여 저장 (장기 저장)
        if (refreshToken != null) {
            try {
                // Refresh token 암호화
                String encryptedRefreshToken = tokenEncryptionUtil.encrypt(refreshToken.getTokenValue());
                
                OAuth2TokenDTO tokenDTO = new OAuth2TokenDTO();
                tokenDTO.setPrincipalName(principalName);
                tokenDTO.setRegistrationId(registrationId);
                tokenDTO.setRefreshTokenValue(encryptedRefreshToken);
                tokenDTO.setRefreshTokenIssuedAt(refreshToken.getIssuedAt());
                tokenDTO.setRefreshTokenExpiresAt(refreshToken.getExpiresAt());
                
                oAuth2TokenMapper.save(tokenDTO);
                log.info("Refresh token 데이터베이스 저장 완료 (암호화됨): principalName={}, expiresAt={}", 
                        principalName, refreshToken.getExpiresAt());
            } catch (Exception e) {
                log.error("Refresh token 암호화/저장 실패: principalName={}", principalName, e);
                throw new RuntimeException("Refresh token 저장 실패", e);
            }
        } else {
            log.warn("Refresh token이 없습니다: principalName={}", principalName);
        }
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