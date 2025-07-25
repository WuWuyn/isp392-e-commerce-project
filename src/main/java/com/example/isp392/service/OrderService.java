package com.example.isp392.service;

import com.example.isp392.model.*;
import com.example.isp392.repository.OrderRepository;
import com.example.isp392.repository.CustomerOrderRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import com.example.isp392.model.enums.WalletReferenceType;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Value("${platform.commission.rate:0.10}")
    private BigDecimal platformCommissionRate; // Default to 10% if not specified

    private final OrderRepository orderRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final PromotionService promotionService;
    private final PromotionCalculationService promotionCalculationService;
    private final BookService bookService;
    private final CustomerOrderService customerOrderService;
    private final CartService cartService;
    private final WalletService walletService;
    private final Lock orderStatusLock = new ReentrantLock();

    public OrderService(OrderRepository orderRepository,
                       CustomerOrderRepository customerOrderRepository,
                       PromotionService promotionService,
                       PromotionCalculationService promotionCalculationService,
                       BookService bookService,
                       CustomerOrderService customerOrderService,
                       CartService cartService,
                       WalletService walletService) {
        this.orderRepository = orderRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.promotionService = promotionService;
        this.promotionCalculationService = promotionCalculationService;
        this.bookService = bookService;
        this.customerOrderService = customerOrderService;
        this.cartService = cartService;
        this.walletService = walletService;
    }

    public List<Order> getOrdersForSeller(Integer sellerId) {
        return orderRepository.findByShopShopId(sellerId);
    }

    @Transactional
    public boolean updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        try {
            orderStatusLock.lock();
            return orderRepository.findById(orderId).map(order -> {
                // Validate status transition
                if (!isValidStatusTransition(order.getOrderStatus(), newStatus)) {
                    throw new IllegalStateException("Không thể chuyển từ trạng thái " + 
                        order.getOrderStatus() + " sang " + newStatus);
                }
                
                // Check if order is part of a customer order
                if (order.getCustomerOrder() != null) {
                    validateCustomerOrderStatus(order.getCustomerOrder(), newStatus);
                }
                
                OrderStatus oldStatus = order.getOrderStatus();
                order.setOrderStatus(newStatus);

                if (oldStatus != newStatus) {
                    handleInventoryForStatusChange(order, oldStatus, newStatus);
                }

                orderRepository.save(order);

                // Process seller payment if order is delivered
                if (newStatus == OrderStatus.DELIVERED && oldStatus != OrderStatus.DELIVERED) {
                    try {
                        processSellerPayment(order);
                        logger.info("Processed seller payment for order {}", orderId);
                    } catch (Exception e) {
                        logger.error("Failed to process seller payment for order {}: {}", orderId, e.getMessage(), e);
                        // Don't fail the status update if payment processing fails
                    }
                }

                // Update customer order status if needed
                if (order.getCustomerOrder() != null) {
                    customerOrderService.updateCustomerOrderStatus(order.getCustomerOrder().getCustomerOrderId());
                }

                return true;
            }).orElse(false);
        } finally {
            orderStatusLock.unlock();
        }
    }

    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (currentStatus) {
            case PROCESSING:
                // From PROCESSING, can only go to SHIPPED or CANCELLED
                return newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED:
                // From SHIPPED, can only go to DELIVERED or CANCELLED
                return newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.CANCELLED;
            case DELIVERED:
            case CANCELLED:
                // Cannot change from a final state
                return false;
            default:
                return false;
        }
    }

    /**
     * Check if an order can be cancelled by the customer
     */
    public boolean canBeCancelled(Order order) {
        return order.getOrderStatus() == OrderStatus.PROCESSING ||
               order.getOrderStatus() == OrderStatus.SHIPPED;
    }

    /**
     * Cancel an order with reason (for customer cancellation)
     */
    @Transactional
    public boolean cancelOrder(Integer orderId, String cancellationReason, User user) {
        try {
            orderStatusLock.lock();

            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found: {}", orderId);
                return false;
            }

            Order order = orderOpt.get();

            // Verify order belongs to user
            if (!order.getCustomerOrder().getUser().getUserId().equals(user.getUserId())) {
                logger.warn("User {} attempted to cancel order {} that doesn't belong to them",
                           user.getEmail(), orderId);
                return false;
            }

            // Check if order can be cancelled
            if (!canBeCancelled(order)) {
                logger.warn("Order {} cannot be cancelled. Current status: {}",
                           orderId, order.getOrderStatus());
                return false;
            }

            // Cancel the order
            OrderStatus oldStatus = order.getOrderStatus();
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setCancellationReason(cancellationReason);
            order.setCancelledAt(LocalDateTime.now());

            // Handle inventory restoration
            handleInventoryForStatusChange(order, oldStatus, OrderStatus.CANCELLED);

            // Save order
            orderRepository.save(order);

            // Process wallet refund if applicable
            if (order.getCustomerOrder() != null) {
                try {
                    // Check if this is the last order in the customer order to be cancelled
                    CustomerOrder customerOrder = order.getCustomerOrder();
                    boolean allOrdersCancelled = customerOrder.getOrders().stream()
                            .allMatch(o -> o.getOrderStatus() == OrderStatus.CANCELLED);

                    if (allOrdersCancelled) {
                        // Process refund to wallet for the entire customer order
                        walletService.processRefund(customerOrder);
                        logger.info("Processed wallet refund for customer order {} after cancelling order {}",
                                   customerOrder.getCustomerOrderId(), orderId);
                    }
                } catch (Exception e) {
                    logger.error("Failed to process wallet refund for order {}: {}", orderId, e.getMessage(), e);
                    // Don't fail the cancellation if refund fails, but log the error
                }
            }

            // Update customer order status
            if (order.getCustomerOrder() != null) {
                customerOrderService.updateCustomerOrderStatus(order.getCustomerOrder().getCustomerOrderId());
            }

            logger.info("Order {} cancelled successfully by user {}", orderId, user.getEmail());
            return true;

        } catch (Exception e) {
            logger.error("Error cancelling order {}: {}", orderId, e.getMessage(), e);
            return false;
        } finally {
            orderStatusLock.unlock();
        }
    }

    private void validateCustomerOrderStatus(CustomerOrder customerOrder, OrderStatus newStatus) {
        // Check if any order in the customer order is in an incompatible state
        for (Order order : customerOrder.getOrders()) {
            if (order.getOrderStatus() == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
                throw new IllegalStateException("Không thể cập nhật trạng thái khi có đơn hàng đã hủy trong nhóm");
            }
        }
    }

    public Optional<Order> findOrderById(Integer orderId) {
        return orderRepository.findByIdWithCustomerOrder(orderId);
    }

    public Page<Order> findOrders(User user, String status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return orderRepository.findAll((Specification<Order>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("customerOrder").get("user"), user));
            
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.valueOf(status)));
            }
            
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), dateFrom.atStartOfDay()));
            }
            
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), dateTo.plusDays(1).atStartOfDay()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    public Optional<Order> findByIdAndUser(Integer orderId, User user) {
        return orderRepository.findByOrderIdAndUser(orderId, user);
    }

    public void applyPromotion(Order order, String promotionCode) {
        logger.info("Applying promotion {} to order with subtotal: {}", promotionCode, order.getSubTotal());

        // Use centralized promotion service for validation and calculation
        PromotionCalculationService.PromotionApplicationResult result =
            promotionCalculationService.applyPromotion(promotionCode, order.getCustomerOrder().getUser(), order.getSubTotal());

        if (!result.isSuccess()) {
            logger.error("Promotion application failed: {}", result.getErrorMessage());
            throw new RuntimeException(result.getErrorMessage());
        }

        logger.info("Promotion application successful. Discount amount: {}, Final total: {}",
                   result.getDiscountAmount(), result.getFinalTotal());

        order.setDiscountAmount(result.getDiscountAmount());
        order.setDiscountCode(promotionCode);
        promotionCalculationService.recordPromotionUsage(promotionCode, order.getCustomerOrder().getUser().getUserId());

        logger.info("Order discount amount set to: {}, discount code set to: {}",
                   order.getDiscountAmount(), order.getDiscountCode());
    }

    private void handleInventoryForStatusChange(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        if (newStatus == OrderStatus.CANCELLED) {
            // Return items to inventory when order is cancelled
            for (OrderItem item : order.getOrderItems()) {
                bookService.increaseStockQuantity(item.getBook().getBookId(), item.getQuantity());
            }
        } else if (oldStatus == OrderStatus.CANCELLED &&
                  newStatus == OrderStatus.PROCESSING) {
            // Remove items from inventory when cancelled order is reactivated
            for (OrderItem item : order.getOrderItems()) {
                bookService.decreaseStockQuantity(item.getBook().getBookId(), item.getQuantity());
            }
        }
    }
    
    /**
     * Lưu đơn hàng vào cơ sở dữ liệu
     * @param order Đơn hàng cần lưu
     * @return Đơn hàng đã được lưu
     */
    @org.springframework.transaction.annotation.Transactional(isolation = Isolation.SERIALIZABLE)
    public Order save(Order order) {
        logger.info("Saving order with {} items for user: {}",
                   order.getOrderItems().size(),
                   order.getCustomerOrder() != null && order.getCustomerOrder().getUser() != null ?
                   order.getCustomerOrder().getUser().getEmail() : "Unknown");

        // Validate order data
        validateOrderData(order);

        // Validate order items first
        for (OrderItem item : order.getOrderItems()) {
            // Validate pricing
            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Giá sản phẩm không hợp lệ: " + item.getBook().getTitle());
            }

            // Validate quantity
            if (item.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng sản phẩm không hợp lệ: " + item.getBook().getTitle());
            }
        }

        // Atomically reserve inventory for all items to prevent race conditions
        try {
            logger.info("Reserving inventory for order with {} items", order.getOrderItems().size());
            bookService.reserveInventoryForOrder(order.getOrderItems());
            logger.info("Successfully reserved inventory for all order items");
        } catch (IllegalArgumentException e) {
            logger.error("Failed to reserve inventory: {}", e.getMessage());
            throw new RuntimeException("Đặt hàng thất bại: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during inventory reservation: {}", e.getMessage(), e);
            throw new RuntimeException("Có lỗi xảy ra khi đặt hàng. Vui lòng thử lại.");
        }

        Order savedOrder = orderRepository.save(order);
        logger.info("Order saved successfully with ID: {} for user: {}",
                   savedOrder.getOrderId(),
                   savedOrder.getCustomerOrder() != null && savedOrder.getCustomerOrder().getUser() != null ?
                   savedOrder.getCustomerOrder().getUser().getEmail() : "Unknown");
        return savedOrder;
    }

    private void validateOrderData(Order order) {
        if (order.getCustomerOrder() == null || order.getCustomerOrder().getUser() == null) {
            throw new RuntimeException("Thông tin người dùng không hợp lệ");
        }

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải có ít nhất một sản phẩm");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Tổng tiền đơn hàng không hợp lệ");
        }

        CustomerOrder customerOrder = order.getCustomerOrder();
        if (customerOrder.getRecipientName() == null || customerOrder.getRecipientName().trim().isEmpty()) {
            throw new RuntimeException("Tên người nhận không được để trống");
        }

        if (customerOrder.getRecipientPhone() == null || customerOrder.getRecipientPhone().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại người nhận không được để trống");
        }

        if (customerOrder.getShippingAddressDetail() == null || customerOrder.getShippingAddressDetail().trim().isEmpty()) {
            throw new RuntimeException("Địa chỉ giao hàng không được để trống");
        }

        logger.info("Order validation passed for user: {}", customerOrder.getUser().getEmail());
    }
    
    // Seller-specific methods
    
    public List<Map<String, Object>> getRecentOrders(Integer shopId, int limit) {
        return orderRepository.getRecentOrders(shopId, limit);
    }
    
    public int getNewOrdersCount(Integer shopId, int daysAgo) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysAgo);
        return orderRepository.countByShopShopIdAndOrderDateAfter(shopId, startDate);
    }
    
    public BigDecimal getTodayRevenue(Integer shopId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();

        // Only count revenue from DELIVERED orders
        List<Order> orders = orderRepository.findByShopShopIdAndOrderDateBetweenAndOrderStatus(
            shopId, startOfDay, endOfDay, OrderStatus.DELIVERED);

        return orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalRevenue(Integer shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        // Only count revenue from DELIVERED orders
        List<Order> orders = orderRepository.findByShopShopIdAndOrderDateBetweenAndOrderStatus(
            shopId, start, end, OrderStatus.DELIVERED);

        return orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public Long getTotalOrders(Integer shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        // Only count DELIVERED orders for accurate revenue statistics
        return orderRepository.countByShopShopIdAndOrderDateBetweenAndOrderStatus(shopId, start, end, OrderStatus.DELIVERED);
    }
    
    public List<BigDecimal> getWeeklyRevenue(Integer shopId) {
        LocalDateTime startDate = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime endDate = LocalDate.now().plusDays(1).atStartOfDay();

        // Only count revenue from DELIVERED orders
        List<Order> orders = orderRepository.findByShopShopIdAndOrderDateBetweenAndOrderStatus(
            shopId, startDate, endDate, OrderStatus.DELIVERED);

        Map<LocalDate, BigDecimal> revenueByDate = orders.stream()
            .collect(Collectors.groupingBy(
                order -> order.getOrderDate().toLocalDate(),
                Collectors.mapping(
                    Order::getTotalAmount,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
            
        List<BigDecimal> weeklyRevenue = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            weeklyRevenue.add(revenueByDate.getOrDefault(date, BigDecimal.ZERO));
        }
        
        return weeklyRevenue;
    }
    
    public Optional<Order> findOrderByIdForSeller(Integer orderId, Integer sellerId) {
        return orderRepository.findOrderByIdForSellerWithCustomerOrder(orderId, sellerId);
    }
    
    public Page<Order> searchOrdersForSeller(Integer sellerId, String keyword, OrderStatus status, 
                                           String startDateStr, String endDateStr, Pageable pageable) {
        return orderRepository.findAll((Specification<Order>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(cb.equal(root.get("shop").get("user").get("userId"), sellerId));
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchTerm = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("customerOrder").get("recipientName")), searchTerm),
                    cb.like(cb.lower(root.get("customerOrder").get("recipientPhone")), searchTerm),
                    cb.like(cb.lower(root.get("orderId").as(String.class)), searchTerm)
                ));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("orderStatus"), status));
            }
            
            if (startDateStr != null && !startDateStr.isEmpty()) {
                LocalDate startDate = LocalDate.parse(startDateStr);
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), startDate.atStartOfDay()));
            }
            
            if (endDateStr != null && !endDateStr.isEmpty()) {
                LocalDate endDate = LocalDate.parse(endDateStr);
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), endDate.plusDays(1).atStartOfDay()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    /**
     * Lấy dữ liệu doanh thu theo khoảng thời gian
     * @param shopId ID của shop
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @param period Kỳ (daily, weekly, monthly)
     * @return Danh sách dữ liệu doanh thu
     */
    public List<Map<String, Object>> getRevenueByPeriod(Integer shopId, LocalDate startDate, LocalDate endDate, String period) {
        switch (period.toLowerCase()) {
            case "daily":
                return orderRepository.getRevenueByDay(shopId, startDate, endDate);
            case "weekly":
                return orderRepository.getRevenueByWeek(shopId, startDate, endDate);
            case "monthly":
                return orderRepository.getRevenueByMonth(shopId, startDate, endDate);
            default:
                throw new IllegalArgumentException("Invalid period: " + period);
        }
    }
    
    /**
     * Lấy danh sách sách bán chạy nhất
     * @param shopId ID của shop
     * @param limit Số lượng tối đa
     * @return Danh sách sách bán chạy
     */
    public List<Map<String, Object>> getBestsellingBooks(Integer shopId, int limit) {
        return orderRepository.getBestsellingBooksByQuantity(shopId, limit);
    }
    
    /**
     * Lấy phân bố địa lý của đơn hàng
     * @param shopId ID của shop
     * @return Phân bố địa lý
     */
    public List<Map<String, Object>> getGeographicDistribution(Integer shopId) {
        return orderRepository.getGeographicDistribution(shopId);
    }
    
    /**
     * Tìm đơn hàng cho quản trị viên với các bộ lọc
     * @param search Từ khóa tìm kiếm
     * @param orderStatus Trạng thái đơn hàng
     * @param fromDate Ngày bắt đầu
     * @param toDate Ngày kết thúc
     * @param pageable Phân trang
     * @return Danh sách đơn hàng phù hợp với bộ lọc
     */
    public Page<Order> findOrdersForAdmin(String search, OrderStatus orderStatus, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return orderRepository.findAll((Specification<Order>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add JOIN FETCH for customerOrder to avoid lazy loading issues
            if (query.getResultType().equals(Order.class)) {
                root.fetch("customerOrder", JoinType.LEFT);
            }

            // Search by keyword
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("customerOrder").get("recipientName")), searchTerm),
                    cb.like(cb.lower(root.get("customerOrder").get("recipientPhone")), searchTerm),
                    cb.like(cb.lower(root.get("orderId").as(String.class)), searchTerm),
                    cb.like(cb.lower(root.get("customerOrder").get("user").get("email")), searchTerm),
                    cb.like(cb.lower(root.get("customerOrder").get("user").get("fullName")), searchTerm)
                ));
            }
            
            // Filter by status
            if (orderStatus != null) {
                predicates.add(cb.equal(root.get("orderStatus"), orderStatus));
            }
            
            // Filter by date range
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), fromDate.atStartOfDay()));
            }
            
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), toDate.atTime(LocalTime.MAX)));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
    
    /**
     * Cập nhật trạng thái đơn hàng bởi quản trị viên
     * @param orderId ID đơn hàng
     * @param newStatus Trạng thái mới
     * @param adminNotes Ghi chú của quản trị viên
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    @Transactional
    public boolean updateOrderStatusByAdmin(Integer orderId, OrderStatus newStatus, String adminNotes) {
        try {
            orderStatusLock.lock();
            return orderRepository.findById(orderId).map(order -> {
                OrderStatus oldStatus = order.getOrderStatus();

                // MODIFICATION: Add status transition validation
                if (!isValidStatusTransition(oldStatus, newStatus)) {
                    // If the transition is not valid, throw an exception
                    throw new IllegalStateException("Invalid status transition from " + oldStatus + " to " + newStatus);
                }

                order.setOrderStatus(newStatus);

                // Add admin notes if provided
                if (adminNotes != null && !adminNotes.trim().isEmpty()) {
                    String existingNotes = order.getNotes();
                    String timestamp = LocalDateTime.now().toString();
                    String newNote = "[ADMIN " + timestamp + "]: " + adminNotes;

                    if (existingNotes != null && !existingNotes.isEmpty()) {
                        order.setNotes(existingNotes + "\n" + newNote);
                    } else {
                        order.setNotes(newNote);
                    }
                }

                // Handle inventory changes if status changed
                if (oldStatus != newStatus) {
                    handleInventoryForStatusChange(order, oldStatus, newStatus);
                }

                // Update customer order status if needed
                if (order.getCustomerOrder() != null) {
                    customerOrderService.updateCustomerOrderStatus(order.getCustomerOrder().getCustomerOrderId());
                }

                orderRepository.save(order);
                return true;
            }).orElse(false);
        } finally {
            orderStatusLock.unlock();
        }
    }
    
    /**
     * Xử lý hoàn tiền cho đơn hàng
     * @param orderId ID đơn hàng
     * @param refundReason Lý do hoàn tiền
     * @return true nếu xử lý thành công, false nếu thất bại
     */
    @Transactional
    public boolean processRefund(Integer orderId, String refundReason) {
        return orderRepository.findById(orderId).map(order -> {
            // Validate if order can be refunded
            if (order.getCustomerOrder() == null || order.getCustomerOrder().getPaymentStatus() != PaymentStatus.PAID) {
                return false;
            }

            // Set payment status to REFUNDED
            order.getCustomerOrder().setPaymentStatus(PaymentStatus.REFUNDED);
            
            // Set order status to CANCELLED if not already
            if (order.getOrderStatus() != OrderStatus.CANCELLED) {
                OrderStatus oldStatus = order.getOrderStatus();
                order.setOrderStatus(OrderStatus.CANCELLED);
                handleInventoryForStatusChange(order, oldStatus, OrderStatus.CANCELLED);
            }
            
            // Add refund reason to notes
            if (refundReason != null && !refundReason.trim().isEmpty()) {
                String existingNotes = order.getNotes();
                String timestamp = LocalDateTime.now().toString();
                String refundNote = "[REFUND " + timestamp + "]: " + refundReason;
                
                if (existingNotes != null && !existingNotes.isEmpty()) {
                    order.setNotes(existingNotes + "\n" + refundNote);
                } else {
                    order.setNotes(refundNote);
                }
            }
            
            // Save the order
            orderRepository.save(order);
            
            // Update customer order status if needed
            if (order.getCustomerOrder() != null) {
                customerOrderService.updateCustomerOrderStatus(order.getCustomerOrder().getCustomerOrderId());
            }
            
            return true;
        }).orElse(false);
    }
    
    /**
     * Thêm ghi chú của quản trị viên vào đơn hàng
     * @param orderId ID đơn hàng
     * @param adminNote Ghi chú của quản trị viên
     * @return true nếu thêm thành công, false nếu thất bại
     */
    @Transactional
    public boolean addAdminNote(Integer orderId, String adminNote) {
        if (adminNote == null || adminNote.trim().isEmpty()) {
            return false;
        }
        
        return orderRepository.findById(orderId).map(order -> {
            String existingNotes = order.getNotes();
            String timestamp = LocalDateTime.now().toString();
            String newNote = "[ADMIN " + timestamp + "]: " + adminNote;
            
            if (existingNotes != null && !existingNotes.isEmpty()) {
                order.setNotes(existingNotes + "\n" + newNote);
            } else {
                order.setNotes(newNote);
            }
            
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }

    /**
     * Check if an order can be reordered (rebuy)
     * Allow rebuy for both DELIVERED and CANCELLED orders
     */
    public boolean canBeReordered(Order order) {
        return order.getOrderStatus() == OrderStatus.DELIVERED ||
               order.getOrderStatus() == OrderStatus.CANCELLED;
    }

    /**
     * Rebuy all items from a delivered order by adding them to cart
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean rebuyOrder(Integer orderId, User user) {
        try {
            // First validate the order without transaction
            Order order = validateOrderForRebuy(orderId, user);
            if (order == null) {
                return false;
            }

            // Process each item independently to avoid transaction rollback issues
            return processRebuyItems(order, user);

        } catch (Exception e) {
            logger.error("Error during rebuy of order {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate order for rebuy without transaction
     */
    private Order validateOrderForRebuy(Integer orderId, User user) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            logger.warn("Order not found for rebuy: {}", orderId);
            return null;
        }

        Order order = orderOpt.get();

        // Verify order belongs to user
        if (!order.getCustomerOrder().getUser().getUserId().equals(user.getUserId())) {
            logger.warn("User {} attempted to rebuy order {} that doesn't belong to them",
                       user.getEmail(), orderId);
            return null;
        }

        // Check if order can be reordered
        if (!canBeReordered(order)) {
            logger.warn("Order {} cannot be reordered. Current status: {}",
                       orderId, order.getOrderStatus());
            return null;
        }

        return order;
    }

    /**
     * Process rebuy items with individual error handling
     */
    private boolean processRebuyItems(Order order, User user) {
        int itemsAdded = 0;
        int totalItems = order.getOrderItems().size();

        for (OrderItem orderItem : order.getOrderItems()) {
            try {
                if (addSingleItemToCart(orderItem, user)) {
                    itemsAdded++;
                }
            } catch (Exception e) {
                String bookTitle = getBookTitleFromOrderItem(orderItem);
                logger.error("Failed to add book '{}' to cart for rebuy: {}", bookTitle, e.getMessage());
                // Continue with other items even if one fails
            }
        }

        if (itemsAdded > 0) {
            logger.info("Successfully added {} out of {} items to cart for rebuy of order {}",
                       itemsAdded, totalItems, order.getOrderId());
            return true;
        } else {
            logger.warn("No items could be added to cart for rebuy of order {}", order.getOrderId());
            return false;
        }
    }

    /**
     * Add a single order item to cart with validation
     */
    private boolean addSingleItemToCart(OrderItem orderItem, User user) {
        Book book = orderItem.getBook();

        // Check if book still exists
        if (book == null) {
            logger.warn("Book for order item {} no longer exists, skipping rebuy", orderItem.getOrderItemId());
            return false;
        }

        // Check if book is still active and has stock
        if (!book.getActive()) {
            logger.warn("Book {} is no longer active, skipping rebuy", book.getBookId());
            return false;
        }

        if (book.getStockQuantity() == null || book.getStockQuantity() <= 0) {
            logger.warn("Book {} is out of stock, skipping rebuy", book.getBookId());
            return false;
        }

        // Check current cart quantity to avoid exceeding stock
        int currentCartQuantity = cartService.getCurrentCartQuantity(user, book.getBookId());
        int availableQuantity = book.getStockQuantity() - currentCartQuantity;

        if (availableQuantity <= 0) {
            logger.warn("Book {} already at maximum quantity in cart, skipping rebuy", book.getBookId());
            return false;
        }

        // Determine quantity to add (limited by available stock)
        int quantityToAdd = Math.min(orderItem.getQuantity(), availableQuantity);

        try {
            // Add to cart using CartService
            cartService.addBookToCart(user, book.getBookId(), quantityToAdd);
            logger.info("Added {} x {} to cart for rebuy", quantityToAdd, book.getTitle());
            return true;
        } catch (RuntimeException e) {
            // Log the specific error but don't propagate to avoid transaction rollback
            logger.warn("Could not add book {} to cart for rebuy: {}", book.getTitle(), e.getMessage());
            return false;
        }
    }

    /**
     * Get book title from order item (prefer captured title)
     */
    private String getBookTitleFromOrderItem(OrderItem orderItem) {
        if (orderItem.getBookTitle() != null) {
            return orderItem.getBookTitle();
        }
        if (orderItem.getBook() != null && orderItem.getBook().getTitle() != null) {
            return orderItem.getBook().getTitle();
        }
        return "Unknown Book";
    }

    /**
     * Process and format cancellation reason from UI
     * @param reason Raw reason from UI (could be predefined or custom)
     * @return Formatted cancellation reason
     */
    private String processCancellationReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "Không có lý do cụ thể";
        }

        // Check if it's a predefined reason from CancellationReason enum
        try {
            CancellationReason cancellationReason = CancellationReason.fromDisplayName(reason.trim());
            if (cancellationReason != CancellationReason.OTHER) {
                return cancellationReason.getDisplayName();
            }
        } catch (Exception e) {
            // If not a predefined reason, treat as custom reason
            logger.debug("Custom cancellation reason provided: {}", reason);
        }

        // For custom reasons, ensure it's properly formatted
        String processedReason = reason.trim();
        if (processedReason.length() > 500) {
            processedReason = processedReason.substring(0, 497) + "...";
        }

        return processedReason;
    }

    /**
     * Find orders with search functionality
     */
    public Page<Order> findOrdersWithSearch(User user, String status, LocalDate dateFrom, LocalDate dateTo, String search, Pageable pageable) {
        return orderRepository.findAll((Specification<Order>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join with CustomerOrder to access User
            Join<Order, CustomerOrder> customerOrderJoin = root.join("customerOrder", JoinType.INNER);
            predicates.add(cb.equal(customerOrderJoin.get("user"), user));

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.valueOf(status)));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), dateTo.plusDays(1)));
            }

            // Search functionality
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";

                // Search by order ID or book title
                Predicate orderIdPredicate = cb.like(cb.lower(cb.toString(root.get("orderId"))), searchPattern);

                // Search by book title in order items
                Join<Order, OrderItem> orderItemJoin = root.join("orderItems", JoinType.LEFT);
                Join<OrderItem, Book> bookJoin = orderItemJoin.join("book", JoinType.LEFT);
                Predicate bookTitlePredicate = cb.like(cb.lower(bookJoin.get("title")), searchPattern);

                predicates.add(cb.or(orderIdPredicate, bookTitlePredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    /**
     * Get order statistics for a user
     */
    public Map<String, Object> getOrderStatistics(User user) {
        Map<String, Object> statistics = new HashMap<>();

        // Total orders count
        Long totalOrders = orderRepository.countByCustomerOrderUser(user);
        statistics.put("totalOrders", totalOrders);

        // Total amount spent (only for DELIVERED orders)
        BigDecimal totalSpent = orderRepository.findByCustomerOrderUserAndOrderStatus(user, OrderStatus.DELIVERED)
                .stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalSpent", totalSpent);

        // Orders by status
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            Long count = orderRepository.countByCustomerOrderUserAndOrderStatus(user, status);
            ordersByStatus.put(status.name(), count);
        }
        statistics.put("ordersByStatus", ordersByStatus);

        return statistics;
    }

    /**
     * Process payment to seller when order is delivered
     * @param order The delivered order
     */
    private void processSellerPayment(Order order) {
        if (order.getShop() == null || order.getShop().getUser() == null) {
            logger.error("Cannot process seller payment: shop or seller user not found for order {}", order.getOrderId());
            return;
        }

        User seller = order.getShop().getUser();
        BigDecimal orderTotal = order.getTotalAmount();

        // Calculate platform commission
        BigDecimal commission = orderTotal.multiply(platformCommissionRate);
        BigDecimal sellerAmount = orderTotal.subtract(commission);

        logger.info("Processing seller payment for order {}: Total={}, Commission={}, Seller Amount={}",
                   order.getOrderId(), orderTotal, commission, sellerAmount);

        // Add funds to seller's wallet
        String description = String.format("Payment for delivered order #%d (Total: %s VND, Commission: %s VND)",
                                         order.getOrderId(), orderTotal, commission);

        try {
            walletService.addFunds(
                seller,
                sellerAmount,
                description,
                WalletReferenceType.ORDER_PAYMENT,
                order.getOrderId(),
                seller.getUserId()
            );

            logger.info("Successfully processed seller payment of {} VND for order {} to seller {}",
                       sellerAmount, order.getOrderId(), seller.getEmail());
        } catch (Exception e) {
            logger.error("Failed to add funds to seller wallet for order {}: {}", order.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process seller payment: " + e.getMessage(), e);
        }
    }

    /**
     * Enhanced cancel order method with better reason processing
     * @param orderId Order ID to cancel
     * @param cancellationReason Raw cancellation reason from UI
     * @param user User performing the cancellation
     * @return true if cancellation successful, false otherwise
     */
    @Transactional
    public boolean cancelOrderWithReason(Integer orderId, String cancellationReason, User user) {
        String processedReason = processCancellationReason(cancellationReason);
        logger.info("Cancelling order {} with processed reason: {}", orderId, processedReason);

        return cancelOrder(orderId, processedReason, user);
    }

    /**
     * Get all delivered order items for a user (for review management)
     * @param user The user to get order items for
     * @return List of delivered order items
     */
    public List<OrderItem> getDeliveredOrderItemsForUser(User user) {
        List<Order> deliveredOrders = orderRepository.findByCustomerOrderUserAndOrderStatus(user, OrderStatus.DELIVERED);

        List<OrderItem> allOrderItems = new ArrayList<>();
        for (Order order : deliveredOrders) {
            allOrderItems.addAll(order.getOrderItems());
        }

        // Sort by order date (newest first)
        allOrderItems.sort((a, b) -> b.getOrder().getOrderDate().compareTo(a.getOrder().getOrderDate()));

        return allOrderItems;
    }

    /**
     * Get order item by ID
     * @param orderItemId The order item ID
     * @return OrderItem or null if not found
     */
    public OrderItem getOrderItemById(Integer orderItemId) {
        // You'll need to add this method to your OrderItemRepository
        // For now, let's implement it by searching through orders
        List<Order> allOrders = orderRepository.findAll();
        for (Order order : allOrders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getOrderItemId().equals(orderItemId)) {
                    return item;
                }
            }
        }
        return null;
    }
    public long countNewOrdersForSeller(Integer shopId) {
        if (shopId == null) {
            return 0;
        }
        return orderRepository.countByShopShopIdAndOrderStatus(shopId, OrderStatus.PROCESSING);
    }
}
