package com.club.magazine_club_program.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageReservationRequest {

    @NotNull(message = "fromUserId는 필수입니다.")
    private Integer fromUserId;

    @NotBlank(message = "fromUserType는 필수입니다.")
    private String fromUserType;

    @NotNull(message = "toUserId는 필수입니다.")
    private Integer toUserId;

    @NotBlank(message = "toUserType는 필수입니다.")
    private String toUserType;

    @NotBlank(message = "message는 필수입니다.")
    private String message;

    @Pattern(regexp = "^$|^[0-9T:+-]+$", message = "sendAt 형식이 올바르지 않습니다.")
    private String sendAt;

    @Positive(message = "delaySeconds는 0보다 큰 값이어야 합니다.")
    private Long delaySeconds;

    private Map<String, Object> metadata;

    public Integer getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Integer fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserType() {
        return fromUserType;
    }

    public void setFromUserType(String fromUserType) {
        this.fromUserType = fromUserType;
    }

    public Integer getToUserId() {
        return toUserId;
    }

    public void setToUserId(Integer toUserId) {
        this.toUserId = toUserId;
    }

    public String getToUserType() {
        return toUserType;
    }

    public void setToUserType(String toUserType) {
        this.toUserType = toUserType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSendAt() {
        return sendAt;
    }

    public void setSendAt(String sendAt) {
        this.sendAt = sendAt;
    }

    public Long getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Long delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

