package com.example.isp392.controller;

import com.example.isp392.model.*;
import com.example.isp392.service.CartService;
import com.example.isp392.service.CustomerOrderService;
import com.example.isp392.service.OrderService;
import com.example.isp392.service.UserService;
import com.example.isp392.service.BookReviewService;
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
    private final CustomerOrderService customerOrderService;
    private final BookReviewService bookReviewService;

    public OrderController(OrderService orderService, CartService cartService, UserService userService, CustomerOrderService customerOrderService, BookReviewService bookReviewService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
        this.customerOrderService = customerOrderService;
        this.bookReviewService = bookReviewService;
    }

    @GetMapping("/orders")
    public String getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            Model model,
            Authentication authentication) {

        User user = getCurrentUser(authentication); // Sử dụng helper method
        if (user == null) {
            return "redirect:/buyer/login";
        }

        Page<Order> orderPage = orderService.findOrders(
                user, status, dateFrom, dateTo, PageRequest.of(page, 10) // Giả sử 1 trang 10 đơn hàng
        );

        // **PHẦN THÊM VÀO TỪ PHƯƠNG THỨC CŨ**
        // Lấy danh sách ID các sản phẩm đã được đánh giá
        model.addAttribute("reviewedItemIds", bookReviewService.getReviewedItemIdsForUser(user));

        // Phần còn lại giữ nguyên
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());

        // Giữ lại các tham số lọc để dùng cho các liên kết phân trang
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
    public String orderSuccessPage(@RequestParam Integer customerOrderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<CustomerOrder> customerOrderOpt = customerOrderService.findByIdAndUser(customerOrderId, user);
        if (customerOrderOpt.isEmpty()) {
            return "redirect:/buyer/orders";
        }

        model.addAttribute("customerOrder", customerOrderOpt.get());
        return "buyer/order-success";
    }

    @GetMapping("/orders/review/{orderItemId}")
    public String showReviewForm(@PathVariable("orderItemId") Integer orderItemId, Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) return "redirect:/buyer/login";

        Optional<OrderItem> orderItemOpt = orderService.findOrderItemById(orderItemId);

        // SỬA LỖI Ở ĐÂY
        if (orderItemOpt.isEmpty() || !orderItemOpt.get().getOrder().getCustomerOrder().getUser().equals(currentUser)) {
            return "redirect:/buyer/orders?error=Invalid item";
        }

        model.addAttribute("review", bookReviewService.getOrCreateReview(currentUser, orderItemId));
        model.addAttribute("isEditing", bookReviewService.isExistingReview(currentUser, orderItemId));
        model.addAttribute("orderItem", orderItemOpt.get());

        return "buyer/review-form";
    }

    @PostMapping("/orders/review")
    public String submitReview(@ModelAttribute BookReview review,
                               @RequestParam("orderItemId") Integer orderItemId,
                               RedirectAttributes redirectAttributes, Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) return "redirect:/buyer/login";

        Optional<OrderItem> orderItemOpt = orderService.findOrderItemById(orderItemId);

        // SỬA LỖI Ở ĐÂY
        if (orderItemOpt.isEmpty() || !orderItemOpt.get().getOrder().getCustomerOrder().getUser().equals(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "An error occurred. Invalid item.");
            return "redirect:/buyer/orders";
        }

        boolean isEditing = bookReviewService.isExistingReview(currentUser, orderItemId);
        bookReviewService.saveOrUpdateReview(review, currentUser, orderItemOpt.get());

        String message = isEditing ? "Your review has been updated successfully!" : "Thank you for your review!";
        redirectAttributes.addFlashAttribute("success", message);

        return "redirect:/buyer/orders";
    }

    // Phương thức này không cần sửa, giữ nguyên
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        return userService.findByEmail(email).orElse(null);
    }

    // Phương thức này không cần sửa, giữ nguyên
    @PostMapping("/orders/review/delete")
    public String deleteReview(@RequestParam("reviewId") Integer reviewId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/buyer/login";
        }

        boolean isDeleted = bookReviewService.deleteReview(reviewId, currentUser);

        if (isDeleted) {
            redirectAttributes.addFlashAttribute("success", "Your review has been deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete the review. It may not exist or you do not have permission.");
        }

        return "redirect:/buyer/orders";
    }
}