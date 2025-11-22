package com.club.magazine_club_program.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChatServerService {
    private static final Logger log = LoggerFactory.getLogger(ChatServerService.class);
    private final RestTemplate restTemplate;
    
    @Value("${chat.server.base-url}")
    private String chatServerUrl;

    public ChatServerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 채팅 서버에서 차단된 사용자 목록 조회
     */
    public List<Map<String, Object>> getBlockedUsers() {
        try {
            String url = chatServerUrl + "/api/profanity/blocked-users";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Boolean success = (Boolean) body.get("success");
                if (success != null && success) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> blockedUsers = (List<Map<String, Object>>) body.get("blockedUsers");
                    log.info("채팅 서버에서 차단된 사용자 조회 성공: {}명", blockedUsers != null ? blockedUsers.size() : 0);
                    return blockedUsers != null ? blockedUsers : new ArrayList<>();
                }
            }
            
            log.warn("채팅 서버에서 차단된 사용자 조회 실패: 응답이 올바르지 않음");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("채팅 서버에서 차단된 사용자 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 채팅 서버에서 특정 사용자의 위반 기록 조회
     */
    public Map<String, Object> getUserViolationHistory(int userId, String userType) {
        try {
            String url = chatServerUrl + "/api/profanity/violations/" + userId + "/" + userType;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Boolean success = (Boolean) body.get("success");
                if (success != null && success) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> violationHistory = (Map<String, Object>) body.get("violationHistory");
                    return violationHistory;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("채팅 서버에서 위반 기록 조회 실패: userId={}, userType={}, error={}", 
                    userId, userType, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 채팅 서버에서 사용자 차단 해제
     */
    public boolean unblockUser(int userId, String userType) {
        try {
            String url = chatServerUrl + "/api/profanity/unblock";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("userType", userType);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Boolean success = (Boolean) body.get("success");
                if (success != null && success) {
                    log.info("채팅 서버에서 사용자 차단 해제 성공: userId={}, userType={}", userId, userType);
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("채팅 서버에서 사용자 차단 해제 실패: userId={}, userType={}, error={}", 
                    userId, userType, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 채팅 서버에서 사용자 위반 기록 초기화
     */
    public boolean resetViolations(int userId, String userType) {
        try {
            String url = chatServerUrl + "/api/profanity/reset-violations";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("userType", userType);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Boolean success = (Boolean) body.get("success");
                if (success != null && success) {
                    log.info("채팅 서버에서 위반 기록 초기화 성공: userId={}, userType={}", userId, userType);
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("채팅 서버에서 위반 기록 초기화 실패: userId={}, userType={}, error={}", 
                    userId, userType, e.getMessage(), e);
            return false;
        }
    }
}

