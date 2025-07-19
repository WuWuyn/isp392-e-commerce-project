package com.example.isp392.service;

import com.example.isp392.config.PaymentConfig;
import com.example.isp392.model.CustomerOrder;
import com.example.isp392.model.PaymentReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VNPayService {
    private final PaymentConfig paymentConfig;
    private final RestTemplate restTemplate = new RestTemplate();



    public String createPaymentUrl(CustomerOrder customerOrder) {
        // Validate input
        if (customerOrder == null) {
            throw new IllegalArgumentException("CustomerOrder cannot be null");
        }
        if (customerOrder.getCustomerOrderId() == null) {
            throw new IllegalArgumentException("CustomerOrder ID cannot be null");
        }
        if (customerOrder.getTotalAmount() == null || customerOrder.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("CustomerOrder total amount must be greater than 0");
        }

        // Validate configuration
        if (paymentConfig.getVnpayTerminalId() == null || paymentConfig.getVnpayTerminalId().trim().isEmpty()) {
            throw new IllegalStateException("VNPay Terminal ID is not configured");
        }
        if (paymentConfig.getVnpaySecretKey() == null || paymentConfig.getVnpaySecretKey().trim().isEmpty()) {
            throw new IllegalStateException("VNPay Secret Key is not configured");
        }
        if (paymentConfig.getVnpayPaymentUrl() == null || paymentConfig.getVnpayPaymentUrl().trim().isEmpty()) {
            throw new IllegalStateException("VNPay Payment URL is not configured");
        }
        if (paymentConfig.getReturnUrl() == null || paymentConfig.getReturnUrl().trim().isEmpty()) {
            throw new IllegalStateException("Return URL is not configured");
        }

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = String.valueOf(customerOrder.getCustomerOrderId());
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = paymentConfig.getVnpayTerminalId();
        String orderType = "other";
        String vnp_OrderInfo = "Thanh toan don hang " + vnp_TxnRef; // Remove colon
        String vnp_OrderType = orderType;
        String vnp_Amount = String.valueOf(customerOrder.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue());
        String vnp_ReturnUrl = paymentConfig.getReturnUrl();
        String vnp_CurrCode = "VND";

        // Create parameters map with all required fields in exact VNPay format
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", vnp_Amount);
        vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Add timestamp fields
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Set expiration to 1 minute for testing (change back to 10 for production)
        cld.add(Calendar.MINUTE, 1);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Log parameters for debugging
        System.out.println("=== VNPay Parameters before signing ===");
        System.out.println("Terminal ID: " + vnp_TmnCode);
        System.out.println("Secret Key: " + paymentConfig.getVnpaySecretKey());
        System.out.println("Payment URL: " + paymentConfig.getVnpayPaymentUrl());
        System.out.println("Return URL: " + vnp_ReturnUrl);
        System.out.println("Amount: " + vnp_Amount);
        System.out.println("TxnRef: " + vnp_TxnRef);
        System.out.println("All parameters:");
        vnp_Params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> System.out.println("  " + entry.getKey() + "=" + entry.getValue()));
        System.out.println("=== End Parameters ===");

        // Build hash data and query string according to VNPay specification
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data (WITH URL encoding using US_ASCII)
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(urlEncodeUSASCII(fieldValue));

                // Build query string (ALSO with URL encoding using US_ASCII - same as hash data!)
                query.append(urlEncodeUSASCII(fieldName));
                query.append('=');
                query.append(urlEncodeUSASCII(fieldValue));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        System.out.println("Hash data string: " + hashData.toString());
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(paymentConfig.getVnpaySecretKey(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        String finalUrl = paymentConfig.getVnpayPaymentUrl() + "?" + queryUrl;

        System.out.println("Generated secure hash: " + vnp_SecureHash);
        System.out.println("VNPayService: Generated final payment URL: " + finalUrl);
        System.out.println("VNPayService: URL length: " + finalUrl.length());

        return finalUrl;
    }

    public boolean validatePaymentResponse(Map<String, String> response) {
        System.out.println("VNPay Response Validation - Received parameters:");
        response.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> System.out.println(entry.getKey() + "=" + entry.getValue()));

        String vnp_SecureHash = response.get("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            System.out.println("VNPay Response Validation - No secure hash found in response");
            return false;
        }

        // Create a copy to avoid modifying the original
        Map<String, String> params = new HashMap<>(response);
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data with US_ASCII encoding for validation
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(urlEncodeUSASCII(fieldValue));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        System.out.println("VNPay Response Validation - Hash data string: " + hashData.toString());
        String calculatedHash = hmacSHA512(paymentConfig.getVnpaySecretKey(), hashData.toString());
        System.out.println("VNPay Response Validation - Calculated hash: " + calculatedHash);
        System.out.println("VNPay Response Validation - Received hash: " + vnp_SecureHash);

        boolean isValid = calculatedHash.equals(vnp_SecureHash);
        System.out.println("VNPay Response Validation - Result: " + isValid);

        return isValid;
    }

    public Map<String, String> queryTransactionStatus(String orderId) {
        String vnp_RequestId = String.valueOf(System.currentTimeMillis());
        String vnp_Version = "2.1.0";
        String vnp_Command = "querydr";
        String vnp_TmnCode = paymentConfig.getVnpayTerminalId();
        String vnp_TxnRef = orderId;
        String vnp_OrderInfo = "Kiem tra trang thai GD:" + vnp_TxnRef;
        String vnp_TransDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_RequestId", vnp_RequestId);
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_TransDate", vnp_TransDate);

        String hashData = String.join("|", vnp_RequestId, vnp_Version, vnp_Command, vnp_TmnCode, vnp_TxnRef, vnp_TransDate);
        String vnp_SecureHash = hmacSHA512(paymentConfig.getVnpaySecretKey(), hashData);

        vnp_Params.put("vnp_SecureHash", vnp_SecureHash);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(vnp_Params, headers);

        // Use ParameterizedTypeReference to avoid unchecked operation warning
        ParameterizedTypeReference<Map<String, String>> responseType =
            new ParameterizedTypeReference<Map<String, String>>() {};

        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
            paymentConfig.getVnpayApiUrl(),
            HttpMethod.POST,
            request,
            responseType
        );

        return response.getBody();
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            sha512_HMAC.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = sha512_HMAC.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }

    private String urlEncodeUSASCII(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * Create payment URL for VNPay from PaymentReservation
     */
    public String createPaymentUrlFromReservation(PaymentReservation paymentReservation) {
        // Validate input
        if (paymentReservation == null) {
            throw new IllegalArgumentException("PaymentReservation cannot be null");
        }
        if (paymentReservation.getVnpayTxnRef() == null) {
            throw new IllegalArgumentException("PaymentReservation TxnRef cannot be null");
        }
        if (paymentReservation.getTotalAmount() == null || paymentReservation.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("PaymentReservation total amount must be greater than 0");
        }

        // Validate configuration
        if (paymentConfig.getVnpayTerminalId() == null || paymentConfig.getVnpayTerminalId().trim().isEmpty()) {
            throw new IllegalStateException("VNPay Terminal ID is not configured");
        }
        if (paymentConfig.getVnpaySecretKey() == null || paymentConfig.getVnpaySecretKey().trim().isEmpty()) {
            throw new IllegalStateException("VNPay Secret Key is not configured");
        }
        if (paymentConfig.getVnpayPaymentUrl() == null || paymentConfig.getVnpayPaymentUrl().trim().isEmpty()) {
            throw new IllegalStateException("VNPay Payment URL is not configured");
        }
        if (paymentConfig.getReturnUrl() == null || paymentConfig.getReturnUrl().trim().isEmpty()) {
            throw new IllegalStateException("Return URL is not configured");
        }

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = paymentReservation.getVnpayTxnRef();
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = paymentConfig.getVnpayTerminalId();
        String orderType = "other";
        String vnp_OrderInfo = "Thanh toan don hang " + paymentReservation.getReservationId();
        String vnp_OrderType = orderType;
        String vnp_Amount = String.valueOf(paymentReservation.getTotalAmount().multiply(new BigDecimal(100)).longValue());
        String vnp_ReturnUrl = paymentConfig.getReturnUrl();
        String vnp_CurrCode = "VND";

        // Create parameters map with all required fields in exact VNPay format
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", vnp_Amount);
        vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Add timestamp fields
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Set expiration to 15 minutes for VNPay
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Log parameters for debugging
        System.out.println("=== VNPay Parameters for PaymentReservation ===");
        System.out.println("Terminal ID: " + vnp_TmnCode);
        System.out.println("Secret Key: " + paymentConfig.getVnpaySecretKey());
        System.out.println("Payment URL: " + paymentConfig.getVnpayPaymentUrl());
        System.out.println("Return URL: " + vnp_ReturnUrl);
        System.out.println("Amount: " + vnp_Amount);
        System.out.println("TxnRef: " + vnp_TxnRef);
        System.out.println("All parameters:");
        vnp_Params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> System.out.println("  " + entry.getKey() + "=" + entry.getValue()));
        System.out.println("=== End Parameters ===");

        // Build hash data and query string according to VNPay specification
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data (WITH URL encoding using US_ASCII)
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(urlEncodeUSASCII(fieldValue));

                // Build query string (ALSO with URL encoding using US_ASCII - same as hash data!)
                query.append(urlEncodeUSASCII(fieldName));
                query.append('=');
                query.append(urlEncodeUSASCII(fieldValue));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        System.out.println("Hash data string: " + hashData.toString());

        String vnp_SecureHash = hmacSHA512(paymentConfig.getVnpaySecretKey(), hashData.toString());
        System.out.println("Generated secure hash: " + vnp_SecureHash);

        String queryUrl = query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = paymentConfig.getVnpayPaymentUrl() + "?" + queryUrl;

        System.out.println("VNPayService: Generated final payment URL: " + paymentUrl);
        System.out.println("VNPayService: URL length: " + paymentUrl.length());

        return paymentUrl;
    }
}