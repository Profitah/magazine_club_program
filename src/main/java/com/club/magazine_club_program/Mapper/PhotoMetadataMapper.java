package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.PhotoMetadataDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PhotoMetadataMapper {

    public PhotoMetadataDTO toSuccess(LocalDateTime capturedAt, String location) {
        PhotoMetadataDTO dto = new PhotoMetadataDTO();
        dto.setSuccess(true);
        dto.setMessage(null);
        dto.setCapturedAt(capturedAt);
        dto.setLocation(location != null ? location : "입력된 위치가 없습니다.");
        return dto;
    }

    public PhotoMetadataDTO toFailure(String message) {
        PhotoMetadataDTO dto = new PhotoMetadataDTO();
        dto.setSuccess(false);
        dto.setMessage(message);
        dto.setCapturedAt(null);
        dto.setLocation("입력된 위치가 없습니다.");
        return dto;
    }
}


