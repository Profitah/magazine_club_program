package com.club.magazine_club_program.DTO;

public class MemberDTO {
    private final int id;
    private String name;

    public MemberDTO(int id, String name) {
        this.id = id;
        this.name = name;
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
}