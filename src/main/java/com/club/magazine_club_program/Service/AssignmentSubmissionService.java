package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.AssignmentSubmissionDTO;
import com.club.magazine_club_program.Mapper.AssignmentSubmissionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentSubmissionService {
    
    private final AssignmentSubmissionMapper submissionMapper;

    public AssignmentSubmissionService(AssignmentSubmissionMapper submissionMapper) {
        this.submissionMapper = submissionMapper;
    }

    // 특정 모임 글에 제출된 모든 과제 조회 (과제 인증한 회원들의 프로필)
    public List<AssignmentSubmissionDTO> getSubmissionsByPostId(Integer postId) {
        return submissionMapper.findByPostId(postId);
    }

    // 특정 회원이 특정 모임 글에 제출한 과제 조회
    public AssignmentSubmissionDTO getSubmissionByPostIdAndMemberId(Integer postId, Integer memberId) {
        return submissionMapper.findByPostIdAndMemberId(postId, memberId);
    }

    // 특정 모임 글에 과제를 제출한 회원 수 조회
    public int getSubmissionCountByPostId(Integer postId) {
        return submissionMapper.countSubmissionsByPostId(postId);
    }

    // 과제 제출
    public AssignmentSubmissionDTO submitAssignment(AssignmentSubmissionDTO submission) {
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(LocalDateTime.now());
        }
        
        // 이미 제출한 경우 업데이트, 아니면 새로 생성
        AssignmentSubmissionDTO existing = submissionMapper.findByPostIdAndMemberId(
            submission.getPostId(), 
            submission.getMemberId()
        );
        
        if (existing != null) {
            submission.setId(existing.getId());
            submissionMapper.update(submission);
        } else {
            submissionMapper.insert(submission);
        }
        
        return submission;
    }

    // 과제 제출 수정
    public boolean updateSubmission(AssignmentSubmissionDTO submission) {
        int updatedRows = submissionMapper.update(submission);
        return updatedRows > 0;
    }

    // 과제 제출 삭제
    public boolean deleteSubmission(Integer id) {
        int deletedRows = submissionMapper.delete(id);
        return deletedRows > 0;
    }
}

