package com.club.magazine_club_program.DTO;

public class NotificationRecipientDTO {
    private int userId;
    private String userType;
    private String name;

    public NotificationRecipientDTO() {
    }

    public NotificationRecipientDTO(int userId, String userType, String name) {
        this.userId = userId;
        this.userType = userType;
        this.name = name;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
