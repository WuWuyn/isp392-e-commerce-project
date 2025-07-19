package com.example.isp392.service;

import com.example.isp392.config.PaymentConfig;
import com.example.isp392.dto.CheckoutDTO;
import com.example.isp392.dto.ShopOrderDTO;
import com.example.isp392.model.*;
import com.example.isp392.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final VNPayService vnPayService;
    private final BookService bookService;

    // Lock for concurrent inventory updates
    private final Lock inventoryLock = new ReentrantLock();

    @Transactional
    public CustomerOrder checkout(Integer userId, CheckoutDTO checkoutDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate cart items and inventory
        List<CartItem> selectedItems = cartItemRepository.findAllById(
                checkoutDTO.getShopOrders().stream()
                        .flatMap(so -> so.getCartItemIds().stream())
                        .collect(Collectors.toList())
        );
        
        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Không có sản phẩm nào được chọn");
        }
        
        // Validate inventory before proceeding
        try {
            inventoryLock.lock();
            logger.info("Validating inventory for {} selected items", selectedItems.size());

            for (CartItem item : selectedItems) {
                logger.info("Checking inventory for book ID: {}, requested: {}, available: {}",
                           item.getBook().getBookId(), item.getQuantity(), item.getBook().getStockQuantity());

                if (item.getQuantity() > item.getBook().getStockQuantity()) {
                    logger.error("Insufficient inventory for book ID: {}, title: {}, requested: {}, available: {}",
                               item.getBook().getBookId(), item.getBook().getTitle(),
                               item.getQuantity(), item.getBook().getStockQuantity());
                    throw new RuntimeException("Sản phẩm '" + item.getBook().getTitle() + "' không đủ số lượng trong kho. " +
                                             "Yêu cầu: " + item.getQuantity() + ", Có sẵn: " + item.getBook().getStockQuantity());
                }
            }
            logger.info("Inventory validation passed for all items");
            
            // Create customer order with shipping and payment information
            CustomerOrder customerOrder = new CustomerOrder();
            customerOrder.setUser(user);
            customerOrder.setRecipientName(checkoutDTO.getRecipientName());
            customerOrder.setRecipientPhone(checkoutDTO.getRecipientPhone());
            customerOrder.setShippingProvince(checkoutDTO.getShippingProvince());
            customerOrder.setShippingDistrict(checkoutDTO.getShippingDistrict());
            customerOrder.setShippingWard(checkoutDTO.getShippingWard());
            customerOrder.setShippingAddressDetail(checkoutDTO.getShippingAddressDetail());
            customerOrder.setShippingCompany(checkoutDTO.getShippingCompany());
            customerOrder.setShippingAddressType(checkoutDTO.getShippingAddressType());
            customerOrder.setPaymentMethod(PaymentMethod.valueOf(checkoutDTO.getPaymentMethod()));
            customerOrder.setPaymentStatus(PaymentStatus.PENDING);
            customerOrder.setTotalAmount(BigDecimal.ZERO);
            customerOrder.setShippingFee(BigDecimal.ZERO);
            customerOrder.setDiscountAmount(BigDecimal.ZERO);
            customerOrder.setStatus(OrderStatus.PENDING);
            customerOrder.setNotes(checkoutDTO.getNotes());
            customerOrder.setCreatedAt(LocalDateTime.now());
            customerOrder = customerOrderRepository.save(customerOrder);

            // Group cart items by shop
            Map<Integer, List<CartItem>> itemsByShop = selectedItems.stream()
                    .collect(Collectors.groupingBy(item -> item.getShop().getShopId()));

            BigDecimal groupTotal = BigDecimal.ZERO;

            // Create orders for each shop
            List<Order> orders = new ArrayList<>();
            for (ShopOrderDTO shopOrderDTO : checkoutDTO.getShopOrders()) {
                Shop shop = shopRepository.findById(shopOrderDTO.getShopId())
                        .orElseThrow(() -> new RuntimeException("Shop not found"));

                List<CartItem> shopItems = itemsByShop.get(shop.getShopId());
                if (shopItems == null || shopItems.isEmpty()) {
                    continue;
                }

                // Calculate totals
                BigDecimal subTotal = shopItems.stream()
                        .map(item -> item.getBook().getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Create order for this shop
                Order order = new Order();
                order.setShop(shop);
                order.setCustomerOrder(customerOrder);
                order.setSubTotal(subTotal);
                order.setShippingFee(PaymentConfig.DEFAULT_SHIPPING_FEE);
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setTotalAmount(subTotal.add(PaymentConfig.DEFAULT_SHIPPING_FEE));
                order.setOrderStatus(OrderStatus.PENDING);
                order.setNotes(shopOrderDTO.getShopNotes());
                order.setOrderDate(LocalDateTime.now());

                // Create order items
                List<OrderItem> orderItems = new ArrayList<>();
                for (CartItem cartItem : shopItems) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setBook(cartItem.getBook());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(cartItem.getBook().getSellingPrice());
                    orderItems.add(orderItem);
                }
                order.setOrderItems(orderItems);

                // Atomically reserve inventory for this shop's items
                try {
                    logger.info("Reserving inventory for shop {} with {} items",
                               shop.getShopName(), orderItems.size());
                    bookService.reserveInventoryForOrder(orderItems);
                    logger.info("Successfully reserved inventory for shop {}", shop.getShopName());
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to reserve inventory for shop {}: {}", shop.getShopName(), e.getMessage());
                    throw new RuntimeException("Đặt hàng thất bại cho shop " + shop.getShopName() + ": " + e.getMessage());
                }

                // Save order
                order = orderRepository.save(order);
                orders.add(order);
                groupTotal = groupTotal.add(order.getTotalAmount());

                // Remove cart items
                cartItemRepository.deleteAll(shopItems);
            }

            // Update customer order total and orders
            BigDecimal totalShippingFee = orders.stream()
                    .map(Order::getShippingFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalDiscountAmount = orders.stream()
                    .map(Order::getDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            customerOrder.setShippingFee(totalShippingFee);
            customerOrder.setDiscountAmount(totalDiscountAmount);
            customerOrder.setTotalAmount(groupTotal);
            customerOrder.setOrders(orders);
            customerOrder = customerOrderRepository.save(customerOrder);

            // If payment method is VNPAY, create payment URL
            if (PaymentMethod.VNPAY.name().equals(checkoutDTO.getPaymentMethod())) {
                String paymentUrl = vnPayService.createPaymentUrl(customerOrder);
                customerOrder.setPaymentUrl(paymentUrl);
            }

            return customerOrder;
            
        } finally {
            inventoryLock.unlock();
        }
    }

    @Transactional
    public void handleVNPayCallback(Map<String, String> vnpayResponse) {
        if (!vnPayService.validatePaymentResponse(vnpayResponse)) {
            throw new RuntimeException("Invalid VNPay response");
        }

        String orderId = vnpayResponse.get("vnp_TxnRef");
        String vnpayResponseCode = vnpayResponse.get("vnp_ResponseCode");

        // Double check transaction status with VNPay
        Map<String, String> transactionStatus = vnPayService.queryTransactionStatus(orderId);
        String apiResponseCode = transactionStatus.get("vnp_ResponseCode");

        CustomerOrder customerOrder = customerOrderRepository.findById(Integer.parseInt(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found"));

        try {
            inventoryLock.lock();
            // Only update if both callback and API check confirm success
            if ("00".equals(vnpayResponseCode) && "00".equals(apiResponseCode)) {
                // Payment successful
                customerOrder.setPaymentStatus(PaymentStatus.PAID);
                for (Order order : customerOrder.getOrders()) {
                    orderRepository.save(order);
                }
                customerOrderRepository.save(customerOrder);
            } else {
                // Payment failed - rollback inventory
                customerOrder.setPaymentStatus(PaymentStatus.FAILED);
                customerOrder.setStatus(OrderStatus.CANCELLED);
                for (Order order : customerOrder.getOrders()) {
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    // Return items to inventory
                    for (OrderItem item : order.getOrderItems()) {
                        bookService.increaseStockQuantity(item.getBook().getBookId(), item.getQuantity());
                    }
                }
                customerOrderRepository.save(customerOrder);
            }
        } finally {
            inventoryLock.unlock();
        }
    }
} 