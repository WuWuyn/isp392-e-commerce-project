package com.example.isp392.config;

import com.example.isp392.model.Cart;
import com.example.isp392.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CartService cartService;

    @ModelAttribute("cart")
    public Cart addCartToModel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            // Assuming your CartService has a method to get the cart for the authenticated user
            // You might need to adjust this based on how your CartService identifies the user
            return cartService.getCartForCurrentUser();
        }
        return new Cart(); // Return an empty cart if not authenticated or anonymous
    }
} 