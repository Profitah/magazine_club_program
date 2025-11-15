package com.club.magazine_club_program.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * 이미지를 S3에 업로드
     * @param inputStream 이미지 입력 스트림
     * @param contentType 컨텐츠 타입 (예: "image/jpeg")
     * @param folder 폴더 경로 (예: "instagram", "photos")
     * @param filename 파일명 (null이면 UUID 생성)
     * @return S3 URL
     */
    public String uploadImage(InputStream inputStream, String contentType, String folder, String filename) {
        try {
            if (filename == null || filename.isEmpty()) {
                filename = UUID.randomUUID().toString() + ".jpg";
            }

            String key = folder + "/" + filename;

            // InputStream을 byte 배열로 변환
            byte[] imageBytes = inputStream.readAllBytes();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

            // S3 URL 생성
            return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(key)).toString();

        } catch (S3Exception e) {
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * URL에서 이미지를 다운로드하여 S3에 업로드
     * @param imageUrl 이미지 URL
     * @param folder 폴더 경로
     * @param filename 파일명
     * @return S3 URL
     */
    public String downloadAndUpload(String imageUrl, String folder, String filename) {
        try {
            URL url = new URL(imageUrl);
            try (InputStream inputStream = url.openStream()) {
                String contentType = "image/jpeg"; // 기본값
                if (imageUrl.contains(".png")) {
                    contentType = "image/png";
                } else if (imageUrl.contains(".gif")) {
                    contentType = "image/gif";
                }
                return uploadImage(inputStream, contentType, folder, filename);
            }
        } catch (Exception e) {
            throw new RuntimeException("이미지 다운로드 및 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 인스타그램 이미지를 S3에 저장
     * @param instagramUrl 인스타그램 이미지 URL
     * @param username 인스타그램 사용자명
     * @param index 이미지 인덱스
     * @return S3 URL
     */
    public String uploadInstagramImage(String instagramUrl, String username, int index) {
        String filename = username + "_" + Instant.now().toEpochMilli() + "_" + index + ".jpg";
        return downloadAndUpload(instagramUrl, "instagram", filename);
    }
}
