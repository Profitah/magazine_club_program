package com.club.magazine_club_program.DTO;

import java.time.Instant;

public class OAuth2TokenDTO {
    private Long id;
    private String principalName; // 사용자 식별자 (memberId)
    private String registrationId; // "kakao"
    // Access token은 메모리에만 저장 (짧은 만료 시간)
    // Refresh token만 DB에 암호화 저장
    private String refreshTokenValue; // 암호화된 refresh token
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;

    public OAuth2TokenDTO() {
    }

    public OAuth2TokenDTO(String principalName, String registrationId) {
        this.principalName = principalName;
        this.registrationId = registrationId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }


    public String getRefreshTokenValue() {
        return refreshTokenValue;
    }

    public void setRefreshTokenValue(String refreshTokenValue) {
        this.refreshTokenValue = refreshTokenValue;
    }

    public Instant getRefreshTokenIssuedAt() {
        return refreshTokenIssuedAt;
    }

    public void setRefreshTokenIssuedAt(Instant refreshTokenIssuedAt) {
        this.refreshTokenIssuedAt = refreshTokenIssuedAt;
    }

    public Instant getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public void setRefreshTokenExpiresAt(Instant refreshTokenExpiresAt) {
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }
}

