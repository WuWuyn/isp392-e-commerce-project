package com.example.isp392.controller.buyer;

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

import java.util.Arrays;
import java.util.List;

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
            Model model,
            Authentication authentication,
            HttpSession session
    ) {
        logger.info("Checkout request received with items: {}", selectedItems);
        
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

        // Validate selected items
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
            logger.info("Retrieved {} cart items", selectedCartItems.size());
            
            if (selectedCartItems.isEmpty()) {
                logger.warn("No cart items found for the given IDs, redirecting to cart");
                return "redirect:/buyer/cart";
            }

            // Calculate totals
            double subtotal = selectedCartItems.stream()
                    .mapToDouble(item -> item.getBook().getSellingPrice().doubleValue() * item.getQuantity())
                    .sum();

            // Get user's addresses
            List<UserAddress> userAddresses = userAddressService.findByUser(user);
            UserAddress defaultAddress = userAddresses.stream()
                    .filter(UserAddress::isDefault)
                    .findFirst()
                    .orElse(null);

            // Add to model
            model.addAttribute("selectedItems", selectedCartItems);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("userAddresses", userAddresses);
            model.addAttribute("defaultAddress", defaultAddress);
            model.addAttribute("user", user);

            // Store selected items in session for order processing
            session.setAttribute("checkoutItems", selectedCartItems);

            return "buyer/checkout";
        } catch (Exception e) {
            logger.error("Error processing checkout: {}", e.getMessage(), e);
            return "redirect:/buyer/cart?error=checkout_failed";
        }
    }
} 