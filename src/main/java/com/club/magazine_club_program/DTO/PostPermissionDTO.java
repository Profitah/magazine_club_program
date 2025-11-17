package com.club.magazine_club_program.DTO;

import java.time.LocalDateTime;

/**
 * 모임 글별 권한 위임을 위한 DTO
 * 특정 모임 글에 대한 관리 권한을 다른 관리자에게 위임할 수 있음
 */
public class PostPermissionDTO {
    private Integer id;
    private Integer postId;              // 모임 글 ID
    private Integer adminId;             // 권한을 가진 Admin ID
    private String adminEmail;           // Admin 이메일 (JOIN으로 가져옴)
    private String permissionType;       // OWNER, EDITOR, VIEWER (소유자, 편집자, 조회자)
    private Integer grantedByAdminId;    // 권한을 부여한 Admin ID
    private LocalDateTime grantedAt;     // 권한 부여일시

    public PostPermissionDTO() {}

    public PostPermissionDTO(Integer postId, Integer adminId, String permissionType, Integer grantedByAdminId) {
        this.postId = postId;
        this.adminId = adminId;
        this.permissionType = permissionType;
        this.grantedByAdminId = grantedByAdminId;
        this.grantedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public Integer getGrantedByAdminId() {
        return grantedByAdminId;
    }

    public void setGrantedByAdminId(Integer grantedByAdminId) {
        this.grantedByAdminId = grantedByAdminId;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }
}

