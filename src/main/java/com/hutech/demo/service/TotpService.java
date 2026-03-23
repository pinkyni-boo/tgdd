package com.hutech.demo.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class TotpService {

    private static final String ISSUER = "Hutech Demo";
    private final GoogleAuthenticator googleAuthenticator;

    public TotpService() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(6)
                .setTimeStepSizeInMillis(30_000)
                .setWindowSize(3)
                .build();
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }

    public String generateSecret() {
        return googleAuthenticator.createCredentials().getKey();
    }

    public String buildOtpAuthUrl(String username, String secret) {
        String account = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        return "otpauth://totp/" + issuer + ":" + account +
                "?secret=" + secret +
                "&issuer=" + issuer +
                "&algorithm=SHA1&digits=6&period=30";
    }

    public String buildQrCodeDataUri(String otpAuthUrl) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(otpAuthUrl, BarcodeFormat.QR_CODE, 260, 260);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | java.io.IOException e) {
            throw new IllegalStateException("Cannot generate QR code", e);
        }
    }

    public boolean verifyCode(String secret, String otpCode) {
        if (secret == null || secret.isBlank() || otpCode == null || otpCode.isBlank()) {
            return false;
        }
        String cleaned = otpCode.trim().replaceAll("\\s+", "");
        if (!cleaned.matches("\\d{6}")) {
            return false;
        }
        int code = Integer.parseInt(cleaned);
        return googleAuthenticator.authorize(secret, code);
    }
}
