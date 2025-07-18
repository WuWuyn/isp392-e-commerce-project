package com.example.isp392.controller.buyer;

import com.example.isp392.dto.OrderDTO;
import com.example.isp392.model.*;
import com.example.isp392.service.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/buyer")
public class ProcessCheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessCheckoutController.class);

    private final UserService userService;
    private final UserAddressService userAddressService;
    private final OrderService orderService;
    private final CartService cartService;
    private final PromotionService promotionService;
    private final VNPayService vnPayService;

    public ProcessCheckoutController(UserService userService,
                                     UserAddressService userAddressService,
                                     OrderService orderService,
                                     CartService cartService,
                                     PromotionService promotionService,
                                     VNPayService vnPayService) {
        this.userService = userService;
        this.userAddressService = userAddressService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.promotionService = promotionService;
        this.vnPayService = vnPayService;
    }

    @PostMapping("/process-checkout")
    public String processCheckout(@ModelAttribute OrderDTO orderDTO,
                                  Authentication authentication,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        logger.info("Processing checkout for order: {}", orderDTO);
        logger.info("Payment method: {}", orderDTO.getPaymentMethod());
        logger.info("Existing address ID: {}", orderDTO.getExistingAddressId());

        if (authentication == null) {
            logger.warn("Authentication is null, redirecting to login");
            return "redirect:/login";
        }

        try {
            // Get current user
            String email = authentication.getName();
            User user = userService.findByEmail(email).orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

            // Get selected items from session
            @SuppressWarnings("unchecked")
            List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("checkoutItems");
            logger.info("Selected items from session: {}", selectedItems != null ? selectedItems.size() : "null");
            if (selectedItems == null || selectedItems.isEmpty()) {
                logger.warn("No selected items found in session, redirecting to cart");
                return "redirect:/buyer/cart?error=no_items";
            }

            // Create new order
            Order order = new Order();
            order.setUser(user);
            order.setOrderDate(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.PENDING);

            // Set payment information
            PaymentMethod paymentMethod = PaymentMethod.valueOf(orderDTO.getPaymentMethod());
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(PaymentStatus.PENDING);

            // Set order notes
            order.setNotes(orderDTO.getNotes());

            // Process shipping address
            // Check if an existing address is selected or if we should use default address
            if (orderDTO.getExistingAddressId() != null && !orderDTO.getExistingAddressId().isEmpty() && !orderDTO.getExistingAddressId().equals("new")) {
                // Use existing address
                UserAddress address = userAddressService.findAddressById(Integer.valueOf(orderDTO.getExistingAddressId()))
                        .orElseThrow(() -> new IllegalArgumentException("Address not found"));

                order.setRecipientName(address.getRecipientName());
                order.setRecipientPhone(address.getRecipientPhone());
                order.setShippingProvince(address.getProvince());
                order.setShippingDistrict(address.getDistrict());
                order.setShippingWard(address.getWard());
                order.setShippingAddressDetail(address.getAddressDetail());
                order.setShippingCompany(address.getCompany());
                order.setShippingAddressType(address.getAddress_type());
            } else if (orderDTO.getExistingAddressId() == null || orderDTO.getExistingAddressId().isEmpty()) {
                // No address specified, try to use default address
                UserAddress defaultAddress = userAddressService.findDefaultAddress(user);
                if (defaultAddress != null) {
                    order.setRecipientName(defaultAddress.getRecipientName());
                    order.setRecipientPhone(defaultAddress.getRecipientPhone());
                    order.setShippingProvince(defaultAddress.getProvince());
                    order.setShippingDistrict(defaultAddress.getDistrict());
                    order.setShippingWard(defaultAddress.getWard());
                    order.setShippingAddressDetail(defaultAddress.getAddressDetail());
                    order.setShippingCompany(defaultAddress.getCompany());
                    order.setShippingAddressType(defaultAddress.getAddress_type());
                } else {
                    // No default address, use new address from form
                    order.setRecipientName(orderDTO.getRecipientName());
                    order.setRecipientPhone(orderDTO.getRecipientPhone());
                    order.setShippingProvince(orderDTO.getProvince());
                    order.setShippingDistrict(orderDTO.getDistrict());
                    order.setShippingWard(orderDTO.getWard());
                    order.setShippingAddressDetail(orderDTO.getAddressDetail());
                    order.setShippingCompany(orderDTO.getCompany());
                    order.setShippingAddressType(orderDTO.getAddressType());
                }
            } else {
                // Use new address
                order.setRecipientName(orderDTO.getRecipientName());
                order.setRecipientPhone(orderDTO.getRecipientPhone());
                order.setShippingProvince(orderDTO.getProvince());
                order.setShippingDistrict(orderDTO.getDistrict());
                order.setShippingWard(orderDTO.getWard());
                order.setShippingAddressDetail(orderDTO.getAddressDetail());
                order.setShippingCompany(orderDTO.getCompany());
                order.setShippingAddressType(orderDTO.getAddressType());

                // Save address if requested
                if (Boolean.TRUE.equals(orderDTO.getSaveAddress())) {
                    UserAddress newAddress = new UserAddress();
                    newAddress.setUser(user);
                    newAddress.setRecipientName(orderDTO.getRecipientName());
                    newAddress.setRecipientPhone(orderDTO.getRecipientPhone());
                    newAddress.setProvince(orderDTO.getProvince());
                    newAddress.setDistrict(orderDTO.getDistrict());
                    newAddress.setWard(orderDTO.getWard());
                    newAddress.setAddressDetail(orderDTO.getAddressDetail());
                    newAddress.setCompany(orderDTO.getCompany());
                    newAddress.setAddress_type(orderDTO.getAddressType());
                    newAddress.setDefault(false); // Not setting as default automatically

                    userAddressService.saveAddress(newAddress);
                }
            }

            // Set shop for the order (use the shop from the first cart item)
            // For individual orders, all items should be from the same shop
            if (!selectedItems.isEmpty()) {
                Shop orderShop = selectedItems.get(0).getBook().getShop();
                order.setShop(orderShop);
                logger.info("Setting order shop to: {}", orderShop.getShopName());
            }

            // Calculate order totals
            BigDecimal subtotal = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartItem item : selectedItems) {
                // Validate inventory before creating order item
                if (item.getQuantity() > item.getBook().getStockQuantity()) {
                    logger.error("Insufficient stock for book ID: {}. Requested: {}, Available: {}",
                               item.getBook().getBookId(), item.getQuantity(), item.getBook().getStockQuantity());
                    redirectAttributes.addFlashAttribute("error",
                        "Sản phẩm '" + item.getBook().getTitle() + "' không đủ số lượng trong kho");
                    return "redirect:/buyer/cart";
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setBook(item.getBook());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(item.getBook().getSellingPrice());
                orderItem.setSubtotal(
                        item.getBook().getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );

                orderItems.add(orderItem);
                subtotal = subtotal.add(orderItem.getSubtotal());

                logger.info("Added order item - Book: {}, Quantity: {}, Unit Price: {}, Subtotal: {}",
                           item.getBook().getTitle(), item.getQuantity(),
                           orderItem.getUnitPrice(), orderItem.getSubtotal());
            }

            order.setOrderItems(orderItems);
            order.setSubTotal(subtotal);

            // Set shipping fee (fixed 30,000 VND)
            BigDecimal shippingFee = new BigDecimal(30000);
            order.setShippingFee(shippingFee);

            // Apply discount if applicable
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (orderDTO.getDiscountCode() != null && !orderDTO.getDiscountCode().isEmpty()) {
                try {
                    orderService.applyPromotion(order, orderDTO.getDiscountCode());
                    discountAmount = order.getDiscountAmount();
                } catch (Exception e) {
                    logger.warn("Failed to apply promotion: {}", e.getMessage());
                    // Continue without applying promotion
                    order.setDiscountAmount(BigDecimal.ZERO);
                }
            } else {
                order.setDiscountAmount(BigDecimal.ZERO);
            }

            // Calculate total amount
            BigDecimal totalAmount = subtotal.add(shippingFee).subtract(order.getDiscountAmount());
            order.setTotalAmount(totalAmount);

            // Save order
            logger.info("Attempting to save order for user: {}", user.getEmail());
            Order savedOrder = orderService.save(order);
            logger.info("Order saved successfully with ID: {}", savedOrder.getOrderId());

            // Remove items from cart only if not a Buy Now order
            Boolean isBuyNow = (Boolean) session.getAttribute("isBuyNow");
            if (!Boolean.TRUE.equals(isBuyNow)) {
                logger.info("Removing {} items from cart after successful order", selectedItems.size());
                try {
                    for (CartItem item : selectedItems) {
                        cartService.removeItem(user, item.getBook().getBookId());
                        logger.debug("Removed book ID {} from cart", item.getBook().getBookId());
                    }
                    logger.info("Successfully removed all items from cart");
                } catch (Exception e) {
                    logger.warn("Failed to remove some items from cart: {}", e.getMessage());
                    // Don't fail the order if cart cleanup fails
                }
            }

            // Clear session data
            session.removeAttribute("checkoutItems");
            session.removeAttribute("buyNowSession");
            session.removeAttribute("isBuyNow");
            session.removeAttribute("discountCode");
            session.removeAttribute("discountAmount");

            // Redirect to success page
            return "redirect:/buyer/individual-order-success?orderId=" + savedOrder.getOrderId();

        } catch (Exception e) {
            logger.error("Error processing checkout: {}", e.getMessage(), e);

            // Add user-friendly error message
            String errorMessage = e.getMessage();
            if (errorMessage.contains("không đủ số lượng") || errorMessage.contains("Insufficient")) {
                errorMessage = "Một số sản phẩm trong giỏ hàng đã hết hàng. Vui lòng kiểm tra lại giỏ hàng.";
            } else if (errorMessage.contains("Đặt hàng thất bại")) {
                // Keep the original message as it's already user-friendly
            } else {
                errorMessage = "Có lỗi xảy ra khi đặt hàng. Vui lòng thử lại sau.";
            }

            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return "redirect:/buyer/checkout?error";
        }
    }

    @PostMapping("/vnpay-payment")
    public String processVNPayPayment(@ModelAttribute OrderDTO orderDTO,
                                      Authentication authentication,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {

        logger.info("Processing VNPay payment for order: {}", orderDTO);

        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // Get current user
            String email = authentication.getName();
            User user = userService.findByEmail(email).orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

            // Get selected items from session
            @SuppressWarnings("unchecked")
            List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("checkoutItems");
            if (selectedItems == null || selectedItems.isEmpty()) {
                return "redirect:/buyer/cart?error=no_items";
            }

            // Create new order
            Order order = new Order();
            order.setUser(user);
            order.setOrderDate(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.PENDING);

            // Set payment information for VNPay
            order.setPaymentMethod(PaymentMethod.VNPAY);
            order.setPaymentStatus(PaymentStatus.PENDING);

            // Set order notes
            order.setNotes(orderDTO.getNotes());

            // Process shipping address (same logic as COD)
            processShippingAddress(order, orderDTO, user);

            // Calculate order totals
            BigDecimal subtotal = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartItem item : selectedItems) {
                // Validate inventory before creating order item
                if (item.getQuantity() > item.getBook().getStockQuantity()) {
                    logger.error("Insufficient stock for book ID: {} in VNPay checkout. Requested: {}, Available: {}",
                               item.getBook().getBookId(), item.getQuantity(), item.getBook().getStockQuantity());
                    redirectAttributes.addFlashAttribute("error",
                        "Sản phẩm '" + item.getBook().getTitle() + "' không đủ số lượng trong kho");
                    return "redirect:/buyer/cart";
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setBook(item.getBook());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(item.getBook().getSellingPrice());
                orderItem.setSubtotal(
                        item.getBook().getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );

                orderItems.add(orderItem);
                subtotal = subtotal.add(orderItem.getSubtotal());

                logger.info("Added VNPay order item - Book: {}, Quantity: {}, Unit Price: {}, Subtotal: {}",
                           item.getBook().getTitle(), item.getQuantity(),
                           orderItem.getUnitPrice(), orderItem.getSubtotal());
            }

            order.setOrderItems(orderItems);
            order.setSubTotal(subtotal);

            // Set shipping fee (fixed 30,000 VND)
            BigDecimal shippingFee = new BigDecimal(30000);
            order.setShippingFee(shippingFee);

            // Apply discount if applicable
            if (orderDTO.getDiscountCode() != null && !orderDTO.getDiscountCode().isEmpty()) {
                try {
                    orderService.applyPromotion(order, orderDTO.getDiscountCode());
                } catch (Exception e) {
                    logger.warn("Failed to apply promotion: {}", e.getMessage());
                    order.setDiscountAmount(BigDecimal.ZERO);
                }
            } else {
                order.setDiscountAmount(BigDecimal.ZERO);
            }

            // Calculate total amount
            BigDecimal totalAmount = subtotal.add(shippingFee).subtract(order.getDiscountAmount());
            order.setTotalAmount(totalAmount);

            // Save order first to get order ID
            Order savedOrder = orderService.save(order);

            // Create VNPay payment URL
            String paymentUrl = vnPayService.createPaymentUrl(savedOrder);

            // Store order info in session for callback processing
            session.setAttribute("pendingOrderId", savedOrder.getOrderId());
            session.setAttribute("pendingOrderItems", selectedItems);

            // Redirect to VNPay payment page
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            logger.error("Error processing VNPay payment: {}", e.getMessage(), e);

            // Add user-friendly error message
            String errorMessage = e.getMessage();
            if (errorMessage.contains("không đủ số lượng") || errorMessage.contains("Insufficient")) {
                errorMessage = "Một số sản phẩm trong giỏ hàng đã hết hàng. Vui lòng kiểm tra lại giỏ hàng.";
            } else if (errorMessage.contains("Đặt hàng thất bại")) {
                // Keep the original message as it's already user-friendly
            } else {
                errorMessage = "Có lỗi xảy ra khi tạo thanh toán. Vui lòng thử lại sau.";
            }

            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return "redirect:/buyer/checkout?error";
        }
    }

    private void processShippingAddress(Order order, OrderDTO orderDTO, User user) {
        // Check if an existing address is selected or if we should use default address
        if (orderDTO.getExistingAddressId() != null && !orderDTO.getExistingAddressId().isEmpty() && !orderDTO.getExistingAddressId().equals("new")) {
            // Use existing address
            UserAddress address = userAddressService.findAddressById(Integer.valueOf(orderDTO.getExistingAddressId()))
                    .orElseThrow(() -> new IllegalArgumentException("Address not found"));

            order.setRecipientName(address.getRecipientName());
            order.setRecipientPhone(address.getRecipientPhone());
            order.setShippingProvince(address.getProvince());
            order.setShippingDistrict(address.getDistrict());
            order.setShippingWard(address.getWard());
            order.setShippingAddressDetail(address.getAddressDetail());
            order.setShippingCompany(address.getCompany());
            order.setShippingAddressType(address.getAddress_type());
        } else if (orderDTO.getExistingAddressId() == null || orderDTO.getExistingAddressId().isEmpty()) {
            // No address specified, try to use default address
            UserAddress defaultAddress = userAddressService.findDefaultAddress(user);
            if (defaultAddress != null) {
                order.setRecipientName(defaultAddress.getRecipientName());
                order.setRecipientPhone(defaultAddress.getRecipientPhone());
                order.setShippingProvince(defaultAddress.getProvince());
                order.setShippingDistrict(defaultAddress.getDistrict());
                order.setShippingWard(defaultAddress.getWard());
                order.setShippingAddressDetail(defaultAddress.getAddressDetail());
                order.setShippingCompany(defaultAddress.getCompany());
                order.setShippingAddressType(defaultAddress.getAddress_type());
            } else {
                // No default address, use new address from form
                setNewAddressToOrder(order, orderDTO);
            }
        } else {
            // Use new address
            setNewAddressToOrder(order, orderDTO);

            // Save address if requested
            if (Boolean.TRUE.equals(orderDTO.getSaveAddress())) {
                UserAddress newAddress = new UserAddress();
                newAddress.setUser(user);
                newAddress.setRecipientName(orderDTO.getRecipientName());
                newAddress.setRecipientPhone(orderDTO.getRecipientPhone());
                newAddress.setProvince(orderDTO.getProvince());
                newAddress.setDistrict(orderDTO.getDistrict());
                newAddress.setWard(orderDTO.getWard());
                newAddress.setAddressDetail(orderDTO.getAddressDetail());
                newAddress.setCompany(orderDTO.getCompany());
                newAddress.setAddress_type(orderDTO.getAddressType());
                newAddress.setDefault(false);

                userAddressService.saveAddress(newAddress);
            }
        }
    }

    private void setNewAddressToOrder(Order order, OrderDTO orderDTO) {
        order.setRecipientName(orderDTO.getRecipientName());
        order.setRecipientPhone(orderDTO.getRecipientPhone());
        order.setShippingProvince(orderDTO.getProvince());
        order.setShippingDistrict(orderDTO.getDistrict());
        order.setShippingWard(orderDTO.getWard());
        order.setShippingAddressDetail(orderDTO.getAddressDetail());
        order.setShippingCompany(orderDTO.getCompany());
        order.setShippingAddressType(orderDTO.getAddressType());
    }

    @GetMapping("/individual-order-success")
    public String individualOrderSuccessPage(@RequestParam Integer orderId, Model model, Authentication authentication) {
        logger.info("Order success page requested for orderId: {}", orderId);

        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // Get current user
            String email = authentication.getName();
            User user = userService.findByEmail(email).orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

            // Get order by ID and verify it belongs to the user
            Optional<Order> orderOpt = orderService.findByIdAndUser(orderId, user);
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found or doesn't belong to user: orderId={}, userEmail={}", orderId, email);
                return "redirect:/buyer/orders";
            }

            Order order = orderOpt.get();
            model.addAttribute("order", order);
            logger.info("Order success page loaded successfully for orderId: {}", orderId);
            return "buyer/order-success";

        } catch (Exception e) {
            logger.error("Error loading order success page: {}", e.getMessage(), e);
            return "redirect:/buyer/orders";
        }
    }

    @GetMapping("/vnpay-return")
    public String handleVNPayReturn(@RequestParam Map<String, String> queryParams,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {

        logger.info("VNPay return with params: {}", queryParams);

        try {
            // Validate VNPay response
            if (!vnPayService.validatePaymentResponse(queryParams)) {
                logger.error("Invalid VNPay response");
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid payment response");
                return "redirect:/buyer/cart?error=payment_failed";
            }

            String vnpayResponseCode = queryParams.get("vnp_ResponseCode");
            String orderId = queryParams.get("vnp_TxnRef");

            if (orderId == null) {
                logger.error("Order ID not found in VNPay response");
                redirectAttributes.addFlashAttribute("errorMessage", "Order ID not found");
                return "redirect:/buyer/cart?error=payment_failed";
            }

            // Get order from database
            Optional<Order> orderOpt = orderService.findOrderById(Integer.parseInt(orderId));
            if (orderOpt.isEmpty()) {
                logger.error("Order not found: {}", orderId);
                redirectAttributes.addFlashAttribute("errorMessage", "Order not found");
                return "redirect:/buyer/cart?error=payment_failed";
            }

            Order order = orderOpt.get();

            if ("00".equals(vnpayResponseCode)) {
                // Payment successful
                logger.info("VNPay payment successful for order: {}", orderId);

                // Update order payment status
                order.setPaymentStatus(PaymentStatus.PAID);
                orderService.save(order);

                // Remove items from cart if not a Buy Now order
                @SuppressWarnings("unchecked")
                List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("pendingOrderItems");
                Boolean isBuyNow = (Boolean) session.getAttribute("isBuyNow");

                if (selectedItems != null && !Boolean.TRUE.equals(isBuyNow)) {
                    User user = order.getUser();
                    for (CartItem item : selectedItems) {
                        cartService.removeItem(user, item.getBook().getBookId());
                    }
                }

                // Clear session data
                session.removeAttribute("checkoutItems");
                session.removeAttribute("buyNowSession");
                session.removeAttribute("isBuyNow");
                session.removeAttribute("discountCode");
                session.removeAttribute("discountAmount");
                session.removeAttribute("pendingOrderId");
                session.removeAttribute("pendingOrderItems");

                // Redirect to success page
                return "redirect:/buyer/individual-order-success?orderId=" + order.getOrderId();

            } else {
                // Payment failed
                logger.warn("VNPay payment failed for order: {}, response code: {}", orderId, vnpayResponseCode);

                // Update order payment status
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setOrderStatus(OrderStatus.CANCELLED);
                orderService.save(order);

                // Return inventory
                for (OrderItem item : order.getOrderItems()) {
                    // Note: You might need to implement a method to return inventory
                    // bookService.increaseStockQuantity(item.getBook().getBookId(), item.getQuantity());
                }

                redirectAttributes.addFlashAttribute("errorMessage", "Payment failed. Please try again.");
                return "redirect:/buyer/cart?error=payment_failed";
            }

        } catch (Exception e) {
            logger.error("Error handling VNPay return: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Payment processing error");
            return "redirect:/buyer/cart?error=payment_failed";
        }
    }
}