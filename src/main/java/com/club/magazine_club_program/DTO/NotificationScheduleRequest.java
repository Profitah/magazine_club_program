package com.club.magazine_club_program.DTO;

import java.util.List;
import java.util.Map;

public class NotificationScheduleRequest {
    private String sendAt;
    private String title;
    private String body;
    private String type;
    private Map<String, Object> metadata;
    private List<NotificationRecipientDTO> recipients;

    public NotificationScheduleRequest() {
    }

    public String getSendAt() {
        return sendAt;
    }

    public void setSendAt(String sendAt) {
        this.sendAt = sendAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<NotificationRecipientDTO> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<NotificationRecipientDTO> recipients) {
        this.recipients = recipients;
    }
}
