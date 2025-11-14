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
        SELECT id, name, sns_link, kakao_id, email
        FROM MemberInfo
        ORDER BY id
    """)
    List<MemberDTO> findAll();

    // SNS 링크가 있는 멤버만 조회
    @Select("""
        SELECT id, name, sns_link, kakao_id, email
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

    // 카카오 ID로 회원 조회
    @Select("""
        SELECT id, name, sns_link, kakao_id, email
        FROM MemberInfo
        WHERE kakao_id = #{kakaoId}
        LIMIT 1
    """)
    MemberDTO findByKakaoId(@Param("kakaoId") String kakaoId);

    // 카카오 로그인 회원 생성 (카카오 ID, 이메일 포함)
    @Insert("""
        INSERT INTO MemberInfo (name, sns_link, kakao_id, email)
        VALUES (#{name}, #{snsLink}, #{kakaoId}, #{email})
    """)
    int addKakaoMember(MemberDTO member);

    // 카카오 ID로 회원 정보 업데이트
    @Update("""
        UPDATE MemberInfo 
        SET name = #{name}, email = #{email}
        WHERE kakao_id = #{kakaoId}
    """)
    int updateKakaoMember(MemberDTO member);

    // 회원 ID로 전체 정보 업데이트 (kakaoId, email, snsLink 포함)
    @Update("""
        UPDATE MemberInfo 
        SET name = #{name}, 
            kakao_id = #{kakaoId}, 
            email = #{email}, 
            sns_link = #{snsLink}
        WHERE id = #{id}
    """)
    int updateMember(MemberDTO member);
}