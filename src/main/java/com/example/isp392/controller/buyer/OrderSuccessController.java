package com.example.isp392.controller.buyer;

import com.example.isp392.model.Order;
import com.example.isp392.model.User;
import com.example.isp392.service.OrderService;
import com.example.isp392.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/buyer")
public class OrderSuccessController {

    private static final Logger logger = LoggerFactory.getLogger(OrderSuccessController.class);
    
    private final OrderService orderService;
    private final UserService userService;

    public OrderSuccessController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/order-success")
    public String showOrderSuccessPage(@RequestParam Integer orderId,
                                     Authentication authentication,
                                     Model model) {
        
        if (authentication == null) {
            return "redirect:/login";
        }

        // Get current user
        String email = authentication.getName();
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();

        // Get order details
        Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
        if (orderOpt.isEmpty()) {
            logger.warn("Order not found or doesn't belong to current user: {}", orderId);
            return "redirect:/buyer/orders?error=order_not_found";
        }
        
        // Add order to model
        model.addAttribute("order", orderOpt.get());
        
        return "buyer/order-success";
    }
} 