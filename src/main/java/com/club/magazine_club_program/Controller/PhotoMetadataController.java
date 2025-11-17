package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.PhotoMetadataDTO;
import com.club.magazine_club_program.Service.PhotoMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/photos")
public class PhotoMetadataController {

    private final PhotoMetadataService service;

    public PhotoMetadataController(PhotoMetadataService service) {
        this.service = service;
    }

    /**
     * 사진 메타데이터(EXIF)를 검증 및 추출하여 촬영일시와 위치를 반환
     * 과제 제목/소감(한 줄 설명)도 함께 입력받아 응답에 포함
     */
    @PostMapping("/metadata")
    public ResponseEntity<?> extract(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "reflection", required = false) String reflection) {
        try {
            PhotoMetadataDTO dto = service.extractMetadata(file);
            // 메타데이터 검증 로직에는 영향을 주지 않고, 추가 정보만 세팅
            dto.setTitle(title);
            dto.setReflection(reflection);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.ok("메타데이터 추출 실패. 관리자에게 문의해주세요.");
        }
    }
}


