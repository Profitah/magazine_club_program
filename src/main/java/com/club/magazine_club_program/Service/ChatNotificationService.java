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

import java.util.Collections;
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

    public Map<String, Object> fetchNotificationLogs(int limit) {
        String url = chatServerBaseUrl + "/api/notifications/logs?limit=" + limit;
        return getForNotification(url);
    }

    public Map<String, Object> fetchFailedNotifications() {
        String url = chatServerBaseUrl + "/api/notifications/failed";
        return getForNotification(url);
    }

    public ChatNotificationResponse retryNotification(String jobId) {
        String url = chatServerBaseUrl + "/api/notifications/retry";
        return postForNotification(url, Collections.singletonMap("jobId", jobId));
    }

    private Map<String, Object> getForNotification(String url) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            return Map.of(
                    "success", false,
                    "message", ex.getResponseBodyAsString()
            );
        }
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
