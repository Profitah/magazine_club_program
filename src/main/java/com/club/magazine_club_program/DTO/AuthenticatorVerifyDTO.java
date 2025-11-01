package com.club.magazine_club_program.DTO;

public class AuthenticatorVerifyDTO {
    private String email;
    private String code; // Authenticator 앱에서 생성된 6자리 코드

    public AuthenticatorVerifyDTO() {
    }

    public AuthenticatorVerifyDTO(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

