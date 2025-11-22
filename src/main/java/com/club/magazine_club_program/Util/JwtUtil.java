package com.club.magazine_club_program.Util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰 유틸리티 클래스
 * 일반 사용자(카카오/구글 로그인)의 JWT 토큰 생성 및 검증
 * 관리자는 세션 방식 사용 (별도 처리)
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    
    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtUtil(
            @Value("${jwt.secret:defaultSecretKeyForJWTTokenGenerationAtLeast256BitsMagazineClubProgram2024}") String secret,
            @Value("${jwt.access-token-validity-in-seconds:3600}") long accessTokenValidityInSeconds,
            @Value("${jwt.refresh-token-validity-in-seconds:86400}") long refreshTokenValidityInSeconds) {
        
        // SecretKey 생성 (HMAC-SHA256 알고리즘 사용)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // 최소 256비트(32바이트) 키 필요
        if (keyBytes.length < 32) {
            // 키가 짧으면 반복하여 32바이트 이상으로 확장
            byte[] extendedKey = new byte[32];
            System.arraycopy(keyBytes, 0, extendedKey, 0, Math.min(keyBytes.length, 32));
            for (int i = keyBytes.length; i < 32; i++) {
                extendedKey[i] = keyBytes[i % keyBytes.length];
            }
            keyBytes = extendedKey;
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
        
        log.info("JwtUtil 초기화 완료: accessTokenValidity={}초, refreshTokenValidity={}초", 
                accessTokenValidityInSeconds, refreshTokenValidityInSeconds);
    }

    /**
     * Access Token 생성
     * @param memberId 회원 ID
     * @param email 이메일
     * @param name 이름
     * @return JWT Access Token
     */
    public String generateAccessToken(Integer memberId, String email, String name) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("email", email);
        claims.put("name", name);
        claims.put("type", "access");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     * @param memberId 회원 ID
     * @param email 이메일
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(Integer memberId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("email", email);
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Access Token과 Refresh Token을 함께 생성
     * @param memberId 회원 ID
     * @param email 이메일
     * @param name 이름
     * @return 토큰 정보가 담긴 Map (accessToken, refreshToken, expiresIn)
     */
    public Map<String, Object> generateTokenPair(Integer memberId, String email, String name) {
        String accessToken = generateAccessToken(memberId, email, name);
        String refreshToken = generateRefreshToken(memberId, email);

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("accessToken", accessToken);
        tokenInfo.put("refreshToken", refreshToken);
        tokenInfo.put("tokenType", "Bearer");
        tokenInfo.put("expiresIn", accessTokenValidityInMilliseconds / 1000); // 초 단위

        return tokenInfo;
    }

    /**
     * 토큰에서 이메일 추출
     * @param token JWT 토큰
     * @return 이메일
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 토큰에서 회원 ID 추출
     * @param token JWT 토큰
     * @return 회원 ID
     */
    public Integer getMemberIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object memberId = claims.get("memberId");
        if (memberId instanceof Integer) {
            return (Integer) memberId;
        } else if (memberId instanceof Number) {
            return ((Number) memberId).intValue();
        }
        return null;
    }

    /**
     * 토큰에서 이름 추출
     * @param token JWT 토큰
     * @return 이름
     */
    public String getNameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("name", String.class);
    }

    /**
     * 토큰에서 Claims 추출
     * @param token JWT 토큰
     * @return Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.error("토큰 파싱 실패: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * 토큰 유효성 검증
     * @param token JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("토큰 만료: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 만료 여부 확인
     * @param token JWT 토큰
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Refresh Token으로 새로운 Access Token 생성
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token 정보가 담긴 Map
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Claims claims = getClaimsFromToken(refreshToken);
        String type = claims.get("type", String.class);
        
        if (!"refresh".equals(type)) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        Integer memberId = getMemberIdFromToken(refreshToken);
        String email = getEmailFromToken(refreshToken);
        String name = claims.get("name", String.class);

        // 새로운 Access Token 생성
        String newAccessToken = generateAccessToken(memberId, email, name);

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("accessToken", newAccessToken);
        tokenInfo.put("tokenType", "Bearer");
        tokenInfo.put("expiresIn", accessTokenValidityInMilliseconds / 1000);

        return tokenInfo;
    }
}

