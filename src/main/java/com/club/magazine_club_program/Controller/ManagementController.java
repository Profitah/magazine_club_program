package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.ManagementDTO;
import com.club.magazine_club_program.DTO.MemberDTO;
import com.club.magazine_club_program.Service.ManagementService;
import com.club.magazine_club_program.Service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/management")
public class ManagementController {

    @Autowired
    private ManagementService managementService;

    @Autowired
    private MemberService memberService;

    // 모든 멤버 출석, 과제 여부 조회
    @GetMapping("/")
    public ResponseEntity<List<ManagementDTO>> getMemberStat() {
        List<ManagementDTO> stats = managementService.getAllMemberStat();
        List<MemberDTO> members = memberService.getAllMembers();

        // 통합 데이터를 구성
        List<ManagementDTO> integratedStats = stats.stream().map(stat -> {
            members.stream()
                    .filter(member -> member.getId() == stat.getId())
                    .findFirst()
                    .ifPresent(member -> stat.setName(member.getName())); // 이름 추가
            return stat;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(integratedStats);
    }

    // 출석 여부 업데이트
    @PostMapping("updateAttendance")
    public ResponseEntity<String> updateAttendance(@RequestParam int id, @RequestParam boolean isAttendance) {
        ManagementDTO managementDTO = new ManagementDTO(id, isAttendance, false);
        boolean isUpdated = managementService.updateAttendance(managementDTO);

        if (isUpdated) {
            return ResponseEntity.ok( id + ": " +" 출석 업데이트");
        } else {
            return ResponseEntity.status(404).body(id + ": "+  " 출석 업데이트 실패");
        }
    }

    // 과제 여부 업데이트
    @PostMapping("updateTask")
    public ResponseEntity<String> updateTask(@RequestParam int id, @RequestParam boolean isTask) {
        ManagementDTO managementDTO = new ManagementDTO(id, false, isTask);
        boolean isUpdated = managementService.updateTask(managementDTO);

        if (isUpdated) {
            return ResponseEntity.ok(id +": " +  "과제 업데이트");
        } else {
            return ResponseEntity.status(404).body(id + ": " + "과제 업데이트 실패");
        }
    }

    // 출석, 과제 랭킹
    @GetMapping("/ranking")
    public ResponseEntity<List<String>> getRanking() {
        List<ManagementDTO> list = managementService.getAllMemberStat();
        return ResponseEntity.ok(managementService.getRanking(list, memberService));
    }
}