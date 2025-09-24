package com.club.magazine_club_program.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class ThumbnailDTO {
    private String username;
    private List<String> thumbnailUrls;
    private int totalCount;
    private LocalDateTime crawledAt;
    private boolean success;
    private String message;
    
    // 페이지네이션을 위한 필드
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private List<String> currentPageUrls;
    
    public ThumbnailDTO() {}
    
    public ThumbnailDTO(String username, List<String> thumbnailUrls, int totalCount, LocalDateTime crawledAt, boolean success, String message) {
        this.username = username;
        this.thumbnailUrls = thumbnailUrls;
        this.totalCount = totalCount;
        this.crawledAt = crawledAt;
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public List<String> getThumbnailUrls() {
        return thumbnailUrls;
    }
    
    public void setThumbnailUrls(List<String> thumbnailUrls) {
        this.thumbnailUrls = thumbnailUrls;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }
    
    public void setCrawledAt(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public List<String> getCurrentPageUrls() {
        return currentPageUrls;
    }
    
    public void setCurrentPageUrls(List<String> currentPageUrls) {
        this.currentPageUrls = currentPageUrls;
    }
}