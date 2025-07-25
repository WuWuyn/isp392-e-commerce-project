package com.example.isp392.controller.buyer;

import com.example.isp392.dto.CheckoutDiscountBreakdown;
import com.example.isp392.model.CartItem;
import com.example.isp392.model.User;
import com.example.isp392.model.UserAddress;
import com.example.isp392.service.CartService;
import com.example.isp392.service.UserAddressService;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/buyer/checkout")
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    private final CartService cartService;
    private final UserService userService;
    private final UserAddressService userAddressService;

    public CheckoutController(CartService cartService,
                              UserService userService,
                              UserAddressService userAddressService) {
        this.cartService = cartService;
        this.userService = userService;
        this.userAddressService = userAddressService;
    }

    @GetMapping
    public String showCheckoutPage(
            @RequestParam(name = "items", required = false) String selectedItems,
            @RequestParam(name = "buyNow", required = false) Boolean buyNow,
            Model model,
            Authentication authentication,
            HttpSession session
    ) {
        logger.info("Checkout request received with items: {}, buyNow: {}", selectedItems, buyNow);

        // Validate authentication
        if (authentication == null) {
            logger.warn("Authentication is null, redirecting to login");
            return "redirect:/login";
        }

        // Get current user
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElse(null);

        if (user == null) {
            logger.warn("User not found for email: {}", email);
            return "redirect:/login";
        }

        // Handle Buy Now flow
        if (Boolean.TRUE.equals(buyNow)) {
            return handleBuyNowCheckout(model, session, user);
        }

        // Validate selected items for regular cart checkout
        if (!StringUtils.hasText(selectedItems)) {
            logger.warn("No items selected for checkout, redirecting to cart");
            return "redirect:/buyer/cart";
        }

        // Get selected cart items
        String[] itemIds = selectedItems.split(",");
        logger.info("Parsed item IDs: {}", Arrays.toString(itemIds));

        if (itemIds.length == 0) {
            logger.warn("No item IDs found after splitting, redirecting to cart");
            return "redirect:/buyer/cart";
        }

        try {
            List<CartItem> selectedCartItems = cartService.getSelectedCartItems(user, itemIds);
            logger.info("Retrieved {} cart items for user: {}", selectedCartItems.size(), user.getEmail());

            // Debug: log each cart item
            for (CartItem item : selectedCartItems) {
                logger.info("Cart item: Book={}, Price={}, Quantity={}, Total={}",
                    item.getBook().getTitle(),
                    item.getBook().getSellingPrice(),
                    item.getQuantity(),
                    item.getBook().getSellingPrice().doubleValue() * item.getQuantity());
            }

            if (selectedCartItems.isEmpty()) {
                logger.warn("No cart items found for the given IDs, redirecting to cart");
                return "redirect:/buyer/cart";
            }

            // Calculate totals
            double subtotal = selectedCartItems.stream()
                    .mapToDouble(item -> item.getBook().getSellingPrice().doubleValue() * item.getQuantity())
                    .sum();

            // Group selected items by shop to calculate shipping
            Map<String, List<CartItem>> itemsByShop = cartService.groupCartItemsByShop(selectedCartItems);

            // Calculate shipping fee (30,000 VND per shop)
            double shippingFee = itemsByShop.size() * 30000.0;

            // Get discount information from session (applied in cart)
            CheckoutDiscountBreakdown appliedPromotion = (CheckoutDiscountBreakdown) session.getAttribute("appliedPromotion");

            // Calculate final total amount (like final_total_amount logic)
            double discountAmount = 0.0;
            if (appliedPromotion != null && appliedPromotion.isSuccess()) {
                discountAmount = appliedPromotion.getTotalDiscount().doubleValue();
            }

            double originalTotalAmount = subtotal + shippingFee;
            double finalTotalAmount = originalTotalAmount - discountAmount;

            logger.info("Checkout totals - Subtotal: {}, Shipping: {}, Discount: {}, Original: {}, Final: {}",
                    subtotal, shippingFee, discountAmount, originalTotalAmount, finalTotalAmount);

            // Get user's addresses
            List<UserAddress> userAddresses = userAddressService.findByUser(user);
            UserAddress defaultAddress = userAddresses.stream()
                    .filter(UserAddress::isDefault)
                    .findFirst()
                    .orElse(null);

            // Add to model
            model.addAttribute("selectedItems", selectedCartItems);
            model.addAttribute("itemsByShop", itemsByShop);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("originalTotalAmount", originalTotalAmount);
            model.addAttribute("totalAmount", finalTotalAmount); // This is the final amount user pays
            model.addAttribute("userAddresses", userAddresses);
            model.addAttribute("defaultAddress", defaultAddress);
            model.addAttribute("user", user);

            // Add discount information to model
            if (appliedPromotion != null && appliedPromotion.isSuccess()) {
                model.addAttribute("appliedPromotion", appliedPromotion);
                logger.info("Applied promotion from session: {} - {}", appliedPromotion.getPromoCode(), appliedPromotion.getTotalDiscount());
            }

            // Store selected items in session for order processing
            session.setAttribute("checkoutItems", selectedCartItems);

            return "buyer/checkout";
        } catch (Exception e) {
            logger.error("Error processing checkout: {}", e.getMessage(), e);
            return "redirect:/buyer/cart?error=checkout_failed";
        }
    }

    @SuppressWarnings("unchecked")
    private String handleBuyNowCheckout(Model model, HttpSession session, User user) {
        // Get Buy Now session data
        Map<String, Object> buyNowSession = (Map<String, Object>) session.getAttribute("buyNowSession");

        if (buyNowSession == null) {
            logger.warn("No Buy Now session found, redirecting to cart");
            return "redirect:/buyer/cart";
        }

        try {
            Integer bookId = (Integer) buyNowSession.get("bookId");
            Integer quantity = (Integer) buyNowSession.get("quantity");
            Integer userId = (Integer) buyNowSession.get("userId");

            // Validate session data
            if (bookId == null || quantity == null || userId == null || !userId.equals(user.getUserId())) {
                logger.warn("Invalid Buy Now session data, redirecting to cart");
                session.removeAttribute("buyNowSession");
                return "redirect:/buyer/cart";
            }

            // Create a temporary cart item for Buy Now
            List<CartItem> buyNowItems = cartService.createBuyNowCartItems(user, bookId, quantity);

            if (buyNowItems.isEmpty()) {
                logger.warn("Could not create Buy Now cart items, redirecting to cart");
                session.removeAttribute("buyNowSession");
                return "redirect:/buyer/cart";
            }

            // Calculate totals
            double subtotal = buyNowItems.stream()
                    .mapToDouble(item -> item.getBook().getSellingPrice().doubleValue() * item.getQuantity())
                    .sum();

            // Get user's addresses
            List<UserAddress> userAddresses = userAddressService.findByUser(user);
            UserAddress defaultAddress = userAddresses.stream()
                    .filter(UserAddress::isDefault)
                    .findFirst()
                    .orElse(null);

            // Group items by shop (for Buy Now, there should be only one shop)
            Map<String, List<CartItem>> itemsByShop = cartService.groupCartItemsByShop(buyNowItems);

            // Get discount information from session (applied in cart)
            String discountCode = (String) session.getAttribute("appliedDiscountCode");
            BigDecimal discountAmount = (BigDecimal) session.getAttribute("appliedDiscountAmount");
            String discountDescription = (String) session.getAttribute("appliedDiscountDescription");

            // Add to model
            model.addAttribute("selectedItems", buyNowItems);
            model.addAttribute("itemsByShop", itemsByShop);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("userAddresses", userAddresses);
            model.addAttribute("defaultAddress", defaultAddress);
            model.addAttribute("user", user);
            model.addAttribute("isBuyNow", true);

            // Add discount information to model
            if (discountCode != null && discountAmount != null) {
                model.addAttribute("discountCode", discountCode);
                model.addAttribute("discountAmount", discountAmount);
                model.addAttribute("discountDescription", discountDescription);
                logger.info("Applied discount from session for Buy Now: {} - {}", discountCode, discountAmount);
            }

            // Store Buy Now items in session for order processing
            session.setAttribute("checkoutItems", buyNowItems);
            session.setAttribute("isBuyNow", true);

            return "buyer/checkout";
        } catch (Exception e) {
            logger.error("Error processing Buy Now checkout: {}", e.getMessage(), e);
            session.removeAttribute("buyNowSession");
            return "redirect:/buyer/cart?error=buynow_failed";
        }
    }
}