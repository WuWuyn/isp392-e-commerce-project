package com.example.isp392.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class PaymentConfig {
    public static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    @Value("${vnpay.terminal-id}")
    private String vnpayTerminalId;

    @Value("${vnpay.secret-key}")
    private String vnpaySecretKey;

    @Value("${vnpay.payment-url}")
    private String vnpayPaymentUrl;

    @Value("${vnpay.api-url}")
    private String vnpayApiUrl;

    @Value("${app.return-url}")
    private String returnUrl;

    public String getVnpayTerminalId() {
        return vnpayTerminalId;
    }

    public String getVnpaySecretKey() {
        return vnpaySecretKey;
    }

    public String getVnpayPaymentUrl() {
        return vnpayPaymentUrl;
    }

    public String getVnpayApiUrl() {
        return vnpayApiUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }
} 