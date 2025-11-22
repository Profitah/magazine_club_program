package com.club.magazine_club_program.DTO;

import java.time.LocalDateTime;

public class MemberDTO {
    private int id;
    private String name;
    private String snsLink;        
    private String email;
    private LocalDateTime suspendedUntil; // 회원 자격 일시정지 해제 시간 (null이면 정상)

    public MemberDTO() {}

    public MemberDTO(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public MemberDTO(int id, String name, String snsLink) {
        this.id = id;
        this.name = name;
        this.snsLink = snsLink;
    }

    public MemberDTO(String name, String email) {
        this.name = name;
        this.email = email;
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

    public String getSnsLink() {
        return (snsLink == null || snsLink.trim().isEmpty()) ? "링크없음" : snsLink;
    }

    public void setSnsLink(String snsLink) {
        this.snsLink = snsLink;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getSuspendedUntil() {
        return suspendedUntil;
    }

    public void setSuspendedUntil(LocalDateTime suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
    }

    /**
     * 회원 자격이 일시정지되어 있는지 확인
     */
    public boolean isSuspended() {
        if (suspendedUntil == null) {
            return false;
        }
        return suspendedUntil.isAfter(java.time.LocalDateTime.now());
    }
}