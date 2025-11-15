package com.club.magazine_club_program.DTO;

import java.time.LocalDateTime;

public class InstagramImageDTO {
    private Long id;
    private String username;
    private String originalUrl;
    private String s3Url;
    private LocalDateTime crawledAt;
    private LocalDateTime createdAt;

    public InstagramImageDTO() {}

    public InstagramImageDTO(String username, String originalUrl, String s3Url) {
        this.username = username;
        this.originalUrl = originalUrl;
        this.s3Url = s3Url;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getS3Url() {
        return s3Url;
    }

    public void setS3Url(String s3Url) {
        this.s3Url = s3Url;
    }

    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

