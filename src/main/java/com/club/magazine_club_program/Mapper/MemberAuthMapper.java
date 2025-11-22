package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.MemberAuthDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 일반 로그인용 회원 인증 Mapper
 * 기존 MemberInfoMapper와 분리하여 새로운 파일로 관리
 */
@Mapper
public interface MemberAuthMapper {

    /**
     * 이메일과 비밀번호로 회원 조회
     */
    @Select("""
        SELECT id, name, email, password, suspended_until as suspendedUntil
        FROM MemberInfo
        WHERE email = #{email} AND password IS NOT NULL
        LIMIT 1
    """)
    MemberAuthDTO findByEmail(@Param("email") String email);

    /**
     * 일반 로그인 회원 가입
     */
    @Insert("""
        INSERT INTO MemberInfo (name, email, password)
        VALUES (#{name}, #{email}, #{password})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int registerMember(MemberAuthDTO member);

    /**
     * 비밀번호 업데이트
     */
    @Update("""
        UPDATE MemberInfo 
        SET password = #{password}
        WHERE email = #{email}
    """)
    int updatePassword(@Param("email") String email, @Param("password") String password);

    /**
     * 이메일로 회원 존재 확인
     */
    @Select("""
        SELECT COUNT(*) > 0
        FROM MemberInfo
        WHERE email = #{email} AND password IS NOT NULL
    """)
    boolean existsByEmail(@Param("email") String email);
}

