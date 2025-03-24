package com.club.magazine_club_program.DTO;

public class ManagementDTO {
    private final Integer id;
    private Boolean isAttendance;
    private Boolean isTask;
    private String name;

    public ManagementDTO(int id, boolean isAttendance, boolean isTask) {
        this.id = id;
        this.isAttendance = isAttendance;
        this.isTask = isTask;
    }

    public int getId() {
        return id;
    }

    public boolean isAttendance() {
        return isAttendance;
    }
    public void setAttendance(boolean attendance) {
        this.isAttendance = attendance;
    }

    public boolean isTask() {
        return isTask;
    }

    public void setTask(boolean task) {
        this.isTask = task;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}