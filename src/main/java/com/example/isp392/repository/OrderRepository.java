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
    @Query("SELECT o FROM Order o WHERE o.customerOrder.user.userId = :userId ORDER BY o.orderDate DESC")
    List<Order> findByUserUserIdOrderByOrderDateDesc(@Param("userId") Integer userId);
    
    /**
     * Find a specific order by ID and user ID (for security)
     * @param orderId the order ID
     * @param userId the user ID
     * @return the order if found and belongs to the user
     */
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.customerOrder.user.userId = :userId")
    Optional<Order> findByOrderIdAndUserUserId(@Param("orderId") Integer orderId, @Param("userId") Integer userId);
    
    /**
     * Count the number of orders for a user
     * @param userId the user ID
     * @return count of orders for the user
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customerOrder.user.userId = :userId")
    long countByUserUserId(@Param("userId") Integer userId);
    
    /**
     * Find recent orders for a user, limited to a specific count
     * @param userId the user ID
     * @param limit the maximum number of orders to return
     * @return list of recent orders for the user
     */
    @Query("SELECT o FROM Order o WHERE o.customerOrder.user.userId = :userId ORDER BY o.orderDate DESC")
    List<Order> findRecentOrdersByUserId(@Param("userId") Integer userId, @Param("limit") int limit);


    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.book b WHERE b.shop.shopId = :shopId")
    List<Order> findOrdersByShopId(@Param("shopId") Integer shopId);

    /**
     * Find a specific order by ID and eagerly fetch its items.
     * @param orderId the order ID
     * @return the order with its items if found
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Integer orderId);

    @Query("SELECT o FROM Order o WHERE o.customerOrder.user = :user ORDER BY o.orderDate DESC")
    Page<Order> findByUserOrderByOrderDateDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.customerOrder.user = :user AND o.orderStatus = :status ORDER BY o.orderDate DESC")
    Page<Order> findByUserAndOrderStatusOrderByOrderDateDesc(@Param("user") User user, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.customerOrder.user = :user AND o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    Page<Order> findByUserAndOrderDateBetweenOrderByOrderDateDesc(
            @Param("user") User user, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.customerOrder.user = :user AND o.orderStatus = :status AND o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    Page<Order> findByUserAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(
            @Param("user") User user, @Param("status") OrderStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.customerOrder.user = :user")
    Optional<Order> findByOrderIdAndUser(@Param("orderId") Integer orderId, @Param("user") User user);
    
    // New methods for shop-specific orders
    List<Order> findByShopShopId(Integer shopId);
    
    List<Order> findByShopShopIdOrderByOrderDateDesc(Integer shopId, Pageable pageable);
    
    int countByShopShopIdAndOrderDateAfter(Integer shopId, LocalDateTime startDate);
    
    Long countByShopShopIdAndOrderDateBetween(Integer shopId, LocalDateTime startDate, LocalDateTime endDate);
    Long countByShopShopIdAndOrderDateBetweenAndOrderStatus(Integer shopId, LocalDateTime startDate, LocalDateTime endDate, OrderStatus status);
    long countByShopShopIdAndOrderStatus(Integer shopId, OrderStatus status);
    List<Order> findByShopShopIdAndOrderDateBetweenAndOrderStatus(
            Integer shopId, LocalDateTime startDate, LocalDateTime endDate, OrderStatus status);
    
    Optional<Order> findByOrderIdAndShopUserUserId(Integer orderId, Integer userId);

    /**
     * Get today's revenue for a shop
     * 
     * @param shopId ID of the shop
     * @param today Today's date
     * @return Total revenue for today
     */
    @Query(value = "SELECT COALESCE(sum(o.sub_total), 0) " +
            "FROM orders o " +
            "JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
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
            "JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
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
            "JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
            "JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN books b ON oi.book_id = b.book_id " +
            "WHERE b.shop_id = :shopId " +
            "AND o.order_date >= DATEADD(day, -6, CONVERT(date, GETDATE())) " +
            "AND o.order_status = 'DELIVERED' " +
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
     * Get revenue data for daily periods
     * 
     * @param shopId ID of the shop
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @return List of revenue data grouped by day
     */
    @Query(value = "SELECT " +
            "CONVERT(varchar, CONVERT(date, o.order_date), 120) AS time_period, " +
            "SUM(oi.unit_price * oi.quantity) AS revenue, " +
            "COUNT(DISTINCT o.order_id) AS order_count " +
            "FROM orders o " +
            "JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN books b ON oi.book_id = b.book_id " +
            "WHERE b.shop_id = :shopId " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "AND o.order_status = 'DELIVERED' " +
            "GROUP BY CONVERT(varchar, CONVERT(date, o.order_date), 120) " +
            "ORDER BY time_period", nativeQuery = true)
    List<Map<String, Object>> getRevenueByDay(
            @Param("shopId") Integer shopId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get revenue data for weekly periods
     * 
     * @param shopId ID of the shop
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @return List of revenue data grouped by week
     */
    @Query(value = "SELECT " +
            "CONCAT(YEAR(o.order_date), '-W', DATEPART(week, o.order_date)) AS time_period, " +
            "SUM(oi.unit_price * oi.quantity) AS revenue, " +
            "COUNT(DISTINCT o.order_id) AS order_count " +
            "FROM orders o " +
            "JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN books b ON oi.book_id = b.book_id " +
            "WHERE b.shop_id = :shopId " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "AND o.order_status = 'DELIVERED' " +
            "GROUP BY CONCAT(YEAR(o.order_date), '-W', DATEPART(week, o.order_date)) " +
            "ORDER BY time_period", nativeQuery = true)
    List<Map<String, Object>> getRevenueByWeek(
            @Param("shopId") Integer shopId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get revenue data for monthly periods
     *
     * @param shopId ID of the shop
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @return List of revenue data grouped by month
     */
    @Query(value = "SELECT " +
            "CONCAT(YEAR(o.order_date), '-', FORMAT(o.order_date, 'MM')) AS time_period, " +
            "SUM(oi.unit_price * oi.quantity) AS revenue, " +
            "COUNT(DISTINCT o.order_id) AS order_count " +
            "FROM orders o " +
            "JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN books b ON oi.book_id = b.book_id " +
            "WHERE b.shop_id = :shopId " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "AND o.order_status = 'DELIVERED' " +
            "GROUP BY CONCAT(YEAR(o.order_date), '-', FORMAT(o.order_date, 'MM')) " +
            "ORDER BY time_period", nativeQuery = true)
    List<Map<String, Object>> getRevenueByMonth(
            @Param("shopId") Integer shopId, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
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
            "JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
            "JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN books b ON oi.book_id = b.book_id " +
            "JOIN users u ON co.user_id = u.user_id " +
            "WHERE b.shop_id = :shopId " +
            "ORDER BY o.order_date DESC", nativeQuery = true)
    List<Map<String, Object>> getRecentOrders(@Param("shopId") Integer shopId, @Param("limit") int limit);

    /**
     * Get geographic distribution of orders
     *
     * @param shopId ID of the shop
     * @return List of regions with order counts
     */
    @Query(value = "SELECT " +
            "COALESCE(co.shipping_province, 'Unknown') AS region, " +
            "COUNT(DISTINCT o.order_id) AS order_count " +
            "FROM orders o " +
            "JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
            "JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN books b ON oi.book_id = b.book_id " +
            "WHERE b.shop_id = :shopId " +
            "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED') " +
            "GROUP BY co.shipping_province " +
            "ORDER BY COUNT(DISTINCT o.order_id) DESC", nativeQuery = true)
    List<Map<String, Object>> getGeographicDistribution(@Param("shopId") Integer shopId);

    /**
     * Get total revenue of orders
     *
     * @param shopId ID of the shop
     * @param startDate the start of date
     * @param endDate the end of date
     * @return Bigdeciam of total revenue by shop ID
     */
    @Query(value = "SELECT COALESCE(SUM(oi.unit_price * oi.quantity), 0) " +
            "FROM orders o " +
            "JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN books b ON oi.book_id = b.book_id " +
            "WHERE b.shop_id = :shopId AND o.order_date BETWEEN :startDate AND :endDate " +
            "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')",
            nativeQuery = true)
    BigDecimal getTotalRevenue(@Param("shopId") Integer shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.book b WHERE o.orderId = :orderId AND b.shop.user.userId = :sellerId")
    Optional<Order> findOrderByIdForSeller(@Param("orderId") Integer orderId, @Param("sellerId") Integer sellerId);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.customerOrder LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.book WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithCustomerOrder(@Param("orderId") Integer orderId);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.customerOrder JOIN FETCH o.orderItems oi JOIN FETCH oi.book b WHERE o.orderId = :orderId AND b.shop.user.userId = :sellerId")
    Optional<Order> findOrderByIdForSellerWithCustomerOrder(@Param("orderId") Integer orderId, @Param("sellerId") Integer sellerId);

    @Query(value = "SELECT COUNT(DISTINCT o.order_id) " +
            "FROM orders o " +
            "JOIN order_items oi ON o.order_id = oi.order_id " +
            "JOIN books b ON oi.book_id = b.book_id " +
            "WHERE b.shop_id = :shopId AND o.order_date BETWEEN :startDate AND :endDate " +
            "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')",
            nativeQuery = true)
    Long getTotalOrders(@Param("shopId") Integer shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customerOrder.user.userId = :userId AND o.orderStatus IN ('PENDING', 'PROCESSING', 'SHIPPED')")
    long countActiveOrdersByUserId(@Param("userId") Integer userId);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.orderItems oi JOIN oi.book b WHERE b.shop.shopId = :shopId AND o.orderStatus IN ('PENDING', 'PROCESSING', 'SHIPPED')")
    long countActiveOrdersByShopId(@Param("shopId") Integer shopId);
    
    /**
     * Calculate the total value of all orders (except cancelled/refunded)
     * Used for platform revenue calculations
     * 
     * @return Total order value
     */
    @Query(value = "SELECT COALESCE(SUM(final_total_amount), 0) FROM customer_orders " +
           "WHERE status NOT IN ('CANCELLED', 'REFUNDED')",
           nativeQuery = true)
    BigDecimal calculateTotalOrderValue();
    
    /**
     * Get monthly revenue for the platform
     * 
     * @param startDate Start date for the data
     * @param endDate End date for the data
     * @return List of monthly revenue data
     */
    @Query(value = "SELECT FORMAT(order_date, 'yyyy-MM') AS month, " +
            "COALESCE(SUM(total_amount), 0) AS revenue " +
            "FROM orders " +
            "WHERE order_status NOT IN ('CANCELLED', 'REFUNDED') " +
            "AND order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY FORMAT(order_date, 'yyyy-MM') " +
            "ORDER BY month",
            nativeQuery = true)
    List<Map<String, Object>> getMonthlyRevenue(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get top selling products across the platform
     *
     * @param limit Maximum number of products to return
     * @return List of top selling products with sales data
     */
    @Query(value = "SELECT TOP(:limit) b.book_id, b.title, " +
            "SUM(oi.quantity) AS total_quantity, " +
            "SUM(oi.unit_price * oi.quantity) AS total_revenue, " +
            "s.shop_name AS shop_name " +
            "FROM books b " +
            "JOIN order_items oi ON b.book_id = oi.book_id " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
            "JOIN shops s ON b.shop_id = s.shop_id " +
            // MODIFICATION: Changed WHERE clause to only count DELIVERED orders
            "WHERE o.order_status = 'DELIVERED' " +
            "GROUP BY b.book_id, b.title, s.shop_name " +
            "ORDER BY SUM(oi.quantity) DESC",
            nativeQuery = true)
    List<Map<String, Object>> getTopSellingProducts(@Param("limit") int limit);

    /**
     * Get top sellers by revenue
     *
     * @param limit Maximum number of sellers to return
     * @return List of top sellers with revenue data
     */
    @Query(value = "SELECT TOP(:limit) s.shop_id, s.shop_name, u.full_name AS seller_name, " +
            "COUNT(DISTINCT o.order_id) AS total_orders, " +
            "SUM(oi.unit_price * oi.quantity) AS total_revenue " +
            "FROM shops s " +
            "JOIN books b ON s.shop_id = b.shop_id " +
            "JOIN users u ON s.user_id = u.user_id " +
            "JOIN order_items oi ON b.book_id = oi.book_id " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
            "WHERE o.order_status NOT IN ('CANCELLED', 'REFUNDED') " +
            "GROUP BY s.shop_id, s.shop_name, u.full_name " +
            "ORDER BY SUM(oi.unit_price * oi.quantity) DESC",
            nativeQuery = true)
    List<Map<String, Object>> getTopSellers(@Param("limit") int limit);

    /**
     * Get recent orders across the platform
     *
     * @param limit Maximum number of orders to return
     * @return List of recent orders
     */
    @Query(value = "SELECT TOP(:limit) co.customer_order_id AS order_id, co.created_at AS order_date, co.status, " +
           "co.final_total_amount AS total_amount, u.full_name AS customer_name " +
           "FROM customer_orders co " +
           "JOIN users u ON co.user_id = u.user_id " +
           "ORDER BY co.created_at DESC",
           nativeQuery = true)
    List<Map<String, Object>> getRecentPlatformOrders(@Param("limit") int limit);

    /**
     * Count orders placed within the specified number of days
     *
     * @param days Number of days to look back
     * @return Count of new orders
     */
    @Query(value = "SELECT COUNT(*) FROM orders " +
            "WHERE order_date >= DATEADD(day, -:days, GETDATE())",
            nativeQuery = true)
    long countOrdersInLastDays(@Param("days") int days);

    /**
     * Calculate total platform revenue for a time period
     * 
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param commissionRate Platform commission rate (decimal)
     * @return Total platform revenue
     */
    @Query(value = "SELECT COALESCE(SUM(final_total_amount * :commissionRate), 0) " +
           "FROM customer_orders " +
           "WHERE status NOT IN ('CANCELLED', 'REFUNDED') " +
           "AND CAST(created_at AS DATE) BETWEEN :startDate AND :endDate",
           nativeQuery = true)
    BigDecimal calculatePlatformRevenue(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("commissionRate") BigDecimal commissionRate);

    /**
     * Count total orders across the platform.
     * @return Total count of orders.
     */
    @Query(value = "SELECT COUNT(*) FROM customer_orders", nativeQuery = true)
    long countAllOrders();

    /**
     * Get daily platform revenue data for a date range
     * 
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param commissionRate Platform commission rate (decimal)
     * @return List of daily revenue data
     */
    @Query(value = "SELECT " +
            "CAST(created_at AS DATE) AS order_date, " +
            "COALESCE(SUM(total_amount * :commissionRate), 0) AS daily_revenue " +
            "FROM customer_orders " +
            "WHERE status NOT IN ('CANCELLED', 'REFUNDED') " +
            "AND CAST(created_at AS DATE) BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(created_at AS DATE) " +
            "ORDER BY CAST(created_at AS DATE)",
            nativeQuery = true)
    List<Map<String, Object>> getDailyPlatformRevenue(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("commissionRate") BigDecimal commissionRate);

    /**
     * Get top sellers by revenue for a specific period
     *
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param commissionRate Platform commission rate (decimal)
     * @param limit Maximum number of sellers to return
     * @return List of top sellers with revenue data
     */
    @Query(value = "SELECT TOP(:limit) " +
            "s.shop_name, u.full_name AS seller_name, " +
            "COALESCE(SUM(co.total_amount), 0) AS total_sales, " +
            "COALESCE(SUM(co.total_amount * :commissionRate), 0) AS platform_revenue, " +
            "COUNT(DISTINCT co.customer_order_id) AS order_count, " +
            "COALESCE(AVG(co.total_amount), 0) AS average_order_value " +
            "FROM shops s " +
            "JOIN users u ON s.user_id = u.user_id " +
            "LEFT JOIN orders o ON s.shop_id = o.shop_id " +
            "LEFT JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
            "WHERE (co.status IS NULL OR co.status NOT IN ('CANCELLED', 'REFUNDED')) " +
            "AND (co.created_at IS NULL OR CAST(co.created_at AS DATE) BETWEEN :startDate AND :endDate) " +
            "GROUP BY s.shop_id, s.shop_name, u.full_name " +
            "ORDER BY total_sales DESC",
            nativeQuery = true)
    List<Map<String, Object>> getTopSellersByRevenue(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("commissionRate") BigDecimal commissionRate,
            @Param("limit") int limit);

    /**
     * Get detailed revenue information for a specific seller
     *
     * @param sellerId Seller's shop ID
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param commissionRate Platform commission rate (decimal)
     * @return List of seller revenue details
     */
    @Query(value = "SELECT " +
            "s.shop_id, s.shop_name, u.full_name AS seller_name, " +
            "COALESCE(SUM(co.total_amount), 0) AS total_sales, " +
            "COALESCE(SUM(co.total_amount * :commissionRate), 0) AS platform_revenue, " +
            "COUNT(DISTINCT co.customer_order_id) AS order_count, " +
            "COALESCE(AVG(co.total_amount), 0) AS average_order_value " +
            "FROM shops s " +
            "JOIN users u ON s.user_id = u.user_id " +
            "LEFT JOIN orders o ON s.shop_id = o.shop_id " +
            "LEFT JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
            "WHERE s.shop_id = :sellerId " +
            "AND (co.status IS NULL OR co.status NOT IN ('CANCELLED', 'REFUNDED')) " +
            "AND (co.created_at IS NULL OR CAST(co.created_at AS DATE) BETWEEN :startDate AND :endDate) " +
            "GROUP BY s.shop_id, s.shop_name, u.full_name",
            nativeQuery = true)
    List<Map<String, Object>> getSellerRevenueDetails(
            @Param("sellerId") Integer sellerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("commissionRate") BigDecimal commissionRate);

    /**
     * Count orders by date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Count of orders
     */
    @Query(value = "SELECT COUNT(*) FROM customer_orders " +
            "WHERE status NOT IN ('CANCELLED', 'REFUNDED') " +
            "AND CAST(created_at AS DATE) BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    long countOrdersByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total order value by date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Total order value
     */
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM customer_orders " +
            "WHERE status NOT IN ('CANCELLED', 'REFUNDED') " +
            "AND CAST(created_at AS DATE) BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    BigDecimal calculateTotalOrderValueByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT TOP(:limit) b.title, SUM(oi.quantity) as total_quantity, SUM(oi.unit_price * oi.quantity) as total_revenue " +
            "FROM order_items oi JOIN books b ON oi.book_id = b.book_id JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE o.order_status = 'DELIVERED' AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY b.title ORDER BY total_revenue DESC", nativeQuery = true)
    List<Map<String, Object>> getTopSellingProductsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o " +
            "WHERE o.orderStatus = com.example.isp392.model.OrderStatus.DELIVERED " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Long sumQuantitiesByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    /**
     * Count active sellers by date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Count of active sellers
     */
    @Query(value = "SELECT COUNT(DISTINCT s.shop_id) " +
            "FROM shops s " +
            "JOIN orders o ON s.shop_id = o.shop_id " +
            "JOIN customer_orders co ON o.customer_order_id = co.customer_order_id " +
            "WHERE co.status NOT IN ('CANCELLED', 'REFUNDED') " +
            "AND CAST(co.created_at AS DATE) BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    long countActiveSellersByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count total orders for a user
     */
    Long countByCustomerOrderUser(User user);

    /**
     * Count orders by user and status
     */
    Long countByCustomerOrderUserAndOrderStatus(User user, OrderStatus orderStatus);

    /**
     * Find orders by user and status
     */
    List<Order> findByCustomerOrderUserAndOrderStatus(User user, OrderStatus orderStatus);

}
