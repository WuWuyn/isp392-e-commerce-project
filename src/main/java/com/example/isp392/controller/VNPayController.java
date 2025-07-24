package com.example.isp392.controller;

import com.example.isp392.dto.CheckoutDTO;
import com.example.isp392.model.CustomerOrder;
import com.example.isp392.repository.CustomerOrderRepository;
import com.example.isp392.service.CheckoutService;
import com.example.isp392.service.VNPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VNPayController {
    private final CheckoutService checkoutService;
    private final CustomerOrderRepository customerOrderRepository;
    private final VNPayService vnPayService;

    /**
     * Process checkout and create VNPay payment URL
     */
    @PostMapping("/checkout")
    public ResponseEntity<CustomerOrder> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CheckoutDTO checkoutDTO) {
        Integer userId = Integer.parseInt(userDetails.getUsername());
        CustomerOrder customerOrder = checkoutService.checkout(userId, checkoutDTO);
        return ResponseEntity.ok(customerOrder);
    }

    /**
     * Get user's customer orders
     */
    @GetMapping("/user-orders")
    public ResponseEntity<List<CustomerOrder>> getUserOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = Integer.parseInt(userDetails.getUsername());
        List<CustomerOrder> orders = customerOrderRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Handle VNPay return callback (user redirect)
     */
    @GetMapping("/return")
    public ResponseEntity<String> vnpayReturn(@RequestParam Map<String, String> queryParams) {
        try {
            checkoutService.handleVNPayCallback(queryParams);
            return ResponseEntity.ok("Thanh toán thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Thanh toán thất bại: " + e.getMessage());
        }
    }

    /**
     * Handle VNPay IPN callback (server-to-server notification)
     */
    @PostMapping("/ipn")
    public ResponseEntity<String> vnpayIPN(@RequestParam Map<String, String> queryParams) {
        try {
            checkoutService.handleVNPayCallback(queryParams);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("FAIL");
        }
    }

    /**
     * Query transaction status from VNPay
     */
    @GetMapping("/query/{customerOrderId}")
    public ResponseEntity<Map<String, String>> queryTransactionStatus(
            @PathVariable Integer customerOrderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Verify user owns this order
            Integer userId = Integer.parseInt(userDetails.getUsername());
            List<CustomerOrder> userOrders = customerOrderRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
            CustomerOrder customerOrder = userOrders.stream()
                    .filter(order -> order.getCustomerOrderId().equals(customerOrderId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Order not found or access denied"));

            Map<String, String> result = vnPayService.queryTransactionStatus(customerOrderId.toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validate VNPay response signature
     */
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validatePaymentResponse(@RequestBody Map<String, String> vnpayResponse) {
        try {
            boolean isValid = vnPayService.validatePaymentResponse(vnpayResponse);
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }
}
