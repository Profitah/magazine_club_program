package com.club.magazine_club_program.Config;

import com.club.magazine_club_program.Util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 인증 필터
 * Access Token 만료 시 자동으로 Refresh Token으로 새 Access Token 발급
 * 새로고침 없이 자동 토큰 갱신 지원
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    // 토큰 갱신을 제외할 경로들
    private static final List<String> EXCLUDE_PATHS = List.of(
            "/auth/refresh",
            "/auth/logout",
            "/login/",
            "/oauth2/",
            "/admin/",
            "/logout"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // 제외 경로는 필터 스킵
        if (shouldSkip(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = extractTokenFromRequest(request);
        
        if (accessToken != null) {
            try {
                // Access Token 검증
                if (jwtUtil.validateToken(accessToken)) {
                    // Access Token이 유효하면 인증 정보 설정
                    Claims claims = jwtUtil.getClaimsFromToken(accessToken);
                    setAuthentication(claims);
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    // Access Token이 만료되었거나 유효하지 않음
                    log.debug("Access Token 만료 또는 유효하지 않음: {}", requestPath);
                }
            } catch (Exception e) {
                log.debug("Access Token 파싱 실패: {}", e.getMessage());
            }
        }

        // Access Token이 없거나 만료된 경우, Refresh Token 확인
        String refreshToken = extractRefreshTokenFromRequest(request);
        
        if (refreshToken != null) {
            try {
                if (jwtUtil.validateToken(refreshToken)) {
                    // Refresh Token으로 새로운 Access Token 발급
                    Claims refreshClaims = jwtUtil.getClaimsFromToken(refreshToken);
                    String type = refreshClaims.get("type", String.class);
                    
                    if ("refresh".equals(type)) {
                        // 새로운 Access Token 발급
                        Integer memberId = jwtUtil.getMemberIdFromToken(refreshToken);
                        String email = jwtUtil.getEmailFromToken(refreshToken);
                        String name = refreshClaims.get("name", String.class);
                        
                        String newAccessToken = jwtUtil.generateAccessToken(memberId, email, name);
                        
                        // 응답 헤더에 새로운 Access Token 추가
                        response.setHeader("X-New-Access-Token", newAccessToken);
                        response.setHeader("X-Token-Refreshed", "true");
                        
                        // 인증 정보 설정
                        Claims newClaims = jwtUtil.getClaimsFromToken(newAccessToken);
                        setAuthentication(newClaims);
                        
                        log.debug("Access Token 자동 갱신 완료: memberId={}, email={}", memberId, email);
                    }
                }
            } catch (Exception e) {
                log.debug("Refresh Token 처리 실패: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 요청에서 Access Token 추출 (Authorization 헤더)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 요청에서 Refresh Token 추출 (X-Refresh-Token 헤더 또는 쿠키)
     */
    private String extractRefreshTokenFromRequest(HttpServletRequest request) {
        // 헤더에서 확인
        String refreshToken = request.getHeader("X-Refresh-Token");
        if (StringUtils.hasText(refreshToken)) {
            return refreshToken;
        }
        
        // 쿠키에서 확인
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }

    /**
     * 인증 정보 설정
     */
    private void setAuthentication(Claims claims) {
        Integer memberId = claims.get("memberId", Integer.class);
        String email = claims.getSubject();
        
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
        
        // memberId를 details에 추가
        authentication.setDetails(Map.of("memberId", memberId, "email", email));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 필터를 건너뛸 경로 확인
     */
    private boolean shouldSkip(String path) {
        if (path == null) {
            return false;
        }
        
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                return true;
            }
        }
        
        return false;
    }
}

