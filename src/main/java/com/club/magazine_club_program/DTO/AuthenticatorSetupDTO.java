package com.club.magazine_club_program.DTO;

public class AuthenticatorSetupDTO {
    private String email;
    private String qrCode; // Base64 인코딩된 QR 코드 이미지
    private String secret; // TOTP Secret (수동 입력용)

    public AuthenticatorSetupDTO() {
    }

    public AuthenticatorSetupDTO(String email, String qrCode, String secret) {
        this.email = email;
        this.qrCode = qrCode;
        this.secret = secret;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}

