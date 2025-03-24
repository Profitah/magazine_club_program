package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.Service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 멤버 추가
    @PostMapping("/addMember")
    public ResponseEntity<?> addMember(@RequestBody MemberDTO memberDTO) {
        try {
            memberService.addMember(memberDTO);
            return ResponseEntity.ok( memberDTO.getName() + "추가");
        } catch (Exception e) {
            return ResponseEntity.ok("멤버 추가 실패: " + e.getMessage());
        }
    }

    // 멤버 삭제
    @DeleteMapping("/deleteMember")
    public ResponseEntity<?> deleteMember(@RequestBody MemberDTO memberDTO) {
        try {
            boolean isDeleted = memberService.deleteMember(memberDTO.getId());
            if (isDeleted) {
                return ResponseEntity.ok( memberDTO.getId() + "삭제");
            } else {
                return ResponseEntity.ok("멤버 삭제 실패: 해당 ID를 찾을 수 없음");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("멤버 삭제 실패: " + e.getMessage());
        }
    }
}
