package com.club.magazine_club_program.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * OAuth2 토큰 암호화/복호화 유틸리티 (AES-256-GCM)
 * 
 * 환경 변수 필요:
 * - TOKEN_ENCRYPTION_KEY: 32바이트 (256비트) Base64 인코딩된 키
 * 
 * 키 생성 방법:
 * KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
 * keyGenerator.init(256);
 * SecretKey key = keyGenerator.generateKey();
 * String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
 */
@Component
public class TokenEncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(TokenEncryptionUtil.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96비트 IV
    private static final int GCM_TAG_LENGTH = 16; // 128비트 태그
    private static final String AES = "AES";

    private final SecretKey secretKey;

    public TokenEncryptionUtil(@Value("${TOKEN_ENCRYPTION_KEY:}") String encryptionKey) {
        SecretKey key;
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            log.warn("TOKEN_ENCRYPTION_KEY 환경 변수가 설정되지 않았습니다. " +
                    "임시 키를 생성하여 사용합니다. 프로덕션 환경에서는 반드시 설정해주세요.");
            // 임시 키 자동 생성 (서버 재시작 시마다 변경됨)
            key = generateTemporaryKey();
            log.warn("임시 암호화 키로 초기화되었습니다. OAuth2 토큰이 서버 재시작 시 유효하지 않을 수 있습니다.");
        } else {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("암호화 키는 32바이트(256비트)여야 합니다.");
            }
                key = new SecretKeySpec(keyBytes, AES);
            log.info("토큰 암호화 유틸리티 초기화 완료");
            } catch (Exception e) {
                log.error("암호화 키 초기화 실패, 임시 키로 대체합니다: " + e.getMessage());
                key = generateTemporaryKey();
            }
        }
        this.secretKey = key;
    }
    
    /**
     * 임시 암호화 키 생성 (서버 시작 시 자동 생성)
     */
    private SecretKey generateTemporaryKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("임시 키 생성 실패", e);
        }
    }

    /**
     * 토큰 암호화 (AES-256-GCM)
     * 
     * @param plaintext 암호화할 평문 토큰
     * @return Base64 인코딩된 암호문 (IV + 암호문)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            
            // IV 생성 (96비트)
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            // 암호화
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // IV + 암호문 결합 후 Base64 인코딩
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, encryptedWithIv, GCM_IV_LENGTH, ciphertext.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            log.error("토큰 암호화 실패", e);
            throw new RuntimeException("토큰 암호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 토큰 복호화 (AES-256-GCM)
     * 
     * @param encryptedText Base64 인코딩된 암호문 (IV + 암호문)
     * @return 복호화된 평문 토큰
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // Base64 디코딩
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
            
            if (encryptedWithIv.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("암호문 길이가 너무 짧습니다.");
            }
            
            // IV 추출
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            
            // 암호문 추출
            byte[] ciphertext = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // 복호화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("토큰 복호화 실패", e);
            throw new RuntimeException("토큰 복호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 암호화 키 생성 헬퍼 메서드 (개발/테스트용)
     * 프로덕션에서는 별도로 키를 생성하고 안전하게 보관해야 합니다.
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
            keyGenerator.init(256);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("키 생성 실패", e);
        }
    }
}

