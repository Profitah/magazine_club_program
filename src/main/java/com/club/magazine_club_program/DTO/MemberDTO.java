package com.club.magazine_club_program.DTO;

public class MemberDTO {
    private int id;
    private String name;
    private String snsLink;        
    private String kakaoId;
    private String email;

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

    public MemberDTO(String name, String kakaoId, String email) {
        this.name = name;
        this.kakaoId = kakaoId;
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

    public String getKakaoId() {
        return kakaoId;
    }

    public void setKakaoId(String kakaoId) {
        this.kakaoId = kakaoId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}