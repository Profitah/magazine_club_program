package com.club.magazine_club_program.DTO;

public class MemberDTO {
    private final int id;
    private String name;
    private String snsLink;        

    public MemberDTO(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public MemberDTO(int id, String name, String snsLink) {
        this.id = id;
        this.name = name;
        this.snsLink = snsLink;
    }

    public int getId() {
        return id;
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
}