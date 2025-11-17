package com.club.magazine_club_program.DTO;

import java.time.LocalDateTime;

public class ClubPostDTO {
    private Integer id;
    private String title;              // 모임 글 제목
    private String description;        // 모임 글 설명
    private Integer createdByAdminId;  // 작성자 Admin ID (권한 위임을 위해)
    private String createdByName;      // 작성자 이름 (표시용, JOIN으로 가져옴)
    private LocalDateTime createdAt;   // 생성일시
    private LocalDateTime dueDate;     // 과제 마감일 (선택사항)
    private boolean isActive;          // 활성화 여부

    public ClubPostDTO() {}

    public ClubPostDTO(String title, String description, Integer createdByAdminId) {
        this.title = title;
        this.description = description;
        this.createdByAdminId = createdByAdminId;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCreatedByAdminId() {
        return createdByAdminId;
    }

    public void setCreatedByAdminId(Integer createdByAdminId) {
        this.createdByAdminId = createdByAdminId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

