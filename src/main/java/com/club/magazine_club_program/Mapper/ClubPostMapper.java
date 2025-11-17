package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.ClubPostDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ClubPostMapper {
    
    // 모든 모임 글 조회 (활성화된 것만)
    @Select("""
        SELECT 
            p.id, p.title, p.description, p.created_by_admin_id, 
            p.created_at, p.due_date, p.is_active,
            a.email as created_by_name
        FROM ClubPost p
        LEFT JOIN Admin a ON p.created_by_admin_id = a.id
        WHERE p.is_active = true
        ORDER BY p.created_at DESC
    """)
    List<ClubPostDTO> findAllActive();

    // 모든 모임 글 조회 (관리자용)
    @Select("""
        SELECT 
            p.id, p.title, p.description, p.created_by_admin_id, 
            p.created_at, p.due_date, p.is_active,
            a.email as created_by_name
        FROM ClubPost p
        LEFT JOIN Admin a ON p.created_by_admin_id = a.id
        ORDER BY p.created_at DESC
    """)
    List<ClubPostDTO> findAll();

    // ID로 모임 글 조회
    @Select("""
        SELECT 
            p.id, p.title, p.description, p.created_by_admin_id, 
            p.created_at, p.due_date, p.is_active,
            a.email as created_by_name
        FROM ClubPost p
        LEFT JOIN Admin a ON p.created_by_admin_id = a.id
        WHERE p.id = #{id}
    """)
    ClubPostDTO findById(@Param("id") Integer id);

    // 모임 글 생성
    @Insert("""
        INSERT INTO ClubPost (title, description, created_by_admin_id, created_at, due_date, is_active)
        VALUES (#{title}, #{description}, #{createdByAdminId}, #{createdAt}, #{dueDate}, #{isActive})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ClubPostDTO clubPost);

    // 모임 글 수정
    @Update("""
        UPDATE ClubPost
        SET title = #{title}, description = #{description}, due_date = #{dueDate}, is_active = #{isActive}
        WHERE id = #{id}
    """)
    int update(ClubPostDTO clubPost);

    // 모임 글 삭제 (비활성화)
    @Update("""
        UPDATE ClubPost
        SET is_active = false
        WHERE id = #{id}
    """)
    int delete(@Param("id") Integer id);
}

