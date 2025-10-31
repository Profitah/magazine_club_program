package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.PhotoMetadataDTO;
import com.club.magazine_club_program.Mapper.PhotoMetadataMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

public class PhotoMetadataServiceTest {

    @Test
    public void emptyFile_returnsFailure() {
        PhotoMetadataService service = new PhotoMetadataService(new PhotoMetadataMapper());
        MockMultipartFile file = new MockMultipartFile("file", new byte[]{});

        PhotoMetadataDTO dto = service.extractMetadata(file);

        Assertions.assertFalse(dto.isSuccess());
        Assertions.assertNull(dto.getCapturedAt());
        // 위도/경도는 더 이상 노출하지 않음
    }
}


