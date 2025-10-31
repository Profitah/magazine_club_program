package com.club.magazine_club_program.Mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.club.magazine_club_program.DTO.MemberDTO;

@Mapper
public interface MemberInfoMapper {
    // 전체 멤버 조회 
    @Select("""
        SELECT id, name, sns_link
        FROM MemberInfo
        ORDER BY id
    """)
    List<MemberDTO> findAll();

    // SNS 링크가 있는 멤버만 조회
    @Select("""
        SELECT id, name, sns_link
        FROM MemberInfo
        WHERE sns_link IS NOT NULL AND sns_link != ''
        ORDER BY id
    """)
    List<MemberDTO> findMembersWithSNS();

    // 멤버 추가
    @Insert("""
        INSERT INTO MemberInfo (name, sns_link)
        VALUES (#{name}, #{snsLink})
    """)
    int addMember(MemberDTO member);

    // 멤버 SNS 링크 업데이트
    @Update("""
        UPDATE MemberInfo 
        SET sns_link = #{snsLink}
        WHERE id = #{id}
    """)
    int updateMemberSNS(MemberDTO member);

    // 멤버 SNS 링크 삭제
    @Update("""
        UPDATE MemberInfo 
        SET sns_link = NULL
        WHERE id = #{id}
    """)
    int deleteMemberSNS(@Param("id") int id);

    // 멤버 삭제
    @Delete("""
        DELETE FROM MemberInfo
        WHERE id = #{id}
    """)
    int deleteMember(int id);
}