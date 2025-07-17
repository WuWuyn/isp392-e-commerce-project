package com.example.isp392.controller;

import com.example.isp392.dto.CheckoutDTO;
import com.example.isp392.model.GroupOrder;
import com.example.isp392.repository.GroupOrderRepository;
import com.example.isp392.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class ApiCheckoutController {
    private final CheckoutService checkoutService;
    private final GroupOrderRepository groupOrderRepository;

    @PostMapping
    public ResponseEntity<GroupOrder> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CheckoutDTO checkoutDTO) {
        Integer userId = Integer.parseInt(userDetails.getUsername());
        GroupOrder groupOrder = checkoutService.checkout(userId, checkoutDTO);
        return ResponseEntity.ok(groupOrder);
    }

    @GetMapping("/user-orders")
    public ResponseEntity<List<GroupOrder>> getUserOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = Integer.parseInt(userDetails.getUsername());
        List<GroupOrder> orders = groupOrderRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<String> vnpayReturn(@RequestParam Map<String, String> queryParams) {
        try {
            checkoutService.handleVNPayCallback(queryParams);
            return ResponseEntity.ok("Thanh toán thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Thanh toán thất bại: " + e.getMessage());
        }
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<String> vnpayIPN(@RequestParam Map<String, String> queryParams) {
        try {
            checkoutService.handleVNPayCallback(queryParams);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("FAIL");
        }
    }
} 