package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.BannedEmailDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BannedEmailMapper {
    // 이메일로 차단 여부 확인
    @Select("""
        SELECT id, email, reason, member_id as memberId, banned_by as bannedBy, 
               banned_at as bannedAt, ban_type as banType
        FROM BannedEmail 
        WHERE email = #{email}
    """)
    BannedEmailDTO findByEmail(String email);

    // 회원 ID로 차단 여부 확인
    @Select("""
        SELECT id, email, reason, member_id as memberId, banned_by as bannedBy, 
               banned_at as bannedAt, ban_type as banType
        FROM BannedEmail 
        WHERE member_id = #{memberId}
    """)
    BannedEmailDTO findByMemberId(int memberId);

    // 모든 차단된 이메일 조회
    @Select("""
        SELECT id, email, reason, member_id as memberId, banned_by as bannedBy, 
               banned_at as bannedAt, ban_type as banType
        FROM BannedEmail 
        ORDER BY banned_at DESC
    """)
    List<BannedEmailDTO> findAll();

    // 차단된 이메일 추가
    @Insert("""
        INSERT INTO BannedEmail (email, reason, member_id, banned_by, ban_type, banned_at)
        VALUES (#{email}, #{reason}, #{memberId}, #{bannedBy}, #{banType}, NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertBannedEmail(BannedEmailDTO bannedEmail);

    // 차단 해제 (이메일로)
    @Delete("DELETE FROM BannedEmail WHERE email = #{email}")
    int deleteByEmail(String email);

    // 차단 해제 (회원 ID로)
    @Delete("DELETE FROM BannedEmail WHERE member_id = #{memberId}")
    int deleteByMemberId(int memberId);

    // 차단 해제 (ID로)
    @Delete("DELETE FROM BannedEmail WHERE id = #{id}")
    int deleteById(int id);
}