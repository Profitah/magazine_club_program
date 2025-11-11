package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.AssignmentReminderRequest;
import com.club.magazine_club_program.DTO.ChatNotificationResponse;
import com.club.magazine_club_program.DTO.MessageReservationRequest;
import com.club.magazine_club_program.DTO.NotificationScheduleRequest;
import com.club.magazine_club_program.Service.ChatNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Validated
public class NotificationController {

    private final ChatNotificationService chatNotificationService;

    public NotificationController(ChatNotificationService chatNotificationService) {
        this.chatNotificationService = chatNotificationService;
    }

    @PostMapping("/assignments/reminders")
    public ResponseEntity<ChatNotificationResponse> createAssignmentReminder(@Valid @RequestBody AssignmentReminderRequest request) {
        ChatNotificationResponse response = chatNotificationService.sendAssignmentReminder(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.ACCEPTED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/schedule")
    public ResponseEntity<ChatNotificationResponse> scheduleNotification(@Valid @RequestBody NotificationScheduleRequest request) {
        ChatNotificationResponse response = chatNotificationService.scheduleNotification(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.ACCEPTED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/messages/reserve")
    public ResponseEntity<ChatNotificationResponse> reserveMessage(@RequestBody MessageReservationRequest request) {
        ChatNotificationResponse response = chatNotificationService.reserveMessage(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.ACCEPTED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getNotificationLogs(@RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> response = chatNotificationService.fetchNotificationLogs(limit);
        boolean success = Boolean.TRUE.equals(response.get("success"));
        HttpStatus status = success ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/failed")
    public ResponseEntity<?> getFailedNotifications() {
        Map<String, Object> response = chatNotificationService.fetchFailedNotifications();
        boolean success = Boolean.TRUE.equals(response.get("success"));
        HttpStatus status = success ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/retry")
    public ResponseEntity<ChatNotificationResponse> retryFailedNotification(@RequestBody Map<String, String> request) {
        String jobId = request.get("jobId");
        if (jobId == null || jobId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatNotificationResponse(false, "jobId는 필수입니다.", null));
        }

        ChatNotificationResponse response = chatNotificationService.retryNotification(jobId);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
}
