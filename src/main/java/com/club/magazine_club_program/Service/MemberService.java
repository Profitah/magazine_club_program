package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.Mapper.MemberInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    private static final Logger log = LoggerFactory.getLogger(MemberService.class);
    private final MemberInfoMapper memberInfoMapper;

    public MemberService(MemberInfoMapper memberInfoMapper) {
        this.memberInfoMapper = memberInfoMapper;
    }

    // 모든 멤버 조회 
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

    // 회원 전체 정보 업데이트 (email, snsLink 포함)
    public boolean updateMember(MemberDTO memberDTO) {
        return memberInfoMapper.updateMember(memberDTO) > 0;
    }

    // 이메일로 회원 조회
    public MemberDTO findByEmail(String email) {
        return memberInfoMapper.findAll().stream()
                .filter(m -> email != null && email.equals(m.getEmail()))
                .findFirst()
                .orElse(null);
    }

    // 카카오 로그인 회원 생성 또는 조회 (자동 회원가입)
    public MemberDTO findOrCreateKakaoMember(String kakaoId, String nickname, String email) {
        // 이메일로 기존 회원 조회
        MemberDTO member = findByEmail(email);
        
        if (member != null) {
            // 기존 회원이면 정보 업데이트 (닉네임 변경 가능)
            boolean needsUpdate = false;
            if (nickname != null && !nickname.equals(member.getName())) {
                member.setName(nickname);
                needsUpdate = true;
            }
            if (needsUpdate) {
                memberInfoMapper.updateKakaoMember(member);
                // 업데이트 후 다시 조회하여 최신 정보 반환
                member = findByEmail(email);
            }
            log.info("카카오 기존 회원 로그인: email={}, name={}", email, nickname);
            return member;
        } else {
            // 신규 회원 생성
            MemberDTO newMember = new MemberDTO(nickname, email);
            newMember.setSnsLink("카카오 로그인");
            memberInfoMapper.addKakaoMember(newMember);
            log.info("카카오 신규 회원 가입: email={}, name={}", email, nickname);
            // 생성된 회원 정보 다시 조회 (auto-generated id 포함)
            return findByEmail(email);
        }
    }
}
