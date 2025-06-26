package com.example.isp392.repository;

import com.example.isp392.model.Order;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    /**
     * Get today's revenue for a shop
     * 
     * @param shopId ID of the shop
     * @param today Today's date
     * @return Total revenue for today
     */
    @Query(value = "SELECT COALESCE(sum(o.sub_total), 0) " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND CONVERT(date, o.order_date) = :today " +
           "AND o.order_status NOT IN ('CANCELLED')", nativeQuery = true)
    BigDecimal getTodayRevenue(@Param("shopId") Integer shopId, @Param("today") LocalDate today);
    
    /**
     * Get new orders count for a shop within the last N days
     * 
     * @param shopId ID of the shop
     * @param daysAgo Number of days to look back
     * @return Count of new orders
     */
    @Query(value = "SELECT COUNT(DISTINCT o.order_id) " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_date >= DATEADD(day, -:daysAgo, GETDATE())", nativeQuery = true)
    int getNewOrdersCount(@Param("shopId") Integer shopId, @Param("daysAgo") int daysAgo);
    
    /**
     * Get weekly revenue data for a shop
     * 
     * @param shopId ID of the shop
     * @return List of daily revenue for the last 7 days
     */
    @Query(value = "SELECT CONVERT(varchar, CONVERT(date, o.order_date), 120) AS order_date, " +
           "COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS daily_revenue " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_date >= DATEADD(day, -6, CONVERT(date, GETDATE())) " +
           "AND o.order_status NOT IN ('CANCELLED') " +
           "GROUP BY CONVERT(date, o.order_date), CONVERT(varchar, CONVERT(date, o.order_date), 120) " +
           "ORDER BY CONVERT(date, o.order_date)", nativeQuery = true)
    List<Map<String, Object>> getWeeklyRevenue(@Param("shopId") Integer shopId);
    
    /**
     * Get bestselling books by quantity sold
     * 
     * @param shopId ID of the shop
     * @param limit Maximum number of books to return
     * @return List of bestselling books with sales data
     */
    @Query(value = "SELECT TOP(:limit) b.book_id, b.title, SUM(oi.quantity) as total_quantity, " +
           "SUM(oi.unit_price * oi.quantity) as total_revenue " +
           "FROM books b " +
           "JOIN order_items oi ON b.book_id = oi.book_id " +
           "JOIN orders o ON oi.order_id = o.order_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED') " +
           "GROUP BY b.book_id, b.title " +
           "ORDER BY SUM(oi.quantity) DESC", nativeQuery = true)
    List<Map<String, Object>> getBestsellingBooksByQuantity(@Param("shopId") Integer shopId, @Param("limit") int limit);
    
    /**
     * Get revenue data for a period (daily, weekly, monthly)
     * 
     * @param shopId ID of the shop
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param groupBy Group by clause (day, week, month)
     * @return List of revenue data grouped by the specified period
     */
    @Query(value = "SELECT " +
           "CASE " +
           "  WHEN :groupBy = 'day' THEN CONVERT(varchar, CONVERT(date, o.order_date), 120) " +
           "  WHEN :groupBy = 'week' THEN CONCAT(YEAR(o.order_date), '-W', DATEPART(week, o.order_date)) " +
           "  WHEN :groupBy = 'month' THEN CONCAT(YEAR(o.order_date), '-', FORMAT(o.order_date, 'MM')) " +
           "END AS time_period, " +
           "SUM(oi.unit_price * oi.quantity) AS revenue, " +
           "COUNT(DISTINCT o.order_id) AS order_count " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_date BETWEEN :startDate AND :endDate " +
           "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED') " +
           "GROUP BY " +
           "CASE " +
           "  WHEN :groupBy = 'day' THEN CONVERT(varchar, CONVERT(date, o.order_date), 120) " +
           "  WHEN :groupBy = 'week' THEN CONCAT(YEAR(o.order_date), '-W', DATEPART(week, o.order_date)) " +
           "  WHEN :groupBy = 'month' THEN CONCAT(YEAR(o.order_date), '-', FORMAT(o.order_date, 'MM')) " +
           "END " +
           "ORDER BY time_period", nativeQuery = true)
    List<Map<String, Object>> getRevenueByPeriod(
            @Param("shopId") Integer shopId, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate,
            @Param("groupBy") String groupBy);
    
    /**
     * Get recent orders for a shop
     * 
     * @param shopId ID of the shop
     * @param limit Maximum number of orders to return
     * @return List of recent orders
     */
    @Query(value = "SELECT DISTINCT TOP(:limit) o.order_id, o.order_date, o.order_status, " +
           "u.full_name AS customer_name, " +
           "(SELECT SUM(oi2.unit_price * oi2.quantity) FROM order_items oi2 WHERE oi2.order_id = o.order_id) AS total_amount " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "JOIN users u ON o.user_id = u.user_id " +
           "WHERE b.shop_id = :shopId " +
           "ORDER BY o.order_date DESC", nativeQuery = true)
    List<Map<String, Object>> getRecentOrders(@Param("shopId") Integer shopId, @Param("limit") int limit);
    
    /**
     * Get geographic distribution of orders
     * 
     * @param shopId ID of the shop
     * @return List of order counts by region
     */
    @Query(value = "SELECT " +
           "COALESCE(o.shipping_province, 'Unknown') AS region, " +
           "COUNT(DISTINCT o.order_id) AS order_count " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED') " +
           "GROUP BY o.shipping_province " +
           "ORDER BY COUNT(DISTINCT o.order_id) DESC", nativeQuery = true)
    List<Map<String, Object>> getGeographicDistribution(@Param("shopId") Integer shopId);

    /**
     * Get total revenue for a shop within a date range
     *
     * @param shopId ID of the shop
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @return Total revenue for the shop within the date range
     */
    @Query(value = "SELECT COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS total_revenue " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_date BETWEEN :startDate AND :endDate " +
           "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')", nativeQuery = true)
    BigDecimal getTotalRevenue(@Param("shopId") Integer shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get total orders count for a shop within a date range
     *
     * @param shopId ID of the shop
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @return Total number of orders for the shop within the date range
     */
    @Query(value = "SELECT COUNT(DISTINCT o.order_id) AS total_orders " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_date BETWEEN :startDate AND :endDate " +
           "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')", nativeQuery = true)
    Integer getTotalOrders(@Param("shopId") Integer shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.book b WHERE o.orderId = :orderId AND b.shop.user.userId = :sellerId")
    Optional<Order> findOrderByIdForSeller(@Param("orderId") Integer orderId, @Param("sellerId") Integer sellerId);
}