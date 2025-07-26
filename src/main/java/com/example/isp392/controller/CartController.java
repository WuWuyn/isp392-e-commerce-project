package com.example.isp392.controller;

import com.example.isp392.dto.CheckoutDiscountBreakdown;
import com.example.isp392.model.Book;
import com.example.isp392.model.Promotion;
import com.example.isp392.model.User;
import com.example.isp392.service.BookService;
import com.example.isp392.service.CartService;
import com.example.isp392.service.PromotionCalculationService;
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
import java.util.*;

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
    private final PromotionCalculationService promotionCalculationService;
    private final BookService bookService;

    public CartController(CartService cartService, UserService userService,
                         PromotionCalculationService promotionCalculationService, BookService bookService) {
        this.cartService = cartService;
        this.userService = userService;
        this.promotionCalculationService = promotionCalculationService;
        this.bookService = bookService;
    }



    /**
     * Clear applied discount from session
     */
    @PostMapping("/clear-discount")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearDiscount(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        // Remove discount from session
        session.removeAttribute("appliedDiscountCode");
        session.removeAttribute("appliedDiscountAmount");
        session.removeAttribute("appliedDiscountDescription");

        response.put("success", true);
        response.put("message", "Discount cleared successfully");

        log.debug("Cleared discount from session");
        return ResponseEntity.ok(response);
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
            // Calculate unique item count (number of different items)
            int uniqueItemCount = cartService.getUniqueItemCount(user);

            response.put("success", true);
            response.put("uniqueItemCount", uniqueItemCount);
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
                response.put("message", "Requested book quantity exceeds available stock");
                return ResponseEntity.badRequest().body(response);
            }
            
            cartService.addBookToCart(user, bookId, quantity);

            // Get updated unique item count for UI update
            int uniqueItemCount = cartService.getUniqueItemCount(user);

            response.put("success", true);
            response.put("message", "Book added to cart successfully");
            response.put("cartCount", uniqueItemCount);
            
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
                    response.put("message", "Requested book quantity exceeds available stock");
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
                response.put("message", "Requested book quantity exceeds available stock");
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
     * Apply promotion to selected cart items (simplified)
     */
    @PostMapping("/apply-promotion")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyPromotion(
            @RequestBody Map<String, Object> request,
            Authentication authentication,
            HttpSession session) {

        log.info("Applying promotion in cart: {}", request);

        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null) {
                response.put("success", false);
                response.put("message", "Please login to apply promotion");
                return ResponseEntity.ok(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email).orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

            String promotionCode = (String) request.get("promotionCode");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selectedItemsData = (List<Map<String, Object>>) request.get("selectedItems");

            if (promotionCode == null || promotionCode.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Promotion code is required");
                return ResponseEntity.ok(response);
            }

            if (selectedItemsData == null || selectedItemsData.isEmpty()) {
                response.put("success", false);
                response.put("message", "No items selected");
                return ResponseEntity.ok(response);
            }

            // Calculate total order value from selected items
            BigDecimal totalOrderValue = BigDecimal.ZERO;
            Map<Integer, BigDecimal> shopTotals = new HashMap<>();

            for (Map<String, Object> itemData : selectedItemsData) {
                Integer bookId = safeToInteger(itemData.get("bookId"), "bookId");
                Integer quantity = safeToInteger(itemData.get("quantity"), "quantity");
                BigDecimal price = safeToBigDecimal(itemData.get("price"), "price");

                BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(quantity));

                // Get book to determine shop
                Book book = bookService.getBookById(bookId).orElseThrow(() ->
                        new IllegalArgumentException("Book not found: " + bookId));

                Integer shopId = book.getShop().getShopId();
                shopTotals.merge(shopId, itemTotal, BigDecimal::add);
                totalOrderValue = totalOrderValue.add(itemTotal);
            }

            // Add shipping fees (30,000 VND per shop)
            BigDecimal totalShippingFee = new BigDecimal("30000").multiply(BigDecimal.valueOf(shopTotals.size()));
            totalOrderValue = totalOrderValue.add(totalShippingFee);

            // Apply promotion using simple calculation
            PromotionCalculationService.PromotionApplicationResult result =
                promotionCalculationService.applyPromotion(promotionCode, user, totalOrderValue);

            if (result.isSuccess()) {
                // Create simple breakdown for session storage
                CheckoutDiscountBreakdown breakdown = new CheckoutDiscountBreakdown();
                breakdown.setSuccess(true);
                breakdown.setPromoCode(promotionCode);
                breakdown.setTotalDiscount(result.getDiscountAmount());
                breakdown.setMessage("Promotion applied successfully");

                // Store in session for checkout
                session.setAttribute("appliedPromotion", breakdown);
                log.info("Applied promotion {} in cart with discount: {}", promotionCode, result.getDiscountAmount());

                response.put("success", true);
                response.put("promoCode", promotionCode);
                response.put("totalDiscount", result.getDiscountAmount());
                response.put("message", "Promotion applied successfully");
            } else {
                response.put("success", false);
                response.put("message", result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error applying promotion in cart", e);
            response.put("success", false);
            response.put("message", "Error applying promotion: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Store promotion in session for checkout
     */
    @PostMapping("/store-promotion")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> storePromotion(
            @RequestBody CheckoutDiscountBreakdown promotion,
            HttpSession session) {

        log.info("Storing promotion in session: {}", promotion.getPromoCode());

        Map<String, Object> response = new HashMap<>();

        try {
            session.setAttribute("appliedPromotion", promotion);
            response.put("success", true);
            response.put("message", "Promotion stored successfully");
        } catch (Exception e) {
            log.error("Error storing promotion", e);
            response.put("success", false);
            response.put("message", "Error storing promotion");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Remove promotion from session
     */
    @PostMapping("/remove-promotion")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removePromotion(HttpSession session) {
        log.info("Removing promotion from session");

        Map<String, Object> response = new HashMap<>();

        try {
            session.removeAttribute("appliedPromotion");
            response.put("success", true);
            response.put("message", "Promotion removed successfully");
        } catch (Exception e) {
            log.error("Error removing promotion", e);
            response.put("success", false);
            response.put("message", "Error removing promotion");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Safely convert object to Integer (handles Integer, Double, String)
     */
    private Integer safeToInteger(Object obj, String fieldName) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Double) {
            return ((Double) obj).intValue();
        } else if (obj instanceof String) {
            return Integer.parseInt((String) obj);
        } else {
            throw new IllegalArgumentException("Invalid " + fieldName + " format: " + obj);
        }
    }

    /**
     * Safely convert object to BigDecimal (handles Integer, Double, String)
     */
    private BigDecimal safeToBigDecimal(Object obj, String fieldName) {
        if (obj instanceof Integer) {
            return BigDecimal.valueOf((Integer) obj);
        } else if (obj instanceof Double) {
            return BigDecimal.valueOf((Double) obj);
        } else if (obj instanceof String) {
            return new BigDecimal((String) obj);
        } else {
            throw new IllegalArgumentException("Invalid " + fieldName + " format: " + obj);
        }
    }

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
