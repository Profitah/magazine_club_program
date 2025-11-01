package com.club.magazine_club_program.DTO;

public class AdminDTO {
    private Integer id;
    private String email;
    private String totpSecret;

    public AdminDTO() {
    }

    public AdminDTO(String email, String totpSecret) {
        this.email = email;
        this.totpSecret = totpSecret;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }
}

