package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.Service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 전체 멤버 조회 
    @GetMapping
    public ResponseEntity<?> getAllMembers() {
        try {
            List<MemberDTO> members = memberService.getAllMembers();
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.ok("전체 멤버 조회 실패: " + e.getMessage());
        }
    }

    // SNS 링크가 있는 멤버만 조회
    @GetMapping("/with-sns")
    public ResponseEntity<?> getMembersWithSNS() {
        try {
            List<MemberDTO> members = memberService.getMembersWithSNS();
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.ok("SNS 링크가 있는 멤버 조회 실패: " + e.getMessage());
        }
    }

    // 멤버 추가
    @PostMapping("/addMember")
    public ResponseEntity<?> addMember(@RequestBody MemberDTO memberDTO) {
        try {
            memberService.addMember(memberDTO);
            return ResponseEntity.ok(memberDTO.getName() + " 추가");
        } catch (Exception e) {
            return ResponseEntity.ok("멤버 추가 실패: " + e.getMessage());
        }
    }

    // 멤버 SNS 링크 업데이트
    @PutMapping("/update-sns")
    public ResponseEntity<?> updateMemberSNS(@RequestBody MemberDTO memberDTO) {
        try {
            boolean isUpdated = memberService.updateMemberSNS(memberDTO);
            if (isUpdated) {
                return ResponseEntity.ok("SNS 링크 업데이트 성공");
            } else {
                return ResponseEntity.ok("SNS 링크 업데이트 실패: 해당 ID를 찾을 수 없음");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("SNS 링크 업데이트 실패: " + e.getMessage());
        }
    }

    // 멤버 SNS 링크 삭제
    @DeleteMapping("/delete-sns/{id}")
    public ResponseEntity<?> deleteMemberSNS(@PathVariable int id) {
        try {
            boolean isDeleted = memberService.deleteMemberSNS(id);
            if (isDeleted) {
                return ResponseEntity.ok("SNS 링크 삭제 성공");
            } else {
                return ResponseEntity.ok("SNS 링크 삭제 실패: 해당 ID를 찾을 수 없음");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("SNS 링크 삭제 실패: " + e.getMessage());
        }
    }

    // 멤버 삭제
    @DeleteMapping("/deleteMember")
    public ResponseEntity<?> deleteMember(@RequestBody MemberDTO memberDTO) {
        try {
            boolean isDeleted = memberService.deleteMember(memberDTO.getId());
            if (isDeleted) {
                return ResponseEntity.ok(memberDTO.getId() + " 삭제");
            } else {
                return ResponseEntity.ok("멤버 삭제 실패: 해당 ID를 찾을 수 없음");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("멤버 삭제 실패: " + e.getMessage());
        }
    }

    // 회원 전체 정보 업데이트 (kakaoId, email, snsLink 포함)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMember(
            @PathVariable int id,
            @RequestBody MemberDTO memberDTO) {
        try {
            memberDTO.setId(id); // URL의 id를 DTO에 설정
            boolean isUpdated = memberService.updateMember(memberDTO);
            if (isUpdated) {
                return ResponseEntity.ok("회원 정보 업데이트 성공");
            } else {
                return ResponseEntity.ok("회원 정보 업데이트 실패: 해당 ID를 찾을 수 없음");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("회원 정보 업데이트 실패: " + e.getMessage());
        }
    }

    // 카카오 로그인 후 추가 정보 입력 (배열로 받기) - 구조분해할당용
    // [kakaoId, email, snsLink] 형태로 받음
    @PostMapping("/complete-registration/{id}")
    public ResponseEntity<?> completeRegistration(
            @PathVariable int id,
            @RequestBody List<String> additionalInfo) {
        try {
            // 배열 구조분해할당: [kakaoId, email, snsLink]
            if (additionalInfo == null || additionalInfo.size() < 3) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "배열 형태로 [kakaoId, email, snsLink]를 전달해주세요");
                return ResponseEntity.ok(errorResponse);
            }

            String kakaoId = additionalInfo.get(0);
            String email = additionalInfo.get(1);
            String snsLink = additionalInfo.get(2);

            // 기존 회원 정보 조회
            MemberDTO member = memberService.getAllMembers().stream()
                    .filter(m -> m.getId() == id)
                    .findFirst()
                    .orElse(null);

            if (member == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "해당 ID의 회원을 찾을 수 없습니다");
                return ResponseEntity.ok(errorResponse);
            }

            // 구조분해할당으로 받은 값으로 업데이트
            member.setKakaoId(kakaoId);
            member.setEmail(email);
            member.setSnsLink(snsLink);

            boolean isUpdated = memberService.updateMember(member);
            if (isUpdated) {
                // 업데이트된 회원 정보 다시 조회
                MemberDTO updatedMember = memberService.getAllMembers().stream()
                        .filter(m -> m.getId() == id)
                        .findFirst()
                        .orElse(null);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "회원가입 완료");
                response.put("member", updatedMember);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "회원 정보 업데이트 실패");
                return ResponseEntity.ok(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "회원 정보 입력 실패: " + e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    // 통합 회원가입 API (카카오 로그인 + 추가 정보 입력을 한 번에)
    // 프론트에서 회원가입처럼 보이게 하기 위한 API
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody List<String> signupInfo) {
        try {
            // 배열 구조분해할당: [kakaoId, email, snsLink]
            if (signupInfo == null || signupInfo.size() < 3) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "배열 형태로 [kakaoId, email, snsLink]를 전달해주세요");
                return ResponseEntity.ok(errorResponse);
            }

            String kakaoId = signupInfo.get(0);
            String email = signupInfo.get(1);
            String snsLink = signupInfo.get(2);

            // 카카오 ID로 회원 조회
            MemberDTO member = memberService.findByKakaoId(kakaoId);
            
            if (member == null) {
                // 회원이 없으면 생성
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "카카오 로그인을 먼저 진행해주세요");
                return ResponseEntity.ok(errorResponse);
            }

            // 회원 정보 업데이트
            member.setKakaoId(kakaoId);
            member.setEmail(email);
            member.setSnsLink(snsLink);

            boolean isUpdated = memberService.updateMember(member);
            if (isUpdated) {
                // 업데이트된 회원 정보 다시 조회
                MemberDTO updatedMember = memberService.getAllMembers().stream()
                        .filter(m -> m.getId() == member.getId())
                        .findFirst()
                        .orElse(null);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "회원가입 완료");
                response.put("member", updatedMember);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "회원가입 실패");
                return ResponseEntity.ok(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "회원가입 실패: " + e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
}