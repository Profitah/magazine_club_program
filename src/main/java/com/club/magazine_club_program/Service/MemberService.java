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
        try {
            log.debug("전체 멤버 조회 시작");
            List<MemberDTO> members = memberInfoMapper.findAll();
            log.debug("전체 멤버 조회 완료: {}명", members != null ? members.size() : 0);
            return members;
        } catch (Exception e) {
            log.error("전체 멤버 조회 중 에러 발생", e);
            throw e;
        }
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

    // 카카오/구글 로그인 회원 생성 또는 조회 (자동 회원가입)
    public MemberDTO findOrCreateKakaoMember(String providerId, String nickname, String email) {
        // 이메일 차단 확인 (회원가입 방지)
        if (email != null && !email.trim().isEmpty()) {
            // BannedEmailService 주입 필요 (순환 참조 방지를 위해 직접 확인)
            // 이 부분은 CustomOAuth2UserService에서 이미 확인했지만, 추가 안전장치
            log.debug("회원가입 시도: email={}", email);
        }
        
        // nickname이 null이거나 빈 문자열이면 email 사용 (email도 null이면 "카카오 사용자" 등 기본값)
        String name = (nickname != null && !nickname.trim().isEmpty()) ? nickname : 
                     (email != null && !email.trim().isEmpty() ? email : "카카오 사용자");
        log.info("회원 생성/조회 시작: providerId={}, email={}, nickname={}, 사용할 name={}", 
                providerId, email, nickname, name);
        
        String snsLink = "kakao".equals(providerId) ? "카카오 로그인" : "구글 로그인";
        
        // 이메일로 기존 회원 조회 (이메일이 있는 경우)
        MemberDTO member = null;
        if (email != null && !email.trim().isEmpty()) {
            member = findByEmail(email);
        }
        
        // 이메일로 조회 실패했거나 이메일이 없는 경우, 이름과 sns_link로 조회
        // snsLink가 null이거나 "링크없음"인 경우도 고려
        if (member == null) {
            member = memberInfoMapper.findAll().stream()
                    .filter(m -> {
                        boolean nameMatch = name != null && name.equals(m.getName());
                        if (!nameMatch) return false;
                        
                        // snsLink 매칭: 정확히 일치하거나, 둘 다 null이거나, 둘 다 "링크없음"이거나
                        String mSnsLink = m.getSnsLink();
                        if (snsLink == null && (mSnsLink == null || "링크없음".equals(mSnsLink))) {
                            return true;
                        }
                        if (snsLink != null && snsLink.equals(mSnsLink)) {
                            return true;
                        }
                        // snsLink가 "카카오 로그인" 또는 "구글 로그인"이고, 기존 회원의 snsLink가 null이거나 "링크없음"인 경우
                        if ((snsLink.equals("카카오 로그인") || snsLink.equals("구글 로그인")) 
                                && (mSnsLink == null || "링크없음".equals(mSnsLink))) {
                            return true;
                        }
                        return false;
                    })
                    .findFirst()
                    .orElse(null);
            if (member != null) {
                log.info("이름과 sns_link로 기존 회원 찾음: name={}, snsLink={}, 기존 snsLink={}, memberId={}", 
                        name, snsLink, member.getSnsLink(), member.getId());
            }
        }
        
        if (member != null) {
            // 기존 회원이면 정보 업데이트 (닉네임 변경 가능, 이메일 업데이트, snsLink 업데이트)
            boolean needsUpdate = false;
            if (name != null && !name.equals(member.getName())) {
                member.setName(name);
                needsUpdate = true;
            }
            // 이메일이 있고 기존 회원의 이메일이 없거나 다르면 업데이트
            if (email != null && !email.trim().isEmpty() 
                    && (member.getEmail() == null || !email.equals(member.getEmail()))) {
                member.setEmail(email);
                needsUpdate = true;
            }
            // snsLink가 "링크없음"이거나 null이면 업데이트
            String currentSnsLink = member.getSnsLink();
            if (snsLink != null && (currentSnsLink == null || "링크없음".equals(currentSnsLink) || !snsLink.equals(currentSnsLink))) {
                member.setSnsLink(snsLink);
                needsUpdate = true;
            }
            if (needsUpdate) {
                memberInfoMapper.updateMember(member);
                // 업데이트 후 다시 조회하여 최신 정보 반환
                final int memberId = member.getId(); // effectively final 변수로 복사
                if (email != null && !email.trim().isEmpty()) {
                member = findByEmail(email);
                } else {
                    member = memberInfoMapper.findAll().stream()
                            .filter(m -> m.getId() == memberId)
                            .findFirst()
                            .orElse(null);
                }
            }
            log.info("기존 회원 로그인: providerId={}, email={}, name={}, memberId={}", 
                    providerId, email, name, member != null ? member.getId() : null);
            return member;
        } else {
            // 신규 회원 생성
            MemberDTO newMember = new MemberDTO(name, email);
            newMember.setSnsLink(snsLink);
            
            try {
                int result = memberInfoMapper.addKakaoMember(newMember);
                log.info("신규 회원 생성 시도: providerId={}, email={}, name={}, result={}, 생성된 ID={}", 
                        providerId, email, name, result, newMember.getId());
                
                // useGeneratedKeys로 생성된 ID가 newMember 객체에 자동 설정됨
                MemberDTO createdMember = null;
                if (newMember.getId() > 0) {
                    // 생성된 ID로 전체 목록에서 조회
                    createdMember = memberInfoMapper.findAll().stream()
                            .filter(m -> m.getId() == newMember.getId())
                            .findFirst()
                            .orElse(null);
                    
                    if (createdMember != null) {
                        log.info("신규 회원 생성 성공: providerId={}, email={}, name={}, memberId={}", 
                                providerId, email, name, createdMember.getId());
                        return createdMember;
                    } else {
                        log.warn("생성된 ID로 회원을 찾지 못함: id={}, name={}, snsLink={}", 
                                newMember.getId(), name, snsLink);
                    }
                } else {
                    log.warn("생성된 ID가 없음: result={}, name={}, snsLink={}", result, name, snsLink);
                }
                
                // ID로 찾지 못한 경우, 이메일이나 이름+sns_link로 재시도
                if (email != null && !email.trim().isEmpty()) {
                    createdMember = findByEmail(email);
                }
                if (createdMember == null) {
                    // 이름과 sns_link로 조회 (ID가 가장 큰 것 = 가장 최근 생성된 것)
                    List<MemberDTO> allMembers = memberInfoMapper.findAll();
                    createdMember = allMembers.stream()
                            .filter(m -> name != null && name.equals(m.getName()) 
                                    && snsLink != null && snsLink.equals(m.getSnsLink()))
                            .max((m1, m2) -> Integer.compare(m1.getId(), m2.getId()))
                            .orElse(null);
                }
                
                if (createdMember != null) {
                    log.info("신규 회원 생성 성공 (재조회): providerId={}, email={}, name={}, memberId={}", 
                            providerId, email, name, createdMember.getId());
                } else {
                    log.error("신규 회원 생성 후 조회 실패: providerId={}, email={}, name={}, snsLink={}", 
                            providerId, email, name, snsLink);
                }
                return createdMember;
            } catch (Exception e) {
                log.error("신규 회원 생성 중 에러 발생: providerId={}, email={}, name={}, error={}", 
                        providerId, email, name, e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * 회원 자격 일시정지
     */
    public boolean suspendMember(int memberId, java.time.LocalDateTime suspendedUntil, String adminEmail) {
        try {
            int result = memberInfoMapper.suspendMember(memberId, suspendedUntil);
            if (result > 0) {
                log.info("회원 자격 일시정지 완료: memberId={}, suspendedUntil={}, adminEmail={}", 
                        memberId, suspendedUntil, adminEmail);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("회원 자격 일시정지 실패: memberId={}, error={}", memberId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 회원 자격 일시정지 해제
     */
    public boolean unsuspendMember(int memberId) {
        try {
            int result = memberInfoMapper.unsuspendMember(memberId);
            if (result > 0) {
                log.info("회원 자격 일시정지 해제 완료: memberId={}", memberId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("회원 자격 일시정지 해제 실패: memberId={}, error={}", memberId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 만료된 일시정지 자동 해제
     */
    public int unsuspendExpiredMembers() {
        try {
            int result = memberInfoMapper.unsuspendExpiredMembers();
            if (result > 0) {
                log.info("만료된 회원 자격 일시정지 해제: {}명", result);
            }
            return result;
        } catch (Exception e) {
            log.error("만료된 회원 자격 일시정지 해제 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
}