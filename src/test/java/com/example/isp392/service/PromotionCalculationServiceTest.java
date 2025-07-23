package com.example.isp392.service;

import com.example.isp392.model.Promotion;
import com.example.isp392.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionCalculationServiceTest {

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private PromotionCalculationService promotionCalculationService;

    private User testUser;
    private Promotion percentagePromotion;
    private Promotion fixedAmountPromotion;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setEmail("test@example.com");

        // Create percentage discount promotion
        percentagePromotion = new Promotion();
        percentagePromotion.setPromotionId(1);
        percentagePromotion.setCode("SAVE20");
        percentagePromotion.setName("20% Off");
        percentagePromotion.setPromotionType(Promotion.PromotionType.PERCENTAGE_DISCOUNT);
        percentagePromotion.setDiscountValue(new BigDecimal("20"));
        percentagePromotion.setMaxDiscountAmount(new BigDecimal("50000"));
        percentagePromotion.setMinOrderValue(new BigDecimal("100000"));
        percentagePromotion.setScopeType(Promotion.ScopeType.SITE_WIDE);
        percentagePromotion.setIsActive(true);
        percentagePromotion.setStartDate(LocalDateTime.now().minusDays(1));
        percentagePromotion.setEndDate(LocalDateTime.now().plusDays(30));
        percentagePromotion.setCurrentUsageCount(0);

        // Create fixed amount discount promotion
        fixedAmountPromotion = new Promotion();
        fixedAmountPromotion.setPromotionId(2);
        fixedAmountPromotion.setCode("FIXED30K");
        fixedAmountPromotion.setName("30K Off");
        fixedAmountPromotion.setPromotionType(Promotion.PromotionType.FIXED_AMOUNT_DISCOUNT);
        fixedAmountPromotion.setDiscountValue(new BigDecimal("30000"));
        fixedAmountPromotion.setMinOrderValue(new BigDecimal("50000"));
        fixedAmountPromotion.setScopeType(Promotion.ScopeType.SITE_WIDE);
        fixedAmountPromotion.setIsActive(true);
        fixedAmountPromotion.setStartDate(LocalDateTime.now().minusDays(1));
        fixedAmountPromotion.setEndDate(LocalDateTime.now().plusDays(30));
        fixedAmountPromotion.setCurrentUsageCount(0);
    }

    @Test
    void testValidatePromotion_Success() {
        // Arrange
        when(promotionService.findByCode("SAVE20")).thenReturn(Optional.of(percentagePromotion));
        // Remove unnecessary stubbing - getUserUsageCount is not called when usage limits are null

        // Act
        PromotionCalculationService.PromotionValidationResult result =
            promotionCalculationService.validatePromotion("SAVE20", testUser, new BigDecimal("200000"));

        // Assert
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        assertEquals(percentagePromotion, result.getPromotion());
    }

    @Test
    void testValidatePromotion_NotFound() {
        // Arrange
        when(promotionService.findByCode("INVALID")).thenReturn(Optional.empty());

        // Act
        PromotionCalculationService.PromotionValidationResult result = 
            promotionCalculationService.validatePromotion("INVALID", testUser, new BigDecimal("200000"));

        // Assert
        assertFalse(result.isValid());
        assertEquals("Mã giảm giá không tồn tại", result.getErrorMessage());
    }

    @Test
    void testValidatePromotion_BelowMinimumOrder() {
        // Arrange
        when(promotionService.findByCode("SAVE20")).thenReturn(Optional.of(percentagePromotion));

        // Act
        PromotionCalculationService.PromotionValidationResult result = 
            promotionCalculationService.validatePromotion("SAVE20", testUser, new BigDecimal("50000"));

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Giá trị đơn hàng tối thiểu"));
    }

    @Test
    void testCalculateDiscount_Percentage() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("200000");

        // Act
        PromotionCalculationService.DiscountCalculationResult result = 
            promotionCalculationService.calculateDiscount(percentagePromotion, orderTotal);

        // Assert
        assertEquals(0, new BigDecimal("40000").compareTo(result.getDiscountAmount())); // 20% of 200000
        assertEquals(0, new BigDecimal("160000").compareTo(result.getFinalTotal()));
    }

    @Test
    void testCalculateDiscount_PercentageWithMaxCap() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("500000"); // Large order

        // Act
        PromotionCalculationService.DiscountCalculationResult result = 
            promotionCalculationService.calculateDiscount(percentagePromotion, orderTotal);

        // Assert
        assertEquals(new BigDecimal("50000"), result.getDiscountAmount()); // Capped at max discount
        assertEquals(new BigDecimal("450000"), result.getFinalTotal());
    }

    @Test
    void testCalculateDiscount_FixedAmount() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("100000");

        // Act
        PromotionCalculationService.DiscountCalculationResult result = 
            promotionCalculationService.calculateDiscount(fixedAmountPromotion, orderTotal);

        // Assert
        assertEquals(new BigDecimal("30000"), result.getDiscountAmount());
        assertEquals(new BigDecimal("70000"), result.getFinalTotal());
    }

    @Test
    void testCalculateDiscount_FixedAmountExceedsTotal() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("20000"); // Less than discount amount

        // Act
        PromotionCalculationService.DiscountCalculationResult result = 
            promotionCalculationService.calculateDiscount(fixedAmountPromotion, orderTotal);

        // Assert
        assertEquals(new BigDecimal("20000"), result.getDiscountAmount()); // Limited to order total
        assertEquals(new BigDecimal("0"), result.getFinalTotal());
    }

    @Test
    void testApplyPromotion_Success() {
        // Arrange
        when(promotionService.findByCode("SAVE20")).thenReturn(Optional.of(percentagePromotion));
        // Remove unnecessary stubbing - getUserUsageCount is not called when usage limits are null

        // Act
        PromotionCalculationService.PromotionApplicationResult result =
            promotionCalculationService.applyPromotion("SAVE20", testUser, new BigDecimal("200000"));

        // Assert
        assertTrue(result.isSuccess());
        assertNull(result.getErrorMessage());
        assertEquals(0, new BigDecimal("40000").compareTo(result.getDiscountAmount()));
        assertEquals(0, new BigDecimal("160000").compareTo(result.getFinalTotal()));
        assertEquals(percentagePromotion, result.getPromotion());
    }

    @Test
    void testApplyPromotion_ValidationFailure() {
        // Arrange
        when(promotionService.findByCode("INVALID")).thenReturn(Optional.empty());

        // Act
        PromotionCalculationService.PromotionApplicationResult result = 
            promotionCalculationService.applyPromotion("INVALID", testUser, new BigDecimal("200000"));

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Mã giảm giá không tồn tại", result.getErrorMessage());
        assertEquals(new BigDecimal("0"), result.getDiscountAmount());
        assertEquals(new BigDecimal("200000"), result.getFinalTotal());
    }
}
