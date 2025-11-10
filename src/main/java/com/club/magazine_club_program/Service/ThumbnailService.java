package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.ThumbnailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ThumbnailService {
    
    @Autowired
    private RestTemplate restTemplate;
    @Value("${python.service.url}")
    private String pythonServiceUrl;
    private static final int DEFAULT_PAGE_SIZE = 3;
    
    @Value("${instagram.target.username}")
    private String targetInstagramUsername;
    
    @Value("${instagram.source.username}")
    private String sourceInstagramUsername;
    
    /**
     * 파이썬 크롤링 서비스에서 인스타그램 썸네일 URL 조회
     */
    public ThumbnailDTO getInstagramThumbnails(String username) {
        try {
            String actualUsername = convertUsername(username);
            String url = pythonServiceUrl + "/instagram/thumbnails/urls/" + actualUsername;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                @SuppressWarnings("unchecked")
                List<String> thumbnailUrls = (List<String>) responseBody.get("thumbnail_urls");
                Integer count = (Integer) responseBody.get("count");
                Boolean success = (Boolean) responseBody.get("success");
                
                ThumbnailDTO thumbnailDTO = new ThumbnailDTO();
                thumbnailDTO.setUsername(username);
                thumbnailDTO.setThumbnailUrls(thumbnailUrls != null ? thumbnailUrls : new ArrayList<>());
                thumbnailDTO.setTotalCount(count != null ? count : 0);
                thumbnailDTO.setCrawledAt(LocalDateTime.now());
                thumbnailDTO.setSuccess(success != null ? success : false);
                thumbnailDTO.setMessage("썸네일 조회 완료");
                
                return thumbnailDTO;
            }
            
            return createErrorResponse(username, "파이썬 서비스 응답 오류");
            
        } catch (Exception e) {
            return createErrorResponse(username, "파이썬 크롤링 서비스 호출 실패: " + e.getMessage());
        }
    }
    
    /**
     * 페이지네이션을 적용한 썸네일 URL 조회
     */
    public ThumbnailDTO getInstagramThumbnailsWithPagination(String username, int page, int pageSize) {
        ThumbnailDTO thumbnailDTO = getInstagramThumbnails(username);
        
        if (!thumbnailDTO.isSuccess()) {
            return thumbnailDTO;
        }
        
        List<String> allUrls = thumbnailDTO.getThumbnailUrls();
        int totalCount = allUrls.size();
        
        // 페이지네이션 계산
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCount);
        
        // 현재 페이지의 URL 추출
        List<String> currentPageUrls = new ArrayList<>();
        if (startIndex < totalCount) {
            currentPageUrls = allUrls.subList(startIndex, endIndex);
        }
        
        // 페이지네이션 정보 설정
        thumbnailDTO.setCurrentPage(page);
        thumbnailDTO.setPageSize(pageSize);
        thumbnailDTO.setTotalPages(totalPages);
        thumbnailDTO.setCurrentPageUrls(currentPageUrls);
        
        return thumbnailDTO;
    }
    
    /**
     * 기본 페이지네이션 (3개씩)
     */
    public ThumbnailDTO getInstagramThumbnailsDefaultPagination(String username, int page) {
        return getInstagramThumbnailsWithPagination(username, page, DEFAULT_PAGE_SIZE);
    }
    
    /**
     * 파이썬 서비스 상태 확인
     */
    public boolean isPythonServiceHealthy() {
        try {
            String url = pythonServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String convertUsername(String username) {
        if (sourceInstagramUsername.equals(username)) {
            return targetInstagramUsername;
        }
        return username; 
    }
    
    /**
     * 에러 응답 생성
     */
    private ThumbnailDTO createErrorResponse(String username, String errorMessage) {
        ThumbnailDTO thumbnailDTO = new ThumbnailDTO();
        thumbnailDTO.setUsername(username);
        thumbnailDTO.setThumbnailUrls(new ArrayList<>());
        thumbnailDTO.setTotalCount(0);
        thumbnailDTO.setCrawledAt(LocalDateTime.now());
        thumbnailDTO.setSuccess(false);
        thumbnailDTO.setMessage(errorMessage);
        return thumbnailDTO;
    }
}