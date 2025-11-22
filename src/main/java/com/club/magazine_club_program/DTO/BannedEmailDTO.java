package com.club.magazine_club_program.DTO;

import java.time.LocalDateTime;

public class BannedEmailDTO {
    private int id;
    private String email;
    private String reason;
    private int memberId; // 차단된 회원 ID (선택적)
    private String bannedBy; // 차단한 관리자 이메일
    private LocalDateTime bannedAt;
    private LocalDateTime suspendedUntil; // 일시정지 해제 시간 (null이면 영구 차단)
    private String banType; // "LOGIN_BAN" (로그인 정지), "SERVICE_BAN" (서비스 방출), "PERMANENT_BAN" (영구 차단)

    public BannedEmailDTO() {}

    public BannedEmailDTO(String email, String reason, int memberId, String bannedBy, String banType) {
        this.email = email;
        this.reason = reason;
        this.memberId = memberId;
        this.bannedBy = bannedBy;
        this.banType = banType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getBannedBy() {
        return bannedBy;
    }

    public void setBannedBy(String bannedBy) {
        this.bannedBy = bannedBy;
    }

    public LocalDateTime getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(LocalDateTime bannedAt) {
        this.bannedAt = bannedAt;
    }

    public String getBanType() {
        return banType;
    }

    public void setBanType(String banType) {
        this.banType = banType;
    }

    public LocalDateTime getSuspendedUntil() {
        return suspendedUntil;
    }

    public void setSuspendedUntil(LocalDateTime suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
    }
}

