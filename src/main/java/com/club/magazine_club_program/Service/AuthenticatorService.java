package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.SecretAndQrCodeDTO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthenticatorService {

    private final GoogleAuthenticator googleAuthenticator;

    @Autowired
    public AuthenticatorService(GoogleAuthenticator googleAuthenticator) {
        this.googleAuthenticator = googleAuthenticator;
    }

    /**
     * TOTP Secret 생성 및 QR 코드 생성
     * @param email 관리자 이메일 (식별자로 사용)
     * @param issuer 서비스명 (예: "Magazine Club")
     * @return Secret과 QR 코드를 포함한 DTO
     */
    public SecretAndQrCodeDTO generateSecretAndQrCode(String email, String issuer) {
        // TOTP Secret 생성
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials(email);
        String secret = key.getKey();

        // QR 코드 URL 생성 (otpauth:// 형식)
        String otpAuthUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, email, key);

        // QR 코드 이미지 생성 및 Base64 인코딩
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUrl, BarcodeFormat.QR_CODE, 300, 300, hints);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            String qrCodeBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
            return new SecretAndQrCodeDTO(secret, qrCodeBase64);
        } catch (WriterException | IOException e) {
            throw new RuntimeException("QR 코드 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * TOTP Secret만 생성 (QR 코드 없이)
     */
    public String generateSecret(String email) {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials(email);
        return key.getKey();
    }

    /**
     * TOTP 코드 검증
     * @param secret TOTP Secret
     * @param code Authenticator 앱에서 생성된 6자리 코드
     * @return 검증 성공 여부
     */
    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    /**
     * TOTP 코드 검증 (문자열 코드 입력 지원)
     */
    public boolean verifyCode(String secret, String code) {
        try {
            int codeInt = Integer.parseInt(code);
            return verifyCode(secret, codeInt);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

