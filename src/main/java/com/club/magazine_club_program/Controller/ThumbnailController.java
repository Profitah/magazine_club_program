package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.ThumbnailDTO;
import com.club.magazine_club_program.Service.ThumbnailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/instagram")
public class ThumbnailController {
    
    @Autowired
    private ThumbnailService thumbnailService;
    
    
    /**
     * 인스타그램 썸네일 이미지 조회 (페이지네이션 - 3개씩)
     */
    @GetMapping("/thumbnails/{username}/page/{page}")
    public ResponseEntity<?> getThumbnailUrlsWithPagination(
            @PathVariable String username, 
            @PathVariable int page) {
        try {
            // 파이썬 서비스 상태 확인
            if (!thumbnailService.isPythonServiceHealthy()) {
                return ResponseEntity.ok("파이썬 크롤링 서비스가 사용 불가능합니다.");
            }
            
            // 페이지 번호 유효성 검사
            if (page < 1) {
                return ResponseEntity.ok("페이지 번호는 1 이상이어야 합니다.");
            }
            
            ThumbnailDTO result = thumbnailService.getInstagramThumbnailsDefaultPagination(username, page);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok("썸네일 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 인스타그램 썸네일 이미지 조회 (커스텀 페이지네이션)
     */
    @GetMapping("/thumbnails/{username}/page/{page}/size/{pageSize}")
    public ResponseEntity<?> getThumbnailUrlsWithCustomPagination(
            @PathVariable String username, 
            @PathVariable int page,
            @PathVariable int pageSize) {
        try {
            // 파이썬 서비스 상태 확인
            if (!thumbnailService.isPythonServiceHealthy()) {
                return ResponseEntity.ok("파이썬 크롤링 서비스가 사용 불가능합니다.");
            }
            
            // 페이지 번호 및 페이지 크기 유효성 검사
            if (page < 1) {
                return ResponseEntity.ok("페이지 번호는 1 이상이어야 합니다.");
            }
            if (pageSize < 1 || pageSize > 20) {
                return ResponseEntity.ok("페이지 크기는 1-20 사이여야 합니다.");
            }
            
            ThumbnailDTO result = thumbnailService.getInstagramThumbnailsWithPagination(username, page, pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok("썸네일 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 특정 사용자 썸네일 조회 (bbolbbol)
     */
    @GetMapping("/thumbnails/{page}")
    public ResponseEntity<?> getThumbnails(@PathVariable int page) {
        return getThumbnailUrlsWithPagination("bbolbbol", page);
    }
    
    /**
     * 파이썬 서비스 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<?> checkPythonServiceHealth() {
        try {
            boolean isHealthy = thumbnailService.isPythonServiceHealthy();
            if (isHealthy) {
                return ResponseEntity.ok("파이썬 크롤링 서비스가 정상 작동 중입니다.");
            } else {
                return ResponseEntity.ok("파이썬 크롤링 서비스가 사용 불가능합니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("서비스 상태 확인 실패: " + e.getMessage());
        }
    }
}