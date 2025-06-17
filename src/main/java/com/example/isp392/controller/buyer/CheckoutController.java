package com.example.isp392.controller.buyer;

import com.example.isp392.model.Book;
import com.example.isp392.model.CartItem;
import com.example.isp392.model.User;
import com.example.isp392.model.UserAddress;
import com.example.isp392.service.BookService;
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
import java.util.ArrayList;
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
    private final BookService bookService;

    public CheckoutController(CartService cartService, 
                            UserService userService, 
                            UserAddressService userAddressService,
                            BookService bookService) {
        this.cartService = cartService;
        this.userService = userService;
        this.userAddressService = userAddressService;
        this.bookService = bookService;
    }

    @GetMapping
    public String showCheckoutPage(
            @RequestParam(name = "items", required = false) String selectedItems,
            @RequestParam(name = "buyNow", required = false) Boolean isBuyNow,
            Model model,
            Authentication authentication,
            HttpSession session
    ) {
        logger.info("Checkout request received. BuyNow: {}, Items: {}", isBuyNow, selectedItems);
        
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

        List<CartItem> checkoutItems;
        try {
            if (Boolean.TRUE.equals(isBuyNow)) {
                // Handle Buy Now checkout
                @SuppressWarnings("unchecked")
                Map<String, Object> buyNowSession = (Map<String, Object>) session.getAttribute("buyNowSession");
                if (buyNowSession == null) {
                    logger.warn("Buy Now session not found, redirecting to home");
                    return "redirect:/";
                }

                // Validate user matches session
                if (!user.getUserId().equals(buyNowSession.get("userId"))) {
                    logger.warn("User mismatch in Buy Now session");
                    return "redirect:/";
                }

                // Create temporary cart item for buy now
                Integer bookId = (Integer) buyNowSession.get("bookId");
                Integer quantity = (Integer) buyNowSession.get("quantity");
                Book book = bookService.getBookById(bookId).orElse(null);

                if (book == null) {
                    logger.warn("Book not found for Buy Now: {}", bookId);
                    return "redirect:/";
                }

                CartItem buyNowItem = new CartItem();
                buyNowItem.setBook(book);
                buyNowItem.setQuantity(quantity);

                checkoutItems = new ArrayList<>();
                checkoutItems.add(buyNowItem);
            } else {
                // Handle normal cart checkout
                if (!StringUtils.hasText(selectedItems)) {
                    logger.warn("No items selected for checkout, redirecting to cart");
                    return "redirect:/buyer/cart";
                }

                String[] itemIds = selectedItems.split(",");
                if (itemIds.length == 0) {
                    logger.warn("No item IDs found after splitting, redirecting to cart");
                    return "redirect:/buyer/cart";
                }

                checkoutItems = cartService.getSelectedCartItems(user, itemIds);
                if (checkoutItems.isEmpty()) {
                    logger.warn("No cart items found for the given IDs, redirecting to cart");
                    return "redirect:/buyer/cart";
                }
            }

            // Calculate totals
            double subtotal = checkoutItems.stream()
                    .mapToDouble(item -> item.getBook().getSellingPrice().doubleValue() * item.getQuantity())
                    .sum();

            // Get user's addresses
            List<UserAddress> userAddresses = userAddressService.findByUser(user);
            UserAddress defaultAddress = userAddresses.stream()
                    .filter(UserAddress::isDefault)
                    .findFirst()
                    .orElse(null);
            
            // Check for discount code from cart
            String discountCode = (String) session.getAttribute("discountCode");
            BigDecimal discountAmount = (BigDecimal) session.getAttribute("discountAmount");
            
            // Set fixed shipping fee (30,000 VND)
            BigDecimal shippingFee = new BigDecimal(30000);
            
            // Calculate total amount
            BigDecimal totalAmount = BigDecimal.valueOf(subtotal)
                    .add(shippingFee)
                    .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);

            // Add to model
            model.addAttribute("selectedItems", checkoutItems);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("userAddresses", userAddresses);
            model.addAttribute("defaultAddress", defaultAddress);
            model.addAttribute("user", user);
            model.addAttribute("discountCode", discountCode);
            model.addAttribute("discountAmount", discountAmount);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("isBuyNow", isBuyNow);

            // Store selected items in session for order processing
            session.setAttribute("checkoutItems", checkoutItems);
            
            return "buyer/checkout";
        } catch (Exception e) {
            logger.error("Error processing checkout: {}", e.getMessage(), e);
            return "redirect:/buyer/cart?error=checkout_failed";
        }
    }
} 