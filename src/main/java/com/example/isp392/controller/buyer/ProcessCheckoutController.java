package com.example.isp392.controller.buyer;

import com.example.isp392.dto.CartItemDTO;
import com.example.isp392.dto.OrderDTO;
import com.example.isp392.model.*;
import com.example.isp392.service.*;
import com.example.isp392.service.InventoryReservationService;
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
import java.util.stream.Collectors;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/buyer")
public class ProcessCheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessCheckoutController.class);

    private final UserService userService;
    private final UserAddressService userAddressService;
    private final OrderService orderService;
    private final CustomerOrderService customerOrderService;
    private final CartService cartService;
    private final PromotionService promotionService;
    private final VNPayService vnPayService;
    private final BookService bookService;
    private final InventoryReservationService inventoryReservationService;
    private final PaymentReservationService paymentReservationService;

    public ProcessCheckoutController(UserService userService,
                                     UserAddressService userAddressService,
                                     OrderService orderService,
                                     CustomerOrderService customerOrderService,
                                     CartService cartService,
                                     PromotionService promotionService,
                                     VNPayService vnPayService,
                                     BookService bookService,
                                     InventoryReservationService inventoryReservationService,
                                     PaymentReservationService paymentReservationService) {
        this.userService = userService;
        this.userAddressService = userAddressService;
        this.orderService = orderService;
        this.customerOrderService = customerOrderService;
        this.cartService = cartService;
        this.promotionService = promotionService;
        this.vnPayService = vnPayService;
        this.bookService = bookService;
        this.inventoryReservationService = inventoryReservationService;
        this.paymentReservationService = paymentReservationService;
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

            logger.info("COD: Session checkout items: {}", selectedItems != null ? selectedItems.size() : "null");
            if (selectedItems != null) {
                for (CartItem item : selectedItems) {
                    logger.info("COD: Cart item - Book: {}, Shop: {}, Quantity: {}",
                               item.getBook() != null ? item.getBook().getTitle() : "null",
                               item.getShop() != null ? item.getShop().getShopName() : "null",
                               item.getQuantity());
                }
            }

            if (selectedItems == null || selectedItems.isEmpty()) {
                logger.warn("No selected items found in session, redirecting to cart");
                return "redirect:/buyer/cart?error=no_items";
            }

            // Create new customer order
            CustomerOrder customerOrder = new CustomerOrder();
            customerOrder.setUser(user);
            customerOrder.setCreatedAt(LocalDateTime.now());
            customerOrder.setStatus(OrderStatus.PROCESSING);

            // Set payment information
            PaymentMethod paymentMethod = PaymentMethod.valueOf(orderDTO.getPaymentMethod());
            customerOrder.setPaymentMethod(paymentMethod);
            customerOrder.setPaymentStatus(PaymentStatus.PENDING);

            // Set order notes
            customerOrder.setNotes(orderDTO.getNotes());

            // Process shipping address
            // Check if an existing address is selected or if we should use default address
            if (orderDTO.getExistingAddressId() != null && !orderDTO.getExistingAddressId().isEmpty() && !orderDTO.getExistingAddressId().equals("new")) {
                // Use existing address
                UserAddress address = userAddressService.findAddressById(Integer.valueOf(orderDTO.getExistingAddressId()))
                        .orElseThrow(() -> new IllegalArgumentException("Address not found"));

                customerOrder.setRecipientName(address.getRecipientName());
                customerOrder.setRecipientPhone(address.getRecipientPhone());
                customerOrder.setShippingProvince(address.getProvince());
                customerOrder.setShippingDistrict(address.getDistrict());
                customerOrder.setShippingWard(address.getWard());
                customerOrder.setShippingAddressDetail(address.getAddressDetail());
                customerOrder.setShippingCompany(address.getCompany());
                customerOrder.setShippingAddressType(address.getAddress_type());
            } else if (orderDTO.getExistingAddressId() == null || orderDTO.getExistingAddressId().isEmpty()) {
                // No address specified, try to use default address
                UserAddress defaultAddress = userAddressService.findDefaultAddress(user);
                if (defaultAddress != null) {
                    customerOrder.setRecipientName(defaultAddress.getRecipientName());
                    customerOrder.setRecipientPhone(defaultAddress.getRecipientPhone());
                    customerOrder.setShippingProvince(defaultAddress.getProvince());
                    customerOrder.setShippingDistrict(defaultAddress.getDistrict());
                    customerOrder.setShippingWard(defaultAddress.getWard());
                    customerOrder.setShippingAddressDetail(defaultAddress.getAddressDetail());
                    customerOrder.setShippingCompany(defaultAddress.getCompany());
                    customerOrder.setShippingAddressType(defaultAddress.getAddress_type());
                } else {
                    // No default address, use new address from form
                    customerOrder.setRecipientName(orderDTO.getRecipientName());
                    customerOrder.setRecipientPhone(orderDTO.getRecipientPhone());
                    customerOrder.setShippingProvince(orderDTO.getProvince());
                    customerOrder.setShippingDistrict(orderDTO.getDistrict());
                    customerOrder.setShippingWard(orderDTO.getWard());
                    customerOrder.setShippingAddressDetail(orderDTO.getAddressDetail());
                    customerOrder.setShippingCompany(orderDTO.getCompany());
                    customerOrder.setShippingAddressType(orderDTO.getAddressType());
                }
            } else {
                // Use new address
                customerOrder.setRecipientName(orderDTO.getRecipientName());
                customerOrder.setRecipientPhone(orderDTO.getRecipientPhone());
                customerOrder.setShippingProvince(orderDTO.getProvince());
                customerOrder.setShippingDistrict(orderDTO.getDistrict());
                customerOrder.setShippingWard(orderDTO.getWard());
                customerOrder.setShippingAddressDetail(orderDTO.getAddressDetail());
                customerOrder.setShippingCompany(orderDTO.getCompany());
                customerOrder.setShippingAddressType(orderDTO.getAddressType());

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

            // Save the customer order first
            customerOrder = customerOrderService.save(customerOrder);

            // Group cart items by shop
            Map<Integer, List<CartItem>> itemsByShop = selectedItems.stream()
                    .collect(Collectors.groupingBy(item -> item.getShop().getShopId()));

            logger.info("Grouped {} items into {} shops", selectedItems.size(), itemsByShop.size());

            List<Order> orders = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalShippingFee = BigDecimal.ZERO;
            BigDecimal totalDiscountAmount = BigDecimal.ZERO;

            // Create orders for each shop
            for (Map.Entry<Integer, List<CartItem>> shopEntry : itemsByShop.entrySet()) {
                Integer shopId = shopEntry.getKey();
                List<CartItem> shopItems = shopEntry.getValue();

                logger.info("Processing shop {} with {} items", shopId, shopItems.size());

                // Create order for this shop
                Order order = new Order();
                order.setOrderDate(LocalDateTime.now());
                order.setOrderStatus(OrderStatus.PROCESSING);
                order.setNotes(orderDTO.getNotes());

                // Set shop for the order
                Shop orderShop = shopItems.get(0).getShop();
                order.setShop(orderShop);
                logger.info("Setting order shop to: {}", orderShop.getShopName());

                // Calculate order totals for this shop
                BigDecimal subtotal = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();

                for (CartItem item : shopItems) {
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

                // Set shipping fee (fixed 30,000 VND per shop)
                BigDecimal shippingFee = new BigDecimal(30000);
                order.setShippingFee(shippingFee);

                // Apply discount if applicable (only to first shop for simplicity)
                BigDecimal discountAmount = BigDecimal.ZERO;
                if (orders.isEmpty() && orderDTO.getDiscountCode() != null && !orderDTO.getDiscountCode().isEmpty()) {
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

                // Calculate total amount for this order
                BigDecimal orderTotal = subtotal.add(shippingFee).subtract(order.getDiscountAmount());
                order.setTotalAmount(orderTotal);

                // Add order to customer order using helper method that maintains bidirectional relationship
                customerOrder.addOrder(order);

                // Save order
                logger.info("Attempting to save order for shop: {}", orderShop.getShopName());
                Order savedOrder = orderService.save(order);
                logger.info("Order saved successfully with ID: {} for shop: {}", savedOrder.getOrderId(), orderShop.getShopName());

                orders.add(savedOrder);
                totalAmount = totalAmount.add(orderTotal);
                totalShippingFee = totalShippingFee.add(shippingFee);
                totalDiscountAmount = totalDiscountAmount.add(order.getDiscountAmount());
            }

            // Update customer order totals with sum of all shop orders
            customerOrder.setTotalAmount(totalAmount);
            customerOrder.setShippingFee(totalShippingFee);
            customerOrder.setDiscountAmount(totalDiscountAmount);
            // Don't call setOrders() - orders are already added to the collection above
            customerOrder = customerOrderService.save(customerOrder);

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

            // Redirect to success page using the first order ID
            return "redirect:/buyer/individual-order-success?orderId=" + orders.get(0).getOrderId();

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

        logger.info("Processing VNPay payment reservation for order: {}", orderDTO);

        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // Get current user
            String email = authentication.getName();
            User user = userService.findByEmail(email).orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

            // Get selected items from session (same as COD flow)
            @SuppressWarnings("unchecked")
            List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("checkoutItems");

            logger.info("VNPay: Session checkout items: {}", selectedItems != null ? selectedItems.size() : "null");
            if (selectedItems != null) {
                for (CartItem item : selectedItems) {
                    logger.info("VNPay: Cart item - Book: {}, Shop: {}, Quantity: {}",
                               item.getBook() != null ? item.getBook().getTitle() : "null",
                               item.getShop() != null ? item.getShop().getShopName() : "null",
                               item.getQuantity());
                }
            }

            if (selectedItems == null || selectedItems.isEmpty()) {
                logger.warn("No checkout items found in session for VNPay payment");
                return "redirect:/buyer/cart?error=no_items";
            }

            logger.info("VNPay: Processing {} selected items for payment reservation", selectedItems.size());

            // Convert CartItem entities to DTOs for serialization
            List<CartItemDTO> cartItemDTOs = selectedItems.stream()
                .map(CartItemDTO::fromCartItem)
                .collect(Collectors.toList());

            // Set selected items in OrderDTO for serialization
            orderDTO.setSelectedItems(cartItemDTOs);

            // Calculate totals for payment reservation
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalShippingFee = BigDecimal.ZERO;
            BigDecimal totalDiscountAmount = BigDecimal.ZERO;

            // Calculate totals from selected items
            for (CartItem item : selectedItems) {
                BigDecimal itemTotal = item.getBook().getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                // Add shipping fee (simplified - 30,000 VND per shop)
                totalShippingFee = totalShippingFee.add(new BigDecimal("30000"));
            }

            // Create payment reservation instead of order
            PaymentReservation paymentReservation = paymentReservationService.createVNPayReservation(
                user, orderDTO, totalAmount, totalShippingFee, totalDiscountAmount);

            logger.info("VNPay: Created payment reservation with ID: {}, TxnRef: {}",
                       paymentReservation.getReservationId(), paymentReservation.getVnpayTxnRef());

            // Reserve inventory for payment reservation
            try {
                logger.info("VNPay: Reserving inventory for PaymentReservation ID: {}", paymentReservation.getReservationId());
                inventoryReservationService.reserveInventoryForPayment(paymentReservation);
                logger.info("VNPay: Successfully reserved inventory for PaymentReservation ID: {}", paymentReservation.getReservationId());
            } catch (Exception e) {
                logger.error("VNPay: Failed to reserve inventory for PaymentReservation ID: {}", paymentReservation.getReservationId(), e);
                redirectAttributes.addFlashAttribute("errorMessage", "Một số sản phẩm đã hết hàng. Vui lòng thử lại.");
                return "redirect:/buyer/cart";
            }

            // Create VNPay payment URL using PaymentReservation
            logger.info("VNPay: Creating payment URL for PaymentReservation ID: {}, Total Amount: {}",
                       paymentReservation.getReservationId(), paymentReservation.getTotalAmount());

            String paymentUrl = vnPayService.createPaymentUrlFromReservation(paymentReservation);

            // Validate payment URL
            if (paymentUrl == null || paymentUrl.trim().isEmpty()) {
                logger.error("VNPay: Generated payment URL is null or empty");
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo liên kết thanh toán VNPay. Vui lòng thử lại.");
                return "redirect:/buyer/checkout?error";
            }

            logger.info("VNPay: Generated payment URL: {}", paymentUrl);

            // Validate URL format
            if (!paymentUrl.startsWith("https://sandbox.vnpayment.vn/")) {
                logger.error("VNPay: Invalid payment URL format: {}", paymentUrl);
                redirectAttributes.addFlashAttribute("errorMessage", "URL thanh toán VNPay không hợp lệ. Vui lòng thử lại.");
                return "redirect:/buyer/checkout?error";
            }

            // Store reservation info in session for callback processing
            session.setAttribute("pendingReservationId", paymentReservation.getReservationId());
            session.setAttribute("pendingVnpayTxnRef", paymentReservation.getVnpayTxnRef());

            // Redirect to VNPay payment page
            logger.info("VNPay: Redirecting to payment URL: {}", paymentUrl);
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            logger.error("Error processing VNPay payment reservation: {}", e.getMessage(), e);

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

    private void processShippingAddress(CustomerOrder customerOrder, OrderDTO orderDTO, User user) {
        // Check if an existing address is selected or if we should use default address
        if (orderDTO.getExistingAddressId() != null && !orderDTO.getExistingAddressId().isEmpty() && !orderDTO.getExistingAddressId().equals("new")) {
            // Use existing address
            UserAddress address = userAddressService.findAddressById(Integer.valueOf(orderDTO.getExistingAddressId()))
                    .orElseThrow(() -> new IllegalArgumentException("Address not found"));

            customerOrder.setRecipientName(address.getRecipientName());
            customerOrder.setRecipientPhone(address.getRecipientPhone());
            customerOrder.setShippingProvince(address.getProvince());
            customerOrder.setShippingDistrict(address.getDistrict());
            customerOrder.setShippingWard(address.getWard());
            customerOrder.setShippingAddressDetail(address.getAddressDetail());
            customerOrder.setShippingCompany(address.getCompany());
            customerOrder.setShippingAddressType(address.getAddress_type());
        } else if (orderDTO.getExistingAddressId() == null || orderDTO.getExistingAddressId().isEmpty()) {
            // No address specified, try to use default address
            UserAddress defaultAddress = userAddressService.findDefaultAddress(user);
            if (defaultAddress != null) {
                customerOrder.setRecipientName(defaultAddress.getRecipientName());
                customerOrder.setRecipientPhone(defaultAddress.getRecipientPhone());
                customerOrder.setShippingProvince(defaultAddress.getProvince());
                customerOrder.setShippingDistrict(defaultAddress.getDistrict());
                customerOrder.setShippingWard(defaultAddress.getWard());
                customerOrder.setShippingAddressDetail(defaultAddress.getAddressDetail());
                customerOrder.setShippingCompany(defaultAddress.getCompany());
                customerOrder.setShippingAddressType(defaultAddress.getAddress_type());
            } else {
                // No default address, use new address from form
                setNewAddressToCustomerOrder(customerOrder, orderDTO);
            }
        } else {
            // Use new address
            setNewAddressToCustomerOrder(customerOrder, orderDTO);

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

    private void setNewAddressToCustomerOrder(CustomerOrder customerOrder, OrderDTO orderDTO) {
        customerOrder.setRecipientName(orderDTO.getRecipientName());
        customerOrder.setRecipientPhone(orderDTO.getRecipientPhone());
        customerOrder.setShippingProvince(orderDTO.getProvince());
        customerOrder.setShippingDistrict(orderDTO.getDistrict());
        customerOrder.setShippingWard(orderDTO.getWard());
        customerOrder.setShippingAddressDetail(orderDTO.getAddressDetail());
        customerOrder.setShippingCompany(orderDTO.getCompany());
        customerOrder.setShippingAddressType(orderDTO.getAddressType());
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

            // Get the customer order that contains this order
            CustomerOrder customerOrder = order.getCustomerOrder();
            if (customerOrder == null) {
                logger.warn("Order has no associated customer order: orderId={}", orderId);
                return "redirect:/buyer/orders";
            }

            model.addAttribute("customerOrder", customerOrder);
            logger.info("Order success page loaded successfully for orderId: {}, customerOrderId: {}", orderId, customerOrder.getCustomerOrderId());
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
            String vnpayResponseCode = queryParams.get("vnp_ResponseCode");
            String vnpayTxnRef = queryParams.get("vnp_TxnRef");

            if (vnpayTxnRef == null) {
                logger.error("VNPay TxnRef not found in VNPay response");
                redirectAttributes.addFlashAttribute("errorMessage", "Transaction reference not found");
                return "redirect:/buyer/cart?error=payment_failed";
            }

            // Find payment reservation by VNPay transaction reference
            PaymentReservation paymentReservation = paymentReservationService.findByVnpayTxnRef(vnpayTxnRef)
                .orElse(null);

            if (paymentReservation == null) {
                logger.error("PaymentReservation not found for VNPay TxnRef: {}", vnpayTxnRef);
                redirectAttributes.addFlashAttribute("errorMessage", "Payment reservation not found");
                return "redirect:/buyer/cart?error=payment_failed";
            }

            // Validate VNPay response
            if (!vnPayService.validatePaymentResponse(queryParams)) {
                logger.error("Invalid VNPay response signature for TxnRef: {}", vnpayTxnRef);

                // Rollback inventory reservation due to signature validation failure
                try {
                    inventoryReservationService.rollbackReservationByTxnRef(vnpayTxnRef, paymentReservation);
                    paymentReservationService.cancelReservation(paymentReservation);
                    logger.info("VNPay: Rolled back inventory reservation due to signature validation failure for TxnRef: {}", vnpayTxnRef);
                } catch (Exception e) {
                    logger.error("VNPay: Failed to rollback inventory reservation for TxnRef: {}", vnpayTxnRef, e);
                }

                redirectAttributes.addFlashAttribute("errorMessage", "Invalid payment response");
                return "redirect:/buyer/cart?error=payment_failed";
            }

            if ("00".equals(vnpayResponseCode)) {
                // Payment successful - NOW CREATE THE ORDER
                logger.info("VNPay payment successful for TxnRef: {}, creating order from reservation", vnpayTxnRef);

                try {
                    // Create CustomerOrder and Orders from PaymentReservation
                    CustomerOrder customerOrder = createOrderFromReservation(paymentReservation);

                    // Confirm inventory reservation (permanently deduct inventory)
                    inventoryReservationService.confirmReservationByTxnRef(vnpayTxnRef);

                    // Mark payment reservation as confirmed
                    paymentReservationService.confirmReservation(paymentReservation);

                    logger.info("VNPay: Successfully created order {} from payment reservation {}",
                               customerOrder.getCustomerOrderId(), paymentReservation.getReservationId());

                    // Remove items from cart (parse from reservation data)
                    try {
                        OrderDTO orderDTO = paymentReservationService.getOrderDTOFromReservation(paymentReservation);
                        User user = paymentReservation.getUser();

                        for (CartItemDTO cartItem : orderDTO.getSelectedItems()) {
                            cartService.removeItem(user, cartItem.getBookId());
                        }
                        logger.info("Successfully removed {} items from cart after VNPay payment", orderDTO.getSelectedItems().size());
                    } catch (Exception e) {
                        logger.warn("Failed to remove items from cart after successful payment: {}", e.getMessage());
                        // Don't fail the entire transaction for cart cleanup issues
                    }

                    // Clean up session
                    session.removeAttribute("pendingReservationId");
                    session.removeAttribute("pendingVnpayTxnRef");

                    redirectAttributes.addFlashAttribute("successMessage", "Payment successful!");
                    return "redirect:/buyer/order-success/" + customerOrder.getCustomerOrderId();

                } catch (Exception e) {
                    logger.error("VNPay: Failed to create order from reservation after successful payment for TxnRef: {}", vnpayTxnRef, e);

                    // This is a critical error - payment succeeded but order creation failed
                    // We need to handle this carefully
                    try {
                        paymentReservationService.cancelReservation(paymentReservation);
                        inventoryReservationService.rollbackReservationByTxnRef(vnpayTxnRef, paymentReservation);
                        logger.info("VNPay: Rolled back reservation after order creation failure for TxnRef: {}", vnpayTxnRef);
                    } catch (Exception rollbackException) {
                        logger.error("VNPay: CRITICAL - Failed to rollback after order creation failure for TxnRef: {}", vnpayTxnRef, rollbackException);
                    }

                    redirectAttributes.addFlashAttribute("errorMessage", "Payment successful but order creation failed. Please contact support.");
                    return "redirect:/buyer/cart?error=order_creation_failed";
                }

            } else {
                // Payment failed
                logger.warn("VNPay payment failed for TxnRef: {} with response code: {}", vnpayTxnRef, vnpayResponseCode);

                // Rollback inventory reservation
                try {
                    inventoryReservationService.rollbackReservationByTxnRef(vnpayTxnRef, paymentReservation);
                    paymentReservationService.cancelReservation(paymentReservation);
                    logger.info("VNPay: Rolled back inventory reservation for failed payment, TxnRef: {}", vnpayTxnRef);
                } catch (Exception e) {
                    logger.error("VNPay: Failed to rollback inventory reservation for TxnRef: {}", vnpayTxnRef, e);
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

    /**
     * Create CustomerOrder and Orders from PaymentReservation after successful payment
     */
    private CustomerOrder createOrderFromReservation(PaymentReservation paymentReservation) {
        try {
            // Get OrderDTO from reservation data
            OrderDTO orderDTO = paymentReservationService.getOrderDTOFromReservation(paymentReservation);
            User user = paymentReservation.getUser();

            logger.info("Creating order from PaymentReservation ID: {} for user: {}",
                       paymentReservation.getReservationId(), user.getEmail());

            // Create new customer order
            CustomerOrder customerOrder = new CustomerOrder();
            customerOrder.setUser(user);
            customerOrder.setCreatedAt(LocalDateTime.now());
            customerOrder.setStatus(OrderStatus.PROCESSING);
            customerOrder.setPaymentMethod(PaymentMethod.VNPAY);
            customerOrder.setPaymentStatus(PaymentStatus.PAID);
            customerOrder.setNotes(paymentReservation.getNotes());

            // Set shipping address - handle both existing address and new address
            if (orderDTO.getExistingAddressId() != null && !orderDTO.getExistingAddressId().trim().isEmpty()
                && !"0".equals(orderDTO.getExistingAddressId().trim())) {
                // Use existing address
                logger.info("Using existing address ID: {}", orderDTO.getExistingAddressId());
                try {
                    Integer addressId = Integer.parseInt(orderDTO.getExistingAddressId());
                    Optional<UserAddress> userAddressOpt = userAddressService.findAddressById(addressId);

                    if (userAddressOpt.isPresent()) {
                        UserAddress userAddress = userAddressOpt.get();
                        customerOrder.setRecipientName(userAddress.getRecipientName());
                        customerOrder.setRecipientPhone(userAddress.getRecipientPhone());
                        customerOrder.setShippingProvince(userAddress.getProvince());
                        customerOrder.setShippingDistrict(userAddress.getDistrict());
                        customerOrder.setShippingWard(userAddress.getWard());
                        customerOrder.setShippingAddressDetail(userAddress.getAddressDetail());

                        logger.info("Set shipping address from existing address: {}", userAddress.getRecipientName());
                    } else {
                        logger.error("UserAddress not found for ID: {}", addressId);
                        throw new RuntimeException("Địa chỉ giao hàng không tồn tại");
                    }
                } catch (NumberFormatException e) {
                    logger.error("Invalid address ID format: {}", orderDTO.getExistingAddressId());
                    throw new RuntimeException("ID địa chỉ không hợp lệ");
                }
            } else if (orderDTO.getRecipientName() != null && !orderDTO.getRecipientName().trim().isEmpty()) {
                // Use new address from OrderDTO
                customerOrder.setRecipientName(orderDTO.getRecipientName());
                customerOrder.setRecipientPhone(orderDTO.getRecipientPhone());
                customerOrder.setShippingProvince(orderDTO.getProvince());
                customerOrder.setShippingDistrict(orderDTO.getDistrict());
                customerOrder.setShippingWard(orderDTO.getWard());
                customerOrder.setShippingAddressDetail(orderDTO.getAddressDetail());

                logger.info("Set shipping address from OrderDTO: {}", orderDTO.getRecipientName());
            } else {
                // Fallback to reservation data
                customerOrder.setRecipientName(paymentReservation.getRecipientName());
                customerOrder.setRecipientPhone(paymentReservation.getRecipientPhone());
                customerOrder.setShippingProvince(paymentReservation.getShippingProvince());
                customerOrder.setShippingDistrict(paymentReservation.getShippingDistrict());
                customerOrder.setShippingWard(paymentReservation.getShippingWard());
                customerOrder.setShippingAddressDetail(paymentReservation.getShippingAddressDetail());

                logger.info("Set shipping address from reservation: {}", paymentReservation.getRecipientName());
            }

            // Validate that we have recipient information
            if (customerOrder.getRecipientName() == null || customerOrder.getRecipientName().trim().isEmpty()) {
                logger.error("No recipient name found in OrderDTO or PaymentReservation");
                throw new RuntimeException("Thông tin người nhận không đầy đủ");
            }

            // Set totals from reservation
            customerOrder.setTotalAmount(paymentReservation.getTotalAmount());
            customerOrder.setShippingFee(paymentReservation.getShippingFee());
            customerOrder.setDiscountAmount(paymentReservation.getDiscountAmount());

            // Save customer order first
            customerOrder = customerOrderService.save(customerOrder);

            // Group cart items by shop and create orders
            Map<Integer, List<CartItemDTO>> itemsByShop = orderDTO.getSelectedItems().stream()
                .collect(Collectors.groupingBy(CartItemDTO::getShopId));

            logger.info("Creating {} orders for {} shops", itemsByShop.size(), itemsByShop.size());

            for (Map.Entry<Integer, List<CartItemDTO>> shopEntry : itemsByShop.entrySet()) {
                Integer shopId = shopEntry.getKey();
                List<CartItemDTO> shopItems = shopEntry.getValue();

                // Create order for this shop
                Order order = new Order();
                order.setOrderDate(LocalDateTime.now());
                order.setOrderStatus(OrderStatus.PROCESSING);
                order.setCustomerOrder(customerOrder);

                // Get shop information from first item
                Book firstBook = bookService.getBookById(shopItems.get(0).getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Book not found"));
                order.setShop(firstBook.getShop());

                // Create order items
                List<OrderItem> orderItems = new ArrayList<>();
                BigDecimal subtotal = BigDecimal.ZERO;

                for (CartItemDTO cartItem : shopItems) {
                    Book book = bookService.getBookById(cartItem.getBookId())
                        .orElseThrow(() -> new IllegalArgumentException("Book not found: " + cartItem.getBookId()));

                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setBook(book);
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(book.getSellingPrice());
                    orderItem.setSubtotal(book.getSellingPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

                    orderItems.add(orderItem);
                    subtotal = subtotal.add(orderItem.getSubtotal());

                    logger.debug("Added order item - Book: {}, Quantity: {}, Price: {}",
                               book.getTitle(), cartItem.getQuantity(), book.getSellingPrice());
                }

                order.setOrderItems(orderItems);
                order.setSubTotal(subtotal);

                // Set shipping fee (30,000 VND per shop)
                BigDecimal shippingFee = new BigDecimal("30000");
                order.setShippingFee(shippingFee);
                order.setDiscountAmount(BigDecimal.ZERO); // Discount applied at customer order level

                // Calculate total amount for this order
                BigDecimal orderTotal = subtotal.add(shippingFee);
                order.setTotalAmount(orderTotal);

                // Add order to customer order
                customerOrder.addOrder(order);

                // Save order
                Order savedOrder = orderService.save(order);
                logger.info("Created order ID: {} for shop: {}", savedOrder.getOrderId(), firstBook.getShop().getShopName());
            }

            // Update customer order with final totals
            customerOrder = customerOrderService.save(customerOrder);

            logger.info("Successfully created CustomerOrder ID: {} from PaymentReservation ID: {}",
                       customerOrder.getCustomerOrderId(), paymentReservation.getReservationId());

            return customerOrder;

        } catch (Exception e) {
            logger.error("Failed to create order from PaymentReservation ID: {}",
                        paymentReservation.getReservationId(), e);
            throw new RuntimeException("Failed to create order from payment reservation", e);
        }
    }
}