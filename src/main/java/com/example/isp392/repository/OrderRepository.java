package com.example.isp392.repository;

import com.example.isp392.model.Order;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity
 * Provides methods for CRUD operations on orders
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    /**
     * Find all orders for a specific user
     * @param userId the user ID
     * @return list of orders belonging to the user
     */
    List<Order> findByUserUserIdOrderByOrderDateDesc(Integer userId);
    
    /**
     * Find a specific order by ID and user ID (for security)
     * @param orderId the order ID
     * @param userId the user ID
     * @return the order if found and belongs to the user
     */
    Optional<Order> findByOrderIdAndUserUserId(Integer orderId, Integer userId);
    
    /**
     * Count the number of orders for a user
     * @param userId the user ID
     * @return count of orders for the user
     */
    long countByUserUserId(Integer userId);
    
    /**
     * Find recent orders for a user, limited to a specific count
     * @param userId the user ID
     * @param limit the maximum number of orders to return
     * @return list of recent orders for the user
     */
    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId ORDER BY o.orderDate DESC")
    List<Order> findRecentOrdersByUserId(@Param("userId") Integer userId, @Param("limit") int limit);

    /**
     * Find all orders for a specific seller by navigating through book->shop->user relationship
     * @param sellerId the seller ID (user ID of the seller)
     * @return list of orders belonging to the seller
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.book b WHERE b.shop.user.userId = :sellerId")
    List<Order> findOrdersBySellerId(@Param("sellerId") Integer sellerId);

    /**
     * Find a specific order by ID and eagerly fetch its items.
     * @param orderId the order ID
     * @return the order with its items if found
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Integer orderId);

    Page<Order> findByUserOrderByOrderDateDesc(User user, Pageable pageable);

    Page<Order> findByUserAndOrderStatusOrderByOrderDateDesc(User user, OrderStatus status, Pageable pageable);

    Page<Order> findByUserAndOrderDateBetweenOrderByOrderDateDesc(
            User user, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Order> findByUserAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(
            User user, OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Optional<Order> findByOrderIdAndUser(Integer orderId, User user);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.book b WHERE b.shop.user.userId = :sellerId AND (:status IS NULL OR o.orderStatus = :status) AND (:dateFrom IS NULL OR o.orderDate >= :dateFrom) AND (:dateTo IS NULL OR o.orderDate <= :dateTo)")
    Page<Order> findSellerOrders(
            @Param("sellerId") Integer sellerId,
            @Param("status") OrderStatus status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);
} 