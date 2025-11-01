package com.club.magazine_club_program.DTO;

public class SecretAndQrCodeDTO {
    private String secret;
    private String qrCode;

    public SecretAndQrCodeDTO(String secret, String qrCode) {
        this.secret = secret;
        this.qrCode = qrCode;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
}

