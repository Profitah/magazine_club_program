package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.ManagementDTO;
import com.club.magazine_club_program.Mapper.ManagementMapper;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ManagementService {
    private final ManagementMapper managementMapper;

    public ManagementService(ManagementMapper managementMapper) {
        this.managementMapper = managementMapper;
    }

    // 모든 멤버 상태 조회
    public List<ManagementDTO> getAllMemberStat() {
        return managementMapper.findAll();
    }

    // 출석 상태 업데이트
    public boolean updateAttendance(ManagementDTO managementDTO) {
        int updatedRows = managementMapper.updateAttendance(managementDTO.getId(), managementDTO.isAttendance());
        return updatedRows > 0;
    }

    // 과제 상태 업데이트
    public boolean updateTask(ManagementDTO managementDTO) {
        int updatedRows = managementMapper.updateTask(managementDTO.getId(), managementDTO.isTask());
        return updatedRows > 0;
    }

    // 출석 및 과제 랭킹 계산
    public List<String> getRanking(List<ManagementDTO> managementList, MemberService memberService) {
        Map<Integer, Integer> attendanceMap = new HashMap<>();
        Map<Integer, Integer> taskMap = new HashMap<>();

        // 출석 및 과제 횟수 집계
        for (ManagementDTO dto : managementList) {
            int id = dto.getId();
            attendanceMap.put(id, attendanceMap.getOrDefault(id, 0) + (dto.isAttendance() ? 1 : 0));
            taskMap.put(id, taskMap.getOrDefault(id, 0) + (dto.isTask() ? 1 : 0));
        }

        Set<Integer> allIds = new HashSet<>(attendanceMap.keySet());
        allIds.addAll(taskMap.keySet());

        // 정렬: 출석 순 -> 과제 순
        List<Integer> sortedIds = new ArrayList<>(allIds);
        sortedIds.sort(Comparator
                .comparing((Integer id) -> attendanceMap.getOrDefault(id, 0)).reversed()
                .thenComparing(id -> taskMap.getOrDefault(id, 0), Comparator.reverseOrder()));

        List<String> result = new ArrayList<>();
        for (int id : sortedIds) {
            int attendance = attendanceMap.getOrDefault(id, 0);
            int task = taskMap.getOrDefault(id, 0);
            result.add(String.format("ID: %d |  %d |  %d", id, attendance, task));
        }

        return result;
    }
}