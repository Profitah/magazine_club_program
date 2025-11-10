package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.AssignmentReminderRequest;
import com.club.magazine_club_program.DTO.ChatNotificationResponse;
import com.club.magazine_club_program.DTO.NotificationScheduleRequest;
import com.club.magazine_club_program.Service.ChatNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final ChatNotificationService chatNotificationService;

    public NotificationController(ChatNotificationService chatNotificationService) {
        this.chatNotificationService = chatNotificationService;
    }

    @PostMapping("/assignments/reminders")
    public ResponseEntity<ChatNotificationResponse> createAssignmentReminder(@RequestBody AssignmentReminderRequest request) {
        ChatNotificationResponse response = chatNotificationService.sendAssignmentReminder(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.ACCEPTED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/schedule")
    public ResponseEntity<ChatNotificationResponse> scheduleNotification(@RequestBody NotificationScheduleRequest request) {
        ChatNotificationResponse response = chatNotificationService.scheduleNotification(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.ACCEPTED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
}
