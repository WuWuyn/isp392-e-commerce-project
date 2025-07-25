package com.example.isp392.controller;

import com.example.isp392.model.*;
import com.example.isp392.service.CartService;
import com.example.isp392.service.CustomerOrderService;
import com.example.isp392.service.OrderService;
import com.example.isp392.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/buyer")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;
    private final CustomerOrderService customerOrderService;

    public OrderController(OrderService orderService, CartService cartService, UserService userService, CustomerOrderService customerOrderService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
        this.customerOrderService = customerOrderService;
    }

    @GetMapping("/orders")
    public String getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) String search,
            Model model,
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get individual orders with filters (display individual orders, not grouped by CustomerOrder), sorted by newest first
        Sort sort = Sort.by(Sort.Direction.DESC, "orderDate");
        Page<Order> orderPage = orderService.findOrdersWithSearch(
            user, status, dateFrom, dateTo, search, PageRequest.of(page, 10, sort)
        );

        // Calculate statistics
        Map<String, Object> statistics = orderService.getOrderStatistics(user);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("statistics", statistics);

        // Preserve filter parameters for pagination links
        model.addAttribute("paramStatus", status);
        model.addAttribute("paramDateFrom", dateFrom);
        model.addAttribute("paramDateTo", dateTo);
        model.addAttribute("paramSearch", search);

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
            // Verify the order belongs to the user first
            Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
            if (orderOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng không tồn tại hoặc không thuộc về bạn");
                return "redirect:/buyer/orders";
            }

            // Verify the book exists in the order
            Order order = orderOpt.get();
            boolean bookInOrder = order.getOrderItems().stream()
                    .anyMatch(item -> item.getBook() != null && item.getBook().getBookId().equals(bookId));

            if (!bookInOrder) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại trong đơn hàng này");
                return "redirect:/buyer/orders/" + orderId;
            }

            cartService.addBookToCart(user, bookId, quantity);
            redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được thêm vào giỏ hàng!");
        } catch (Exception e) {
            logger.error("Error rebuying item - orderId: {}, bookId: {}, error: {}", orderId, bookId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Không thể thêm sản phẩm: " + e.getMessage());
            return "redirect:/buyer/orders/" + orderId;
        }

        return "redirect:/buyer/cart";
    }

    @GetMapping("/order-success")
    public String orderSuccessPage(@RequestParam(required = false) Integer customerOrderId,
                                 @RequestParam(required = false) Integer orderId,
                                 Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CustomerOrder customerOrder = null;

        // Handle both customerOrderId and orderId parameters
        if (customerOrderId != null) {
            // Direct customer order lookup
            Optional<CustomerOrder> customerOrderOpt = customerOrderService.findByIdAndUser(customerOrderId, user);
            if (customerOrderOpt.isEmpty()) {
                return "redirect:/buyer/orders";
            }
            customerOrder = customerOrderOpt.get();
        } else if (orderId != null) {
            // Lookup via individual order
            Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
            if (orderOpt.isEmpty()) {
                return "redirect:/buyer/orders";
            }
            customerOrder = orderOpt.get().getCustomerOrder();
            if (customerOrder == null) {
                return "redirect:/buyer/orders";
            }
        } else {
            // No valid parameters provided
            return "redirect:/buyer/orders";
        }

        model.addAttribute("customerOrder", customerOrder);
        return "buyer/order-success";
    }

    /**
     * Cancel an order
     */
    @PostMapping("/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelOrder(@RequestParam Integer orderId,
                                                          @RequestParam String reason,
                                                          Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để thực hiện chức năng này");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean success = orderService.cancelOrderWithReason(orderId, reason, user);

            if (success) {
                response.put("success", true);
                response.put("message", "Đơn hàng đã được hủy thành công");
            } else {
                response.put("success", false);
                response.put("message", "Không thể hủy đơn hàng này");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error cancelling order: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi hủy đơn hàng");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Rebuy items from a delivered order
     */
    @PostMapping("/rebuy")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rebuyOrder(@RequestParam Integer orderId,
                                                         Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để thực hiện chức năng này");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean success = orderService.rebuyOrder(orderId, user);

            if (success) {
                response.put("success", true);
                response.put("message", "Sản phẩm đã được thêm vào giỏ hàng");
                response.put("redirectUrl", "/buyer/cart");
            } else {
                response.put("success", false);
                response.put("message", "Không thể mua lại đơn hàng này");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error rebuying order: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi mua lại đơn hàng");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Confirm delivery of an order (buyer confirms they received the order)
     */
    @PostMapping("/orders/{orderId}/confirm-delivery")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmDelivery(@PathVariable Integer orderId,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để thực hiện chức năng này");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
            if (orderOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Đơn hàng không tồn tại hoặc không thuộc về bạn");
                return ResponseEntity.badRequest().body(response);
            }

            Order order = orderOpt.get();

            // Check if order is in SHIPPED status
            if (order.getOrderStatus() != OrderStatus.SHIPPED) {
                response.put("success", false);
                response.put("message", "Chỉ có thể xác nhận nhận hàng cho đơn hàng đã được giao");
                return ResponseEntity.badRequest().body(response);
            }

            // Update order status to DELIVERED
            boolean success = orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);

            if (success) {
                response.put("success", true);
                response.put("message", "Đã xác nhận nhận hàng thành công");
                logger.info("User {} confirmed delivery for order {}", user.getEmail(), orderId);
            } else {
                response.put("success", false);
                response.put("message", "Không thể cập nhật trạng thái đơn hàng");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error confirming delivery for order {}: {}", orderId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xác nhận nhận hàng");
            return ResponseEntity.status(500).body(response);
        }
    }
}