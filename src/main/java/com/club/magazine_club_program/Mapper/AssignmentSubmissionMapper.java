package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.AssignmentSubmissionDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AssignmentSubmissionMapper {
    
    // 특정 모임 글에 제출된 모든 과제 조회 (회원 정보 포함)
    @Select("""
        SELECT 
            s.id, s.post_id, s.member_id, s.image_url, s.title, s.reflection,
            s.captured_at, s.location, s.submitted_at,
            m.name as member_name, m.sns_link
        FROM AssignmentSubmission s
        LEFT JOIN MemberInfo m ON s.member_id = m.id
        WHERE s.post_id = #{postId}
        ORDER BY s.submitted_at DESC
    """)
    List<AssignmentSubmissionDTO> findByPostId(@Param("postId") Integer postId);

    // 특정 회원이 특정 모임 글에 제출한 과제 조회
    @Select("""
        SELECT 
            s.id, s.post_id, s.member_id, s.image_url, s.title, s.reflection,
            s.captured_at, s.location, s.submitted_at,
            m.name as member_name, m.sns_link
        FROM AssignmentSubmission s
        LEFT JOIN MemberInfo m ON s.member_id = m.id
        WHERE s.post_id = #{postId} AND s.member_id = #{memberId}
        ORDER BY s.submitted_at DESC
        LIMIT 1
    """)
    AssignmentSubmissionDTO findByPostIdAndMemberId(@Param("postId") Integer postId, @Param("memberId") Integer memberId);

    // 특정 모임 글에 과제를 제출한 회원 수 조회
    @Select("""
        SELECT COUNT(DISTINCT member_id)
        FROM AssignmentSubmission
        WHERE post_id = #{postId}
    """)
    int countSubmissionsByPostId(@Param("postId") Integer postId);

    // 과제 제출
    @Insert("""
        INSERT INTO AssignmentSubmission 
            (post_id, member_id, image_url, title, reflection, captured_at, location, submitted_at)
        VALUES 
            (#{postId}, #{memberId}, #{imageUrl}, #{title}, #{reflection}, #{capturedAt}, #{location}, #{submittedAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssignmentSubmissionDTO submission);

    // 과제 제출 수정
    @Update("""
        UPDATE AssignmentSubmission
        SET image_url = #{imageUrl}, title = #{title}, reflection = #{reflection},
            captured_at = #{capturedAt}, location = #{location}
        WHERE id = #{id}
    """)
    int update(AssignmentSubmissionDTO submission);

    // 과제 제출 삭제
    @Delete("""
        DELETE FROM AssignmentSubmission
        WHERE id = #{id}
    """)
    int delete(@Param("id") Integer id);
}

