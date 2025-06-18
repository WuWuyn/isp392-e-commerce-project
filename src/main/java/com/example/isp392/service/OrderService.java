package com.example.isp392.service;

import com.example.isp392.model.*;
import com.example.isp392.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
     * Find all orders for a specific seller with pagination and filtering.
     * @param sellerId ID of the seller.
     * @param status Optional order status to filter by.
     * @param dateFrom Optional start date to filter by.
     * @param dateTo Optional end date to filter by.
     * @param pageable Pagination and sorting information.
     * @return Page of orders belonging to the seller.
     */
    public Page<Order> findSellerOrders(Integer sellerId, String status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
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

        return orderRepository.findSellerOrders(sellerId, orderStatus, startDateTime, endDateTime, pageable);
    }
}
