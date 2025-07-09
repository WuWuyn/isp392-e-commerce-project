package com.example.isp392.service;

import com.example.isp392.model.*;
import com.example.isp392.repository.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final PromotionService promotionService;
    private final BookService bookService;

    public OrderService(OrderRepository orderRepository, PromotionService promotionService, BookService bookService) {
        this.orderRepository = orderRepository;
        this.promotionService = promotionService;
        this.bookService = bookService;
    }

    /**
     * Lấy danh sách đơn hàng cho một người bán cụ thể.
     * @param sellerId ID của người bán.
     * @return Danh sách đơn hàng.
     */
    public List<Order> getOrdersForSeller(Integer sellerId) {
        return orderRepository.findOrdersBySellerId(sellerId);
    }

    /**
     * Cập nhật trạng thái của một đơn hàng.
     * @param orderId ID của đơn hàng.
     * @param newStatus Trạng thái mới.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     */
    @Transactional
    public boolean updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        return orderRepository.findById(orderId).map(order -> {
            OrderStatus oldStatus = order.getOrderStatus();
            order.setOrderStatus(newStatus);
            
            // Xử lý số lượng sách dựa trên thay đổi trạng thái
            if (oldStatus != newStatus) {
                if (newStatus == OrderStatus.CANCELLED) {
                    // Nếu đơn hàng bị hủy, hoàn lại số lượng sách vào kho
                    for (OrderItem item : order.getOrderItems()) {
                        bookService.increaseStockQuantity(item.getBook().getBook_id(), item.getQuantity());
                    }
                } else if (oldStatus == OrderStatus.CANCELLED && 
                         (newStatus == OrderStatus.PENDING || newStatus == OrderStatus.PROCESSING)) {
                    // Nếu đơn hàng từ trạng thái hủy chuyển sang pending/confirmed, giảm lại số lượng sách
                    for (OrderItem item : order.getOrderItems()) {
                        bookService.decreaseStockQuantity(item.getBook().getBook_id(), item.getQuantity());
                    }
                }
            }
            
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }

    public Optional<Order> findOrderById(Integer orderId) {
        return orderRepository.findByIdWithItems(orderId);
    }

    public Page<Order> findOrders(User user, String status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            orderStatus = OrderStatus.valueOf(status);
        }

        LocalDateTime startDateTime = null;
        if (dateFrom != null) {
            startDateTime = dateFrom.atStartOfDay();
        }

        LocalDateTime endDateTime = null;
        if (dateTo != null) {
            endDateTime = dateTo.plusDays(1).atStartOfDay().minusNanos(1);
        }

        if (orderStatus != null && startDateTime != null && endDateTime != null) {
            return orderRepository.findByUserAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(
                    user, orderStatus, startDateTime, endDateTime, pageable);
        } else if (orderStatus != null) {
            return orderRepository.findByUserAndOrderStatusOrderByOrderDateDesc(user, orderStatus, pageable);
        } else if (startDateTime != null && endDateTime != null) {
            return orderRepository.findByUserAndOrderDateBetweenOrderByOrderDateDesc(
                    user, startDateTime, endDateTime, pageable);
        } else {
            return orderRepository.findByUserOrderByOrderDateDesc(user, pageable);
        }
    }

    public Page<Order> findByUser(User user, Pageable pageable) {
        return orderRepository.findByUserOrderByOrderDateDesc(user, pageable);
    }

    public Page<Order> findByUserAndStatus(User user, OrderStatus status, Pageable pageable) {
        return orderRepository.findByUserAndOrderStatusOrderByOrderDateDesc(user, status, pageable);
    }

    public Page<Order> findByUserAndDate(User user, LocalDate date, Pageable pageable) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return orderRepository.findByUserAndOrderDateBetweenOrderByOrderDateDesc(
                user, startOfDay, endOfDay, pageable);
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
        
        // Validate promotion
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

        // Calculate discount
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
        
        // Record promotion usage
        promotionService.updatePromotionUsage(promotionCode, order.getUser().getUserId());
    }

    /**
     * Lưu đơn hàng và cập nhật số lượng sách trong kho
     * @param order Đơn hàng cần lưu
     * @return Đơn hàng đã được lưu
     */
    @Transactional
    public Order save(Order order) {
        // Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);

        // Cập nhật số lượng sách trong kho
        if (order.getOrderStatus() == OrderStatus.PENDING || order.getOrderStatus() == OrderStatus.PROCESSING) {
            for (OrderItem item : order.getOrderItems()) {
                try {
                    bookService.decreaseStockQuantity(item.getBook().getBook_id(), item.getQuantity());
                } catch (IllegalArgumentException e) {
                    // Nếu có lỗi khi cập nhật số lượng, rollback giao dịch
                    throw new RuntimeException("Không thể cập nhật số lượng sách: " + e.getMessage());
                }
            }
        }

        return savedOrder;
    }
    
    /**
     * Get today's revenue for a shop
     * 
     * @param shopId ID of the shop
     * @return Today's revenue
     */
    public BigDecimal getTodayRevenue(Integer shopId) {
        LocalDate today = LocalDate.now();
        BigDecimal revenue = orderRepository.getTodayRevenue(shopId, today);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    /**
     * Get new orders count for a shop within the last N days
     * 
     * @param shopId ID of the shop
     * @param daysAgo Number of days to look back
     * @return Count of new orders
     */
    public int getNewOrdersCount(Integer shopId, int daysAgo) {
        return orderRepository.getNewOrdersCount(shopId, daysAgo);
    }
    
    /**
     * Get weekly revenue data for a shop
     * 
     * @param shopId ID of the shop
     * @return List of BigDecimal values representing daily revenue for the last 7 days
     */
    public List<BigDecimal> getWeeklyRevenue(Integer shopId) {
        List<Map<String, Object>> results = orderRepository.getWeeklyRevenue(shopId);
        
        // Create a map to store revenue by date
        Map<String, BigDecimal> revenueByDate = new HashMap<>();
        
        // Fill the map with results from the query
        for (Map<String, Object> result : results) {
            String date = (String) result.get("order_date");
            BigDecimal revenue = (BigDecimal) result.get("daily_revenue");
            revenueByDate.put(date, revenue);
        }
        
        // Create a list of the last 7 days
        List<String> last7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            last7Days.add(date.toString());
        }
        
        // Create the final revenue list with 0 for days without data
        List<BigDecimal> weeklyRevenue = new ArrayList<>();
        for (String date : last7Days) {
            BigDecimal revenue = revenueByDate.getOrDefault(date, BigDecimal.ZERO);
            weeklyRevenue.add(revenue);
        }
        
        return weeklyRevenue;
    }
    
    /**
     * Get bestselling books by quantity sold
     * 
     * @param shopId ID of the shop
     * @param limit Maximum number of books to return
     * @return List of maps with book data
     */
    public List<Map<String, Object>> getBestsellingBooks(Integer shopId, int limit) {
        return orderRepository.getBestsellingBooksByQuantity(shopId, limit);
    }
    
    /**
     * Get revenue data for a period (daily, weekly, monthly)
     * 
     * @param shopId ID of the shop
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param period Period type (daily, weekly, monthly)
     * @return List of maps with revenue data
     */
    public List<Map<String, Object>> getRevenueByPeriod(Integer shopId, LocalDate startDate, LocalDate endDate, String period) {
        switch (period) {
            case "daily":
                return orderRepository.getRevenueByDay(shopId, startDate, endDate);
            case "weekly":
                return orderRepository.getRevenueByWeek(shopId, startDate, endDate);
            case "monthly":
            default:
                return orderRepository.getRevenueByMonth(shopId, startDate, endDate);
        }
    }
    
    /**
     * Get recent orders for a shop
     * 
     * @param shopId ID of the shop
     * @param limit Maximum number of orders to return
     * @return List of maps with order data
     */
    public List<Map<String, Object>> getRecentOrders(Integer shopId, int limit) {
        return orderRepository.getRecentOrders(shopId, limit);
    }
    
    /**
     * Get geographic distribution of orders
     * 
     * @param shopId ID of the shop
     * @return List of maps with region and order count
     */
    public List<Map<String, Object>> getGeographicDistribution(Integer shopId) {
        return orderRepository.getGeographicDistribution(shopId);
    }
    public Page<Order> searchOrdersForSeller(Integer sellerId, String keyword, OrderStatus status, String startDate, String endDate, Pageable pageable) {
        Specification<Order> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Predicate bắt buộc: Lọc theo ID người bán
            // Join qua các bảng để lấy được thông tin seller từ order
            predicates.add(criteriaBuilder.equal(root.join("orderItems").join("book").join("shop").get("user").get("userId"), sellerId));

            // ===== START: BỔ SUNG LOGIC LỌC CÒN THIẾU =====

            // 2. Lọc theo từ khóa (keyword)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate keywordPredicate = criteriaBuilder.or(
                        // Tìm theo tên người nhận
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("recipientName")), likePattern),
                        // Tìm theo mã đơn hàng (orderId)
                        criteriaBuilder.like(root.get("orderId").as(String.class), likePattern)
                );
                predicates.add(keywordPredicate);
            }

            // 3. Lọc theo trạng thái (status) - ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("orderStatus"), status));
            }

            // 4. Lọc theo ngày bắt đầu (From Date)
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("orderDate"), start.atStartOfDay()));
                } catch (Exception e) {
                    // Bỏ qua nếu định dạng ngày không hợp lệ
                }
            }

            // 5. Lọc theo ngày kết thúc (To Date)
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    LocalDate end = LocalDate.parse(endDate);
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("orderDate"), end.atTime(LocalTime.MAX)));
                } catch (Exception e) {
                    // Bỏ qua nếu định dạng ngày không hợp lệ
                }
            }

            // ===== END: BỔ SUNG LOGIC LỌC CÒN THIẾU =====

            query.distinct(true); // Đảm bảo không có đơn hàng trùng lặp
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable);
    }
    public Optional<Order> findOrderByIdForSeller(Integer orderId, Integer sellerId) {
        return orderRepository.findOrderByIdForSeller(orderId, sellerId);
    }
    public BigDecimal getTotalRevenue(Integer shopId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = orderRepository.getTotalRevenue(shopId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    public Long getTotalOrders(Integer shopId, LocalDate startDate, LocalDate endDate) {
        Long total = orderRepository.getTotalOrders(shopId, startDate, endDate);
        return total != null ? total : 0L;
    }
}
