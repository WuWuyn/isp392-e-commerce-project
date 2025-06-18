package com.example.isp392.controller.api;

import com.example.isp392.dto.DiscountValidationResponse;
import com.example.isp392.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiscountApiController {

    private final DiscountService discountService;

    @PostMapping("/validate-discount")
    public ResponseEntity<DiscountValidationResponse> validateDiscount(
            @RequestBody Map<String, String> request,
            @RequestParam(required = false) BigDecimal subtotal) {
        
        String code = request.get("code");
        return ResponseEntity.ok(discountService.validateDiscountCode(code, subtotal));
    }
} 