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

    @Autowired(required = false)
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    /**
     * 바이트 배열을 S3에 업로드
     * @param imageBytes 이미지 바이트 배열
     * @param contentType 컨텐츠 타입 (예: "image/jpeg")
     * @param folder 폴더 경로 (예: "instagram", "photos")
     * @param filename 파일명 (null이면 UUID 생성)
     * @return S3 URL
     */
    public String uploadImageFromBytes(byte[] imageBytes, String contentType, String folder, String filename) {
        try {
            if (s3Client == null || bucketName == null || bucketName.isEmpty()) {
                throw new RuntimeException("S3 클라이언트가 초기화되지 않았습니다. AWS 환경 변수를 확인하세요.");
            }
            
            if (filename == null || filename.isEmpty()) {
                filename = UUID.randomUUID().toString() + ".jpg";
            }

            String key = folder + "/" + filename;

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
     * 이미지를 S3에 업로드
     * @param inputStream 이미지 입력 스트림
     * @param contentType 컨텐츠 타입 (예: "image/jpeg")
     * @param folder 폴더 경로 (예: "instagram", "photos")
     * @param filename 파일명 (null이면 UUID 생성)
     * @return S3 URL
     */
    public String uploadImage(InputStream inputStream, String contentType, String folder, String filename) {
        try {
            if (s3Client == null || bucketName == null || bucketName.isEmpty()) {
                throw new RuntimeException("S3 클라이언트가 초기화되지 않았습니다. AWS 환경 변수를 확인하세요.");
            }
            
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
            // URL 정리: HTML 엔티티 디코딩 및 정리
            String cleanUrl = imageUrl
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .trim();
            
            URL url = new URL(cleanUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            
            // User-Agent와 Referer 헤더 추가 (인스타그램 이미지 접근을 위해)
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            if (cleanUrl.contains("instagram.com") || cleanUrl.contains("cdninstagram.com")) {
                connection.setRequestProperty("Referer", "https://www.instagram.com/");
                connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            }
            
            connection.setConnectTimeout(15000); // 15초 타임아웃
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true); // 리다이렉트 따라가기
            
            try (InputStream inputStream = connection.getInputStream()) {
                // HTTP 응답 코드 확인
                int responseCode = connection.getResponseCode();
                if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    throw new RuntimeException("HTTP " + responseCode + " 오류: " + connection.getResponseMessage());
                }
                
                String contentType = connection.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    // Content-Type이 없거나 이미지가 아니면 URL에서 추론
                    if (cleanUrl.contains(".png")) {
                        contentType = "image/png";
                    } else if (cleanUrl.contains(".gif")) {
                        contentType = "image/gif";
                    } else if (cleanUrl.contains(".webp")) {
                        contentType = "image/webp";
                    } else {
                        contentType = "image/jpeg"; // 기본값
                    }
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
