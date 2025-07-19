package com.example.isp392.controller;

import com.example.isp392.model.*;
import com.example.isp392.service.CartService;
import com.example.isp392.service.OrderService;
import com.example.isp392.service.UserService;
import com.example.isp392.service.BookReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/buyer")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;
    private final BookReviewService bookReviewService;

    public OrderController(OrderService orderService, CartService cartService, UserService userService, BookReviewService bookReviewService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
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
        
        model.addAttribute("order", orderOpt.get());
        return "buyer/order-detail";
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
        // Thêm các sản phẩm từ đơn hàng vào giỏ hàng
        order.getOrderItems().forEach(item -> {
            cartService.addBookToCart(user, item.getBook().getBook_id(), item.getQuantity());
        });
        
        redirectAttributes.addFlashAttribute("success", "Đã thêm các sản phẩm vào giỏ hàng");
        return "redirect:/buyer/cart";
    }

    @PostMapping("/orders/{orderId}/items/{bookId}/rebuy")
    public String rebuyOrderItem(@PathVariable Integer orderId,
                                 @PathVariable Integer bookId,
                                 @RequestParam Integer quantity,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Optional: Verify if the order belongs to the user and contains the item
        // For simplicity, we are directly adding to cart, assuming frontend validation/user context
        
        cartService.addBookToCart(user, bookId, quantity);
        
        redirectAttributes.addFlashAttribute("success", "Item added to cart successfully!");
        return "redirect:/buyer/cart";
    }

    @PostMapping("/checkout")
    public String checkout(
            @RequestParam(required = false) String promotionCode,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Cart cart = cartService.getCart(user);
            if (cart.getItems().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
                return "redirect:/buyer/cart";
            }
            
            // Kiểm tra số lượng sách trong kho trước khi thanh toán
            StringBuilder invalidItems = new StringBuilder();
            boolean hasStockIssue = false;
            
            for (CartItem item : cart.getItems()) {
                Book book = item.getBook();
                if (book.getStockQuantity() == null || book.getStockQuantity() < item.getQuantity()) {
                    hasStockIssue = true;
                    invalidItems.append("- ").append(book.getTitle())
                              .append(": Số lượng trong kho (")
                              .append(book.getStockQuantity() != null ? book.getStockQuantity() : 0)
                              .append(") không đủ để đáp ứng yêu cầu (")
                              .append(item.getQuantity()).append(")\n");
                }
            }
            
            if (hasStockIssue) {
                redirectAttributes.addFlashAttribute("error", 
                    "Một số sản phẩm trong giỏ hàng không có đủ số lượng:\n" + invalidItems.toString());
                return "redirect:/buyer/cart";
            }

            // Create new order
            Order order = new Order();
            order.setUser(user);
            order.setOrderDate(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.PENDING);
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setNotes(notes);

            // Calculate totals
            BigDecimal subTotal = cartService.getCartTotal(user);
            order.setSubTotal(subTotal);
            
            // Set shipping fee (default to 0 as requested)
            order.setShippingFee(BigDecimal.ZERO);

            // Apply promotion if provided
            if (promotionCode != null && !promotionCode.isEmpty()) {
                orderService.applyPromotion(order, promotionCode);
            } else {
                order.setDiscountAmount(BigDecimal.ZERO);
            }

            // Calculate final total
            order.setTotalAmount(subTotal.add(order.getShippingFee()).subtract(order.getDiscountAmount()));

            // Transfer items from cart to order
            cartService.transferCartItemsToOrder(cart, order);

            // Save order
            orderService.save(order);

            // Clear cart
            cartService.clearCart(cart);

            // Redirect to success page with a simpler URL pattern
            return "redirect:/buyer/order-success?orderId=" + order.getOrderId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/buyer/cart";
        }
    }

    @GetMapping("/order-success")
    public String orderSuccessPage(@RequestParam Integer orderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
        if (orderOpt.isEmpty()) {
            return "redirect:/buyer/orders";
        }

        model.addAttribute("order", orderOpt.get());
        return "buyer/order-success";
    }

    @GetMapping("/orders/{orderId}/success")
    public String orderSuccess(@PathVariable Integer orderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
        if (orderOpt.isEmpty()) {
            return "redirect:/buyer/orders";
        }

        model.addAttribute("order", orderOpt.get());
        return "buyer/order-success";
    }

    @GetMapping("/orders/review/{orderItemId}")
    public String showReviewForm(@PathVariable("orderItemId") Integer orderItemId, Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) return "redirect:/buyer/login";

        Optional<OrderItem> orderItemOpt = orderService.findOrderItemById(orderItemId);
        if (orderItemOpt.isEmpty() || !orderItemOpt.get().getOrder().getUser().equals(currentUser)) {
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
        if (orderItemOpt.isEmpty() || !orderItemOpt.get().getOrder().getUser().equals(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "An error occurred. Invalid item.");
            return "redirect:/buyer/orders";
        }

        boolean isEditing = bookReviewService.isExistingReview(currentUser, orderItemId);
        bookReviewService.saveOrUpdateReview(review, currentUser, orderItemOpt.get());

        String message = isEditing ? "Your review has been updated successfully!" : "Thank you for your review!";
        redirectAttributes.addFlashAttribute("success", message);

        return "redirect:/buyer/orders";
    }


    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        return userService.findByEmail(email).orElse(null);
    }

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