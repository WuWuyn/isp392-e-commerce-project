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
    private final GroupOrderRepository groupOrderRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final VNPayService vnPayService;
    private final BookService bookService;

    // Lock for concurrent inventory updates
    private final Lock inventoryLock = new ReentrantLock();

    @Transactional
    public GroupOrder checkout(Integer userId, CheckoutDTO checkoutDTO) {
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
            
            // Create group order
            GroupOrder groupOrder = new GroupOrder();
            groupOrder.setUser(user);
            groupOrder.setTotalAmount(BigDecimal.ZERO);
            groupOrder.setCreatedAt(LocalDateTime.now());
            groupOrder = groupOrderRepository.save(groupOrder);

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
                order.setUser(user);
                order.setShop(shop);
                order.setGroupOrder(groupOrder);
                order.setRecipientName(checkoutDTO.getRecipientName());
                order.setRecipientPhone(checkoutDTO.getRecipientPhone());
                order.setShippingProvince(checkoutDTO.getShippingProvince());
                order.setShippingDistrict(checkoutDTO.getShippingDistrict());
                order.setShippingWard(checkoutDTO.getShippingWard());
                order.setShippingAddressDetail(checkoutDTO.getShippingAddressDetail());
                order.setShippingCompany(checkoutDTO.getShippingCompany());
                order.setShippingAddressType(checkoutDTO.getShippingAddressType());
                order.setSubTotal(subTotal);
                order.setShippingFee(PaymentConfig.DEFAULT_SHIPPING_FEE);
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setTotalAmount(subTotal.add(PaymentConfig.DEFAULT_SHIPPING_FEE));
                order.setOrderStatus(OrderStatus.PENDING);
                order.setPaymentMethod(PaymentMethod.valueOf(checkoutDTO.getPaymentMethod()));
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setNotes(shopOrderDTO.getShopNotes());
                order.setOrderDate(LocalDateTime.now());

                // Create order items and update inventory
                List<OrderItem> orderItems = new ArrayList<>();
                for (CartItem cartItem : shopItems) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setBook(cartItem.getBook());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(cartItem.getBook().getSellingPrice());
                    orderItems.add(orderItem);
                    
                    // Update inventory
                    bookService.decreaseStockQuantity(cartItem.getBook().getBookId(), cartItem.getQuantity());
                }
                order.setOrderItems(orderItems);

                // Save order
                order = orderRepository.save(order);
                orders.add(order);
                groupTotal = groupTotal.add(order.getTotalAmount());

                // Remove cart items
                cartItemRepository.deleteAll(shopItems);
            }

            // Update group order total and orders
            groupOrder.setTotalAmount(groupTotal);
            groupOrder.setOrders(orders);
            groupOrder = groupOrderRepository.save(groupOrder);

            // If payment method is VNPAY, create payment URL
            if (PaymentMethod.VNPAY.name().equals(checkoutDTO.getPaymentMethod())) {
                String paymentUrl = vnPayService.createPaymentUrl(groupOrder);
                groupOrder.setPaymentUrl(paymentUrl);
            }

            return groupOrder;
            
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

        GroupOrder groupOrder = groupOrderRepository.findById(Integer.parseInt(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found"));

        try {
            inventoryLock.lock();
            // Only update if both callback and API check confirm success
            if ("00".equals(vnpayResponseCode) && "00".equals(apiResponseCode)) {
                // Payment successful
                for (Order order : groupOrder.getOrders()) {
                    order.setPaymentStatus(PaymentStatus.PAID);
                    orderRepository.save(order);
                }
            } else {
                // Payment failed - rollback inventory
                for (Order order : groupOrder.getOrders()) {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    
                    // Return items to inventory
                    for (OrderItem item : order.getOrderItems()) {
                        bookService.increaseStockQuantity(item.getBook().getBookId(), item.getQuantity());
                    }
                }
            }
        } finally {
            inventoryLock.unlock();
        }
    }
} 