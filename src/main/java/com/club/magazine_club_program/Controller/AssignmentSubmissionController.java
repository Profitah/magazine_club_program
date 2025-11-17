package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.AssignmentSubmissionDTO;
import com.club.magazine_club_program.DTO.PhotoMetadataDTO;
import com.club.magazine_club_program.Service.AssignmentSubmissionService;
import com.club.magazine_club_program.Service.PhotoMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/assignments")
public class AssignmentSubmissionController {

    private final AssignmentSubmissionService submissionService;
    private final PhotoMetadataService photoMetadataService;

    public AssignmentSubmissionController(
            AssignmentSubmissionService submissionService,
            PhotoMetadataService photoMetadataService) {
        this.submissionService = submissionService;
        this.photoMetadataService = photoMetadataService;
    }

    // 특정 모임 글에 제출된 모든 과제 조회 (과제 인증한 회원들의 프로필)
    @GetMapping("/posts/{postId}/submissions")
    public ResponseEntity<?> getSubmissionsByPostId(@PathVariable Integer postId) {
        try {
            List<AssignmentSubmissionDTO> submissions = submissionService.getSubmissionsByPostId(postId);
            Map<String, Object> response = new HashMap<>();
            response.put("postId", postId);
            response.put("submissionCount", submissions.size());
            response.put("submissions", submissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok("과제 제출 조회 실패: " + e.getMessage());
        }
    }

    // 특정 회원이 특정 모임 글에 제출한 과제 조회
    @GetMapping("/posts/{postId}/members/{memberId}")
    public ResponseEntity<?> getSubmissionByPostIdAndMemberId(
            @PathVariable Integer postId,
            @PathVariable Integer memberId) {
        try {
            AssignmentSubmissionDTO submission = submissionService.getSubmissionByPostIdAndMemberId(postId, memberId);
            if (submission == null) {
                return ResponseEntity.ok("과제 제출 내역이 없습니다.");
            }
            return ResponseEntity.ok(submission);
        } catch (Exception e) {
            return ResponseEntity.ok("과제 제출 조회 실패: " + e.getMessage());
        }
    }

    // 과제 제출 (사진 업로드 + 메타데이터 추출)
    @PostMapping("/posts/{postId}/submit")
    public ResponseEntity<?> submitAssignment(
            @PathVariable Integer postId,
            @RequestParam("memberId") Integer memberId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "reflection", required = false) String reflection) {
        try {
            // 사진 메타데이터 추출
            PhotoMetadataDTO photoMetadata = photoMetadataService.extractMetadata(file);
            
            if (!photoMetadata.isSuccess()) {
                return ResponseEntity.ok("사진 메타데이터 추출 실패: " + photoMetadata.getMessage());
            }

            // 과제 제출 정보 생성
            AssignmentSubmissionDTO submission = new AssignmentSubmissionDTO(postId, memberId);
            submission.setImageUrl(photoMetadata.getImageUrl());
            submission.setTitle(title != null ? title : photoMetadata.getTitle());
            submission.setReflection(reflection != null ? reflection : photoMetadata.getReflection());
            submission.setCapturedAt(photoMetadata.getCapturedAt());
            submission.setLocation(photoMetadata.getLocation());

            // 과제 제출 저장
            AssignmentSubmissionDTO saved = submissionService.submitAssignment(submission);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "과제 제출 완료");
            response.put("submission", saved);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok("과제 제출 실패: " + e.getMessage());
        }
    }

    // 과제 제출 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubmission(
            @PathVariable Integer id,
            @RequestBody AssignmentSubmissionDTO submission) {
        try {
            submission.setId(id);
            boolean updated = submissionService.updateSubmission(submission);
            if (updated) {
                return ResponseEntity.ok("과제 제출 수정 완료");
            } else {
                return ResponseEntity.ok("과제 제출 수정 실패");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("과제 제출 수정 실패: " + e.getMessage());
        }
    }

    // 과제 제출 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubmission(@PathVariable Integer id) {
        try {
            boolean deleted = submissionService.deleteSubmission(id);
            if (deleted) {
                return ResponseEntity.ok("과제 제출 삭제 완료");
            } else {
                return ResponseEntity.ok("과제 제출 삭제 실패");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("과제 제출 삭제 실패: " + e.getMessage());
        }
    }

    // 특정 모임 글에 과제를 제출한 회원 수 조회
    @GetMapping("/posts/{postId}/count")
    public ResponseEntity<?> getSubmissionCount(@PathVariable Integer postId) {
        try {
            int count = submissionService.getSubmissionCountByPostId(postId);
            Map<String, Object> response = new HashMap<>();
            response.put("postId", postId);
            response.put("submissionCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok("과제 제출 수 조회 실패: " + e.getMessage());
        }
    }
}

