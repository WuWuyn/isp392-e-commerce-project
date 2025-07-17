package com.example.isp392.service;

import com.example.isp392.model.*;
import com.example.isp392.repository.OrderRepository;
import com.example.isp392.repository.GroupOrderRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
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
import org.springframework.data.domain.PageRequest;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final GroupOrderRepository groupOrderRepository;
    private final PromotionService promotionService;
    private final BookService bookService;
    private final GroupOrderService groupOrderService;
    private final Lock orderStatusLock = new ReentrantLock();

    public OrderService(OrderRepository orderRepository, 
                       GroupOrderRepository groupOrderRepository,
                       PromotionService promotionService, 
                       BookService bookService,
                       GroupOrderService groupOrderService) {
        this.orderRepository = orderRepository;
        this.groupOrderRepository = groupOrderRepository;
        this.promotionService = promotionService;
        this.bookService = bookService;
        this.groupOrderService = groupOrderService;
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
                
                // Check if order is part of a group order
                if (order.getGroupOrder() != null) {
                    validateGroupOrderStatus(order.getGroupOrder(), newStatus);
                }
                
                OrderStatus oldStatus = order.getOrderStatus();
                order.setOrderStatus(newStatus);

                if (oldStatus != newStatus) {
                    handleInventoryForStatusChange(order, oldStatus, newStatus);
                }

                orderRepository.save(order);
                
                // Update group order status if needed
                if (order.getGroupOrder() != null) {
                    groupOrderService.updateGroupOrderStatus(order.getGroupOrder());
                }
                
                return true;
            }).orElse(false);
        } finally {
            orderStatusLock.unlock();
        }
    }

    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (currentStatus) {
            case PENDING:
                return newStatus == OrderStatus.PROCESSING || 
                       newStatus == OrderStatus.CANCELLED;
            case PROCESSING:
                return newStatus == OrderStatus.SHIPPED || 
                       newStatus == OrderStatus.CANCELLED;
            case SHIPPED:
                return newStatus == OrderStatus.CANCELLED;
            case CANCELLED:
                return false; // Cannot change from cancelled
            default:
                return false;
        }
    }

    private void validateGroupOrderStatus(GroupOrder groupOrder, OrderStatus newStatus) {
        // Check if any order in the group is in an incompatible state
        for (Order order : groupOrder.getOrders()) {
            if (order.getOrderStatus() == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
                throw new IllegalStateException("Không thể cập nhật trạng thái khi có đơn hàng đã hủy trong nhóm");
            }
        }
    }

    public Optional<Order> findOrderById(Integer orderId) {
        return orderRepository.findById(orderId);
    }

    public Page<Order> findOrders(User user, String status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return orderRepository.findAll((Specification<Order>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(cb.equal(root.get("user"), user));
            
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
        Optional<Promotion> promotionOpt = promotionService.findByCode(promotionCode);
        if (promotionOpt.isEmpty()) {
            throw new RuntimeException("Mã giảm giá không tồn tại");
        }

        Promotion promotion = promotionOpt.get();
        
        if (!promotion.getIsActive()) {
            throw new RuntimeException("Mã giảm giá đã hết hạn");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new RuntimeException("Mã giảm giá không trong thời gian sử dụng");
        }

        if (promotion.getMinOrderValue() != null && 
            order.getSubTotal().compareTo(promotion.getMinOrderValue()) < 0) {
            throw new RuntimeException("Giá trị đơn hàng chưa đạt mức tối thiểu");
        }

        BigDecimal discountAmount;
        if ("PERCENTAGE".equals(promotion.getDiscountType())) {
            discountAmount = order.getSubTotal()
                    .multiply(promotion.getDiscountValue().divide(new BigDecimal(100)));
            
            if (promotion.getMaxDiscountAmount() != null && 
                discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discountAmount = promotion.getMaxDiscountAmount();
            }
        } else {
            discountAmount = promotion.getDiscountValue();
            if (discountAmount.compareTo(order.getSubTotal()) > 0) {
                discountAmount = order.getSubTotal();
            }
        }

        order.setDiscountAmount(discountAmount);
        promotionService.updatePromotionUsage(promotionCode, order.getUser().getUserId());
    }

    private void handleInventoryForStatusChange(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        if (newStatus == OrderStatus.CANCELLED) {
            // Return items to inventory when order is cancelled
            for (OrderItem item : order.getOrderItems()) {
                bookService.increaseStockQuantity(item.getBook().getBookId(), item.getQuantity());
            }
        } else if (oldStatus == OrderStatus.CANCELLED && 
                  (newStatus == OrderStatus.PENDING || newStatus == OrderStatus.PROCESSING)) {
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
    @Transactional
    public Order save(Order order) {
        logger.info("Saving order with {} items for user: {}",
                   order.getOrderItems().size(), order.getUser().getEmail());

        // Validate order data
        validateOrderData(order);

        // Kiểm tra tồn kho và giảm số lượng sách
        for (OrderItem item : order.getOrderItems()) {
            try {
                // Validate pricing
                if (item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Giá sản phẩm không hợp lệ: " + item.getBook().getTitle());
                }

                // Validate quantity
                if (item.getQuantity() <= 0) {
                    throw new RuntimeException("Số lượng sản phẩm không hợp lệ: " + item.getBook().getTitle());
                }

                logger.info("Decreasing stock for book ID: {}, quantity: {}",
                    item.getBook().getBookId(), item.getQuantity());
                bookService.decreaseStockQuantity(item.getBook().getBookId(), item.getQuantity());
                logger.info("Successfully decreased stock for book ID: {}", item.getBook().getBookId());
            } catch (IllegalArgumentException e) {
                logger.error("Failed to decrease stock for book ID: {}, error: {}",
                    item.getBook().getBookId(), e.getMessage());
                throw new RuntimeException("Không thể cập nhật số lượng sách: " + e.getMessage());
            }
        }

        Order savedOrder = orderRepository.save(order);
        logger.info("Order saved successfully with ID: {} for user: {}",
                   savedOrder.getOrderId(), savedOrder.getUser().getEmail());
        return savedOrder;
    }

    private void validateOrderData(Order order) {
        if (order.getUser() == null) {
            throw new RuntimeException("Thông tin người dùng không hợp lệ");
        }

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải có ít nhất một sản phẩm");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Tổng tiền đơn hàng không hợp lệ");
        }

        if (order.getRecipientName() == null || order.getRecipientName().trim().isEmpty()) {
            throw new RuntimeException("Tên người nhận không được để trống");
        }

        if (order.getRecipientPhone() == null || order.getRecipientPhone().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại người nhận không được để trống");
        }

        if (order.getShippingAddressDetail() == null || order.getShippingAddressDetail().trim().isEmpty()) {
            throw new RuntimeException("Địa chỉ giao hàng không được để trống");
        }

        logger.info("Order validation passed for user: {}", order.getUser().getEmail());
    }
    
    // Seller-specific methods
    
    public List<Map<String, Object>> getRecentOrders(Integer shopId, int limit) {
        List<Order> orders = orderRepository.findByShopShopIdOrderByOrderDateDesc(shopId, PageRequest.of(0, limit));
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getOrderId());
            orderMap.put("customerName", order.getUser().getFullName());
            orderMap.put("amount", order.getTotalAmount());
            orderMap.put("status", order.getOrderStatus());
            orderMap.put("date", order.getOrderDate());
            result.add(orderMap);
        }
        
        return result;
    }
    
    public int getNewOrdersCount(Integer shopId, int daysAgo) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysAgo);
        return orderRepository.countByShopShopIdAndOrderDateAfter(shopId, startDate);
    }
    
    public BigDecimal getTodayRevenue(Integer shopId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        
        List<Order> orders = orderRepository.findByShopShopIdAndOrderDateBetweenAndOrderStatus(
            shopId, startOfDay, endOfDay, OrderStatus.SHIPPED);
            
        return orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalRevenue(Integer shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        
        List<Order> orders = orderRepository.findByShopShopIdAndOrderDateBetweenAndOrderStatus(
            shopId, start, end, OrderStatus.SHIPPED);
            
        return orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public Long getTotalOrders(Integer shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        
        return orderRepository.countByShopShopIdAndOrderDateBetween(shopId, start, end);
    }
    
    public List<BigDecimal> getWeeklyRevenue(Integer shopId) {
        LocalDateTime startDate = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime endDate = LocalDate.now().plusDays(1).atStartOfDay();
        
        List<Order> orders = orderRepository.findByShopShopIdAndOrderDateBetweenAndOrderStatus(
            shopId, startDate, endDate, OrderStatus.SHIPPED);
            
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
        return orderRepository.findByOrderIdAndUserUserId(orderId, sellerId);
    }
    
    public Page<Order> searchOrdersForSeller(Integer sellerId, String keyword, OrderStatus status, 
                                           String startDateStr, String endDateStr, Pageable pageable) {
        return orderRepository.findAll((Specification<Order>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(cb.equal(root.get("shop").get("user").get("userId"), sellerId));
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchTerm = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("recipientName")), searchTerm),
                    cb.like(cb.lower(root.get("recipientPhone")), searchTerm),
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
            
            // Search by keyword
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("recipientName")), searchTerm),
                    cb.like(cb.lower(root.get("recipientPhone")), searchTerm),
                    cb.like(cb.lower(root.get("orderId").as(String.class)), searchTerm),
                    cb.like(cb.lower(root.get("user").get("email")), searchTerm),
                    cb.like(cb.lower(root.get("user").get("fullName")), searchTerm)
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
                
                // Update group order status if needed
                if (order.getGroupOrder() != null) {
                    groupOrderService.updateGroupOrderStatus(order.getGroupOrder());
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
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                return false;
            }
            
            // Set payment status to REFUNDED
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            
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
            
            // Update group order status if needed
            if (order.getGroupOrder() != null) {
                groupOrderService.updateGroupOrderStatus(order.getGroupOrder());
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
}
