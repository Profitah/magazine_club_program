package com.club.magazine_club_program.Mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import com.club.magazine_club_program.DTO.ManagementDTO;

@Mapper
public interface ManagementMapper {

    // 전체 멤버 상태 조회
    @Select("SELECT id, isAttendance, isTask FROM MemberService")
    List<ManagementDTO> findAll();

    // 출석 업데이트
    @Update("UPDATE MemberService SET isAttendance = #{isAttendance} WHERE id = #{id}")
    int updateAttendance(@Param("id") int id, @Param("isAttendance") boolean isAttendance);

    // 과제 업데이트
    @Update("UPDATE MemberService SET isTask = #{isTask} WHERE id = #{id}")
    int updateTask(@Param("id") int id, @Param("isTask") boolean isTask);
}
