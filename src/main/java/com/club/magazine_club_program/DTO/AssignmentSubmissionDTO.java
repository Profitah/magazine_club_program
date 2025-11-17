package com.club.magazine_club_program.DTO;

import java.time.LocalDateTime;

public class AssignmentSubmissionDTO {
    private Integer id;
    private Integer postId;            // 모임 글 ID
    private Integer memberId;          // 회원 ID
    private String memberName;         // 회원 이름 (JOIN으로 가져옴)
    private String imageUrl;           // 업로드한 사진 URL
    private String title;              // 과제 제목
    private String reflection;         // 과제 소감/한줄 설명
    private LocalDateTime capturedAt;  // 사진 촬영일시
    private String location;           // 촬영 위치
    private LocalDateTime submittedAt; // 제출일시
    private String snsLink;            // 회원 SNS 링크 (선택사항)

    public AssignmentSubmissionDTO() {}

    public AssignmentSubmissionDTO(Integer postId, Integer memberId) {
        this.postId = postId;
        this.memberId = memberId;
        this.submittedAt = LocalDateTime.now();
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

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReflection() {
        return reflection;
    }

    public void setReflection(String reflection) {
        this.reflection = reflection;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSnsLink() {
        return snsLink;
    }

    public void setSnsLink(String snsLink) {
        this.snsLink = snsLink;
    }
}

