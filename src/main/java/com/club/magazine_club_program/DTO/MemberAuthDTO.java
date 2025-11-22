package com.club.magazine_club_program.DTO;

import java.time.LocalDateTime;

/**
 * 일반 로그인용 회원 인증 DTO
 * 기존 MemberDTO와 분리하여 새로운 파일로 관리
 */
public class MemberAuthDTO {
    private int id;
    private String name;
    private String email;
    private String password; // BCrypt로 암호화된 비밀번호
    private LocalDateTime suspendedUntil;

    public MemberAuthDTO() {}

    public MemberAuthDTO(int id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getSuspendedUntil() {
        return suspendedUntil;
    }

    public void setSuspendedUntil(LocalDateTime suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
    }

    public boolean isSuspended() {
        if (suspendedUntil == null) {
            return false;
        }
        return suspendedUntil.isAfter(java.time.LocalDateTime.now());
    }
}

