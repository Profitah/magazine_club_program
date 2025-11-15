package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.PhotoMetadataDTO;
import com.club.magazine_club_program.Mapper.PhotoMetadataMapper;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class PhotoMetadataService {

    private final PhotoMetadataMapper mapper;
    private final ReverseGeocodingService reverseGeocodingService;
    private S3Service s3Service;

    public PhotoMetadataService(PhotoMetadataMapper mapper) {
        this.mapper = mapper;
        this.reverseGeocodingService = null;
    }

    @Autowired
    public PhotoMetadataService(PhotoMetadataMapper mapper, ReverseGeocodingService reverseGeocodingService) {
        this.mapper = mapper;
        this.reverseGeocodingService = reverseGeocodingService;
    }

    @Autowired(required = false)
    public void setS3Service(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public PhotoMetadataDTO extractMetadata(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return mapper.toFailure("파일이 비어있습니다.");
        }

        try (InputStream is = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);

            LocalDateTime capturedAt = null;
            Double latitude = null;
            Double longitude = null;

            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exif != null) {
                Date original = exif.getDateOriginal();
                if (original != null) {
                    capturedAt = LocalDateTime.ofInstant(original.toInstant(), ZoneId.systemDefault());
                }
            }

            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                latitude = gpsDir.getGeoLocation().getLatitude();
                longitude = gpsDir.getGeoLocation().getLongitude();
            }

            if (capturedAt == null && latitude == null && longitude == null) {
                return mapper.toFailure("유효한 사진 메타데이터(EXIF)를 찾을 수 없습니다.");
            }

            String location = null;
            if (latitude != null && longitude != null && reverseGeocodingService != null) {
                location = reverseGeocodingService.reverseGeocodeCountryRegion(latitude, longitude);
            }

            PhotoMetadataDTO dto = mapper.toSuccess(capturedAt, location);
            
            // S3에 이미지 저장
            if (s3Service != null) {
                try {
                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        contentType = "image/jpeg";
                    }
                    String filename = file.getOriginalFilename();
                    if (filename == null || filename.isEmpty()) {
                        filename = "photo_" + System.currentTimeMillis() + ".jpg";
                    }
                    String s3Url = s3Service.uploadImage(file.getInputStream(), contentType, "photos", filename);
                    dto.setImageUrl(s3Url);
                } catch (Exception e) {
                    // S3 업로드 실패해도 메타데이터는 반환
                    System.err.println("S3 업로드 실패: " + e.getMessage());
                }
            }

            return dto;
        } catch (Exception e) {
            return mapper.toFailure("메타데이터 읽기 실패: " + e.getMessage());
        }
    }
}


