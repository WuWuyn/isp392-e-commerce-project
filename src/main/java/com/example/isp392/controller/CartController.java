package com.example.isp392.controller;

import com.example.isp392.model.Promotion;
import com.example.isp392.model.User;
import com.example.isp392.service.CartService;
import com.example.isp392.service.PromotionService;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller responsible for AJAX operations on the buyer's cart such as
 * removing items, deleting multiple selected items, and updating quantity.
 * <p>
 * All endpoints here are secured by Spring Security. They rely on the
 * currently authenticated user contained in the SecurityContext.
 */
@Controller
@RequestMapping("/buyer/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final UserService userService;
    private final PromotionService promotionService;

    public CartController(CartService cartService, UserService userService, PromotionService promotionService) {
        this.cartService = cartService;
        this.userService = userService;
        this.promotionService = promotionService;
    }

    /**
     * Apply a discount code to the cart.
     * @param payload JSON payload containing the discount code
     * @return HTTP 200 with success status and discount amount if valid, 400 otherwise
     */
    @PostMapping("/apply-discount")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyDiscount(@RequestBody Map<String, String> payload) {
        Optional<User> userOptional = getAuthenticatedUser();
        Map<String, Object> response = new HashMap<>();
        
        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "Người dùng chưa đăng nhập");
            return ResponseEntity.badRequest().body(response);
        }
        
        String code = payload.get("code");
        if (code == null || code.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Mã giảm giá không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = userOptional.get();
        try {
            // Find the promotion by code
            Optional<Promotion> promotionOpt = promotionService.findByCode(code);
            
            if (promotionOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Mã giảm giá không tồn tại");
                return ResponseEntity.ok(response);
            }
            
            Promotion promotion = promotionOpt.get();
            
            // Check if promotion is active
            if (!promotion.getIsActive()) {
                response.put("success", false);
                response.put("message", "Mã giảm giá đã hết hạn");
                return ResponseEntity.ok(response);
            }
            
            // Check if promotion is within valid date range
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
                response.put("success", false);
                response.put("message", "Mã giảm giá không trong thời gian sử dụng");
                return ResponseEntity.ok(response);
            }
            
            // Check usage limit per user if applicable
            if (promotion.getUsageLimitPerUser() != null && promotion.getUsageLimitPerUser() > 0) {
                int userUsageCount = promotionService.getUserUsageCount(user.getUserId(), promotion.getPromotionId());
                if (userUsageCount >= promotion.getUsageLimitPerUser()) {
                    response.put("success", false);
                    response.put("message", "Bạn đã sử dụng hết số lần cho phép của mã giảm giá này");
                    return ResponseEntity.ok(response);
                }
            }
            
            // Check total usage limit if applicable
            if (promotion.getTotalUsageLimit() != null && promotion.getTotalUsageLimit() > 0) {
                if (promotion.getCurrentUsageCount() >= promotion.getTotalUsageLimit()) {
                    response.put("success", false);
                    response.put("message", "Mã giảm giá đã hết lượt sử dụng");
                    return ResponseEntity.ok(response);
                }
            }
            
            // Get cart total
            BigDecimal cartTotal = cartService.getCartTotal(user);
            
            // Check minimum order value if applicable
            if (promotion.getMinOrderValue() != null && cartTotal.compareTo(promotion.getMinOrderValue()) < 0) {
                response.put("success", false);
                response.put("message", "Giá trị đơn hàng tối thiểu để sử dụng mã này là " + 
                             promotion.getMinOrderValue().toString() + " VND");
                return ResponseEntity.ok(response);
            }
            
            // Calculate discount amount
            BigDecimal discountAmount;
            if ("PERCENTAGE".equals(promotion.getDiscountType())) {
                discountAmount = cartTotal.multiply(promotion.getDiscountValue().divide(new BigDecimal(100)));
                
                // Apply max discount amount if applicable
                if (promotion.getMaxDiscountAmount() != null && 
                    discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                    discountAmount = promotion.getMaxDiscountAmount();
                }
            } else {
                // Fixed amount discount
                discountAmount = promotion.getDiscountValue();
                
                // Ensure discount doesn't exceed cart total
                if (discountAmount.compareTo(cartTotal) > 0) {
                    discountAmount = cartTotal;
                }
            }
            
            response.put("success", true);
            response.put("message", "Áp dụng mã giảm giá thành công");
            response.put("discountAmount", discountAmount);
            response.put("discountCode", promotion.getCode());
            response.put("discountDescription", promotion.getDescription());
            
            log.debug("Applied discount code {} for user {}, amount: {}", 
                     code, user.getEmail(), discountAmount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error applying discount code: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi áp dụng mã giảm giá");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get the total quantity of all items in the authenticated user's cart.
     * @return HTTP 200 with the total quantity, or 400 if user not authenticated.
     */
    @GetMapping("/total-quantity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCartTotalQuantity() {
        Optional<User> userOptional = getAuthenticatedUser();
        Map<String, Object> response = new HashMap<>();

        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not authenticated");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userOptional.get();
        try {
            // Use the cartService to get the total quantity
            int totalQuantity = cartService.getCartForUser(user).getItems().stream()
                                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                                    .sum();

            response.put("success", true);
            response.put("totalQuantity", totalQuantity);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting cart total quantity: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to get cart total quantity");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Add a book to the cart.
     * @param payload JSON payload containing bookId and quantity
     * @return HTTP 200 with success status and cart count if added, 400 otherwise
     */
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(@RequestBody Map<String, Object> payload) {
        Optional<User> userOptional = getAuthenticatedUser();
        Map<String, Object> response = new HashMap<>();
        
        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not authenticated");
            return ResponseEntity.badRequest().body(response);
        }
        
        Integer bookId;
        Integer quantity;
        
        try {
            bookId = Integer.valueOf(payload.get("bookId").toString());
            quantity = Integer.valueOf(payload.get("quantity").toString());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Invalid payload");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (quantity <= 0) {
            response.put("success", false);
            response.put("message", "Quantity must be greater than zero");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = userOptional.get();
        try {
            // Kiểm tra số lượng trước khi thêm vào giỏ hàng
            if (!cartService.checkCartItemAvailability(user, bookId, quantity)) {
                response.put("success", false);
                response.put("message", "Số lượng sách yêu cầu vượt quá số lượng hiện có trong kho");
                return ResponseEntity.badRequest().body(response);
            }
            
            cartService.addBookToCart(user, bookId, quantity);
            
            // Get updated cart count for UI update
            int cartCount = cartService.getCartForUser(user).getItems().size();
            
            response.put("success", true);
            response.put("message", "Book added to cart successfully");
            response.put("cartCount", cartCount);
            
            log.debug("Added book {} to cart of user {} with quantity {}", bookId, user.getEmail(), quantity);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to add book to cart: " + e.getMessage());
            log.error("Error adding book to cart: {}", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Remove a single item from the cart.
     * @param bookId book identifier
     * @return HTTP 200 if removed, 400 otherwise
     */
    @PostMapping("/remove/{bookId}")
    @ResponseBody
    public ResponseEntity<String> removeItem(@PathVariable Integer bookId) {
        Optional<User> userOptional = getAuthenticatedUser();
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("User not authenticated");
        }
        User user = userOptional.get();
        cartService.removeItem(user, bookId);
        log.debug("Removed book {} from cart of user {}", bookId, user.getEmail());
        return ResponseEntity.ok("Item removed successfully");
    }

    /**
     * Remove multiple items selected in the cart.
     * @param bookIds list of book IDs to remove
     * @return HTTP 200 if removed, 400 otherwise
     */
    @PostMapping("/delete-selected")
    @ResponseBody
    public ResponseEntity<String> deleteSelectedItems(@RequestBody List<Integer> bookIds) {
        Optional<User> userOptional = getAuthenticatedUser();
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("User not authenticated");
        }
        if (bookIds == null || bookIds.isEmpty()) {
            return ResponseEntity.badRequest().body("No items selected");
        }
        User user = userOptional.get();
        for (Integer id : bookIds) {
            cartService.removeItem(user, id);
        }
        log.debug("Removed books {} from cart of user {}", bookIds, user.getEmail());
        return ResponseEntity.ok("Selected items deleted successfully");
    }

    /**
     * Update quantity of a particular cart item. If quantity is 0 or negative, the item is removed.
     * @param payload JSON payload containing bookId and quantity
     * @return HTTP 200 if updated/removed, 400 otherwise
     */
    @PostMapping("/update-qty")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQuantity(@RequestBody Map<String, Object> payload) {
        Optional<User> userOptional = getAuthenticatedUser();
        Map<String, Object> response = new HashMap<>();
        
        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not authenticated");
            return ResponseEntity.badRequest().body(response);
        }
        
        Integer bookId;
        Integer quantity;
        
        try {
            bookId = Integer.valueOf(payload.get("bookId").toString());
            quantity = Integer.valueOf(payload.get("quantity").toString());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Invalid payload");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = userOptional.get();
        
        try {
            if (quantity != null && quantity > 0) {
                // Kiểm tra số lượng sách trong kho
                if (!cartService.checkBookAvailability(bookId, quantity)) {
                    response.put("success", false);
                    response.put("message", "Số lượng sách yêu cầu vượt quá số lượng hiện có trong kho");
                    return ResponseEntity.badRequest().body(response);
                }
                
                cartService.updateQuantity(user, bookId, quantity);
                log.debug("Updated quantity of book {} to {} for user {}", bookId, quantity, user.getEmail());
                
                response.put("success", true);
                response.put("message", "Quantity updated successfully");
                return ResponseEntity.ok(response);
            } else {
                cartService.removeItem(user, bookId);
                log.debug("Removed book {} due to non-positive quantity for user {}", bookId, user.getEmail());
                
                response.put("success", true);
                response.put("message", "Item removed as quantity is 0");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Handle Buy Now functionality - creates a temporary checkout session and redirects to checkout
     * @param payload JSON payload containing bookId, quantity and buyNow flag
     * @return HTTP 200 with redirect URL if successful, 400 otherwise
     */
    @PostMapping("/buy-now")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buyNow(@RequestBody Map<String, Object> payload, HttpSession session) {
        Optional<User> userOptional = getAuthenticatedUser();
        Map<String, Object> response = new HashMap<>();
        
        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not authenticated");
            return ResponseEntity.badRequest().body(response);
        }
        
        Integer bookId;
        Integer quantity;
        
        try {
            bookId = Integer.valueOf(payload.get("bookId").toString());
            quantity = Integer.valueOf(payload.get("quantity").toString());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Invalid payload");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (quantity <= 0) {
            response.put("success", false);
            response.put("message", "Quantity must be greater than zero");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = userOptional.get();
        try {
            // Kiểm tra số lượng trong kho
            if (!cartService.checkBookAvailability(bookId, quantity)) {
                response.put("success", false);
                response.put("message", "Số lượng sách yêu cầu vượt quá số lượng hiện có trong kho");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create a temporary buy now session
            Map<String, Object> buyNowSession = new HashMap<>();
            buyNowSession.put("bookId", bookId);
            buyNowSession.put("quantity", quantity);
            buyNowSession.put("userId", user.getUserId());
            
            // Store in session
            session.setAttribute("buyNowSession", buyNowSession);
            
            // Return success with redirect URL
            response.put("success", true);
            response.put("redirectUrl", "/buyer/checkout?buyNow=true");
            
            log.debug("Buy Now: Created checkout session for book {} for user {}", bookId, user.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in Buy Now: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to process Buy Now request: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ---------- Helper methods ----------

    /**
     * Convenience method to retrieve the currently authenticated user as Optional.
     */
    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String email = authentication.getName();
        return userService.findByEmail(email);
    }
}
