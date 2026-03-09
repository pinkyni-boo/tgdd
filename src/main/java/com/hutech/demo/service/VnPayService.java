package com.hutech.demo.service;

import com.hutech.demo.config.VnPayProperties;
import com.hutech.demo.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VnPayService {
    private final VnPayProperties vnPayProperties;

    public boolean isConfigured() {
        return vnPayProperties.getTmnCode() != null && !vnPayProperties.getTmnCode().isBlank()
                && vnPayProperties.getHashSecret() != null && !vnPayProperties.getHashSecret().isBlank();
    }

    public String createPaymentUrl(Order order, String clientIp) {
        return createPaymentUrl(order, clientIp, null);
    }

    public String createPaymentUrl(Order order, String clientIp, String paymentMethod) {
        if (!isConfigured()) {
            throw new IllegalStateException("VNPay credentials are not configured");
        }

        String txnRef = "OD" + order.getId() + "_" + System.currentTimeMillis();
        long amount = Math.round(order.getTotalAmount() * 100);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        String createDate = formatter.format(calendar.getTime());
        calendar.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(calendar.getTime());

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayProperties.getVersion());
        params.put("vnp_Command", vnPayProperties.getCommand());
        params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang #" + order.getId());
        params.put("vnp_OrderType", vnPayProperties.getOrderType());
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        params.put("vnp_IpAddr", clientIp == null ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);
        String bankCode = resolveBankCode(paymentMethod);
        if (bankCode != null) {
            params.put("vnp_BankCode", bankCode);
        }

        String queryUrl = buildQuery(params, true);
        String hashData = buildQuery(params, false);
        String secureHash = hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        return vnPayProperties.getPayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
    }

    public boolean verifyReturn(Map<String, String> responseParams) {
        if (!isConfigured()) return false;

        String vnpSecureHash = responseParams.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isBlank()) return false;

        Map<String, String> filtered = new HashMap<>();
        for (Map.Entry<String, String> entry : responseParams.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) continue;
            filtered.put(key, entry.getValue() == null ? "" : entry.getValue());
        }

        String hashData = buildQuery(filtered, false);
        String expectedHash = hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        return expectedHash.equalsIgnoreCase(vnpSecureHash);
    }

    private String buildQuery(Map<String, String> params, boolean encodeValue) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String key = fieldNames.get(i);
            String value = params.get(key);
            if (value == null || value.isBlank()) continue;

            if (sb.length() > 0) sb.append('&');
            sb.append(key);
            sb.append('=');
            sb.append(encodeValue ? urlEncode(value) : value);
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hash.append('0');
                hash.append(hex);
            }
            return hash.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate VNPay hash", e);
        }
    }

    private String resolveBankCode(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return null;
        }
        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "QR", "VNPAYQR" -> "VNPAYQR";
            case "CONNECT", "VNBANK", "ATM" -> "VNBANK";
            default -> null;
        };
    }
}
