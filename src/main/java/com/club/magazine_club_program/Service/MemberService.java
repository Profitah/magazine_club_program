package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.Mapper.MemberInfoMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    private final MemberInfoMapper memberInfoMapper;

    public MemberService(MemberInfoMapper memberInfoMapper) {
        this.memberInfoMapper = memberInfoMapper;
    }

    // 모든 멤버 조회 (SNS 링크 포함)
    public List<MemberDTO> getAllMembers() {
        return memberInfoMapper.findAll();
    }

    // SNS 링크가 있는 멤버만 조회
    public List<MemberDTO> getMembersWithSNS() {
        return memberInfoMapper.findMembersWithSNS();
    }
    
    // 신규 멤버 추가
    public void addMember(MemberDTO memberDTO) {
        memberInfoMapper.addMember(memberDTO);
    }

    // 멤버 SNS 링크 업데이트
    public boolean updateMemberSNS(MemberDTO memberDTO) {
        return memberInfoMapper.updateMemberSNS(memberDTO) > 0;
    }

    // 멤버 SNS 링크 삭제
    public boolean deleteMemberSNS(int id) {
        return memberInfoMapper.deleteMemberSNS(id) > 0;
    }

    // 멤버 삭제
    public boolean deleteMember(int id) {
        return memberInfoMapper.deleteMember(id) > 0;
    }
}
