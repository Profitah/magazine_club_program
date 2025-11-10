package com.club.magazine_club_program.DTO;

import java.util.List;

public class AssignmentReminderRequest {
    private String assignmentId;
    private String assignmentTitle;
    private String dueDate;
    private String message;
    private String requestedBy;
    private List<NotificationRecipientDTO> recipients;

    public AssignmentReminderRequest() {
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public List<NotificationRecipientDTO> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<NotificationRecipientDTO> recipients) {
        this.recipients = recipients;
    }
}
