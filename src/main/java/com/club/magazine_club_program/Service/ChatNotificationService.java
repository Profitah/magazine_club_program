package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.AssignmentReminderRequest;
import com.club.magazine_club_program.DTO.ChatNotificationResponse;
import com.club.magazine_club_program.DTO.NotificationScheduleRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ChatNotificationService {

    private final RestTemplate restTemplate;
    private final String chatServerBaseUrl;

    public ChatNotificationService(RestTemplate restTemplate,
                                   @Value("${chat.server.base-url}") String chatServerBaseUrl) {
        this.restTemplate = restTemplate;
        this.chatServerBaseUrl = chatServerBaseUrl;
    }

    public ChatNotificationResponse sendAssignmentReminder(AssignmentReminderRequest request) {
        String url = chatServerBaseUrl + "/api/reminders/assignments/reminders";
        return postForNotification(url, request);
    }

    public ChatNotificationResponse scheduleNotification(NotificationScheduleRequest request) {
        String url = chatServerBaseUrl + "/api/reminders/schedule";
        return postForNotification(url, request);
    }

    private ChatNotificationResponse postForNotification(String url, Object payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> body = response.getBody();
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (body != null && body.containsKey("success")) {
                success = Boolean.TRUE.equals(body.get("success"));
            }
            return new ChatNotificationResponse(success, null, body);
        } catch (HttpStatusCodeException ex) {
            String message = ex.getResponseBodyAsString();
            return new ChatNotificationResponse(false, message, null);
        }
    }
}
