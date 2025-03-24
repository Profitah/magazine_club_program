package com.club.magazine_club_program.Mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

import com.club.magazine_club_program.DTO.MemberDTO;

@Mapper
public interface MemberInfoMapper {
    // 전체 멤버 조회
    @Select("""
        SELECT id, name
        FROM MemberInfo
    """)
    List<MemberDTO> findAll();

    // 멤버 추가
    @Insert("""
        INSERT INTO MemberInfo (name)
        VALUES (#{name})
    """)
    int addMember(MemberDTO member);

    // 멤버 삭제
    @Delete("""
        DELETE FROM MemberInfo
        WHERE id = #{id}
    """)
    int deleteMember(int id);
}