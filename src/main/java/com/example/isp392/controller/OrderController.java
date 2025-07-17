package com.example.isp392.controller;

import com.example.isp392.model.*;
import com.example.isp392.service.CartService;
import com.example.isp392.service.GroupOrderService;
import com.example.isp392.service.OrderService;
import com.example.isp392.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/buyer")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;
    private final GroupOrderService groupOrderService;

    public OrderController(OrderService orderService, CartService cartService, UserService userService, GroupOrderService groupOrderService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
        this.groupOrderService = groupOrderService;
    }

    @GetMapping("/orders")
    public String getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            Model model,
            Authentication authentication) {
        
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get group orders with status
        List<GroupOrder> groupOrders = groupOrderService.findByUser(user);
        model.addAttribute("groupOrders", groupOrders);

        // Get individual orders with filters
        Page<Order> orderPage = orderService.findOrders(
            user, status, dateFrom, dateTo, PageRequest.of(page, 10)
        );

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        
        // Preserve filter parameters for pagination links
        model.addAttribute("paramStatus", status);
        model.addAttribute("paramDateFrom", dateFrom);
        model.addAttribute("paramDateTo", dateTo);
        
        return "buyer/order-history";
    }

    @GetMapping("/orders/{orderId}")
    public String getOrderDetail(@PathVariable Integer orderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
        if (orderOpt.isEmpty()) {
            return "redirect:/buyer/orders";
        }
        
        Order order = orderOpt.get();
        // Check if order belongs to a group order
        if (order.getGroupOrder() != null) {
            return "redirect:/buyer/group-orders/" + order.getGroupOrder().getGroupOrderId();
        }
        
        model.addAttribute("order", order);
        return "buyer/order-detail";
    }
    
    @PostMapping("/orders/{orderId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelOrder(@PathVariable Integer orderId, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Đơn hàng không tồn tại");
        }
        
        Order order = orderOpt.get();
        if (!order.canCancel()) {
            return ResponseEntity.badRequest().body("Không thể hủy đơn hàng ở trạng thái hiện tại");
        }
        
        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đơn hàng đã được hủy thành công");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/orders/{orderId}/rebuy")
    public String rebuyOrder(@PathVariable Integer orderId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không tồn tại");
            return "redirect:/buyer/orders";
        }
        
        Order order = orderOpt.get();
        // Add items from order to cart
        order.getOrderItems().forEach(item -> {
            try {
                cartService.addBookToCart(user, item.getBook().getBookId(), item.getQuantity());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Không thể thêm sản phẩm: " + e.getMessage());
            }
        });
        
        redirectAttributes.addFlashAttribute("success", "Đã thêm các sản phẩm vào giỏ hàng");
        return "redirect:/buyer/cart";
    }

    @PostMapping("/orders/{orderId}/items/{bookId}/rebuy")
    public String rebuyOrderItem(
            @PathVariable Integer orderId,
            @PathVariable Integer bookId,
            @RequestParam Integer quantity,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        try {
            cartService.addBookToCart(user, bookId, quantity);
            redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được thêm vào giỏ hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể thêm sản phẩm: " + e.getMessage());
        }
        
        return "redirect:/buyer/cart";
    }

    @GetMapping("/order-success")
    public String orderSuccessPage(@RequestParam Integer groupOrderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<GroupOrder> groupOrderOpt = groupOrderService.findByIdAndUser(groupOrderId, user);
        if (groupOrderOpt.isEmpty()) {
            return "redirect:/buyer/orders";
        }

        model.addAttribute("groupOrder", groupOrderOpt.get());
        return "buyer/order-success";
    }
} 