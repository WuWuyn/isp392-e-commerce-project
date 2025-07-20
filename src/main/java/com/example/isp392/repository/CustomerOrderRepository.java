package com.example.isp392.repository;

import com.example.isp392.model.CustomerOrder;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Integer>, JpaSpecificationExecutor<CustomerOrder> {
    
    /**
     * Find customer orders by user ID ordered by creation date descending
     * @param userId the user ID
     * @return list of customer orders
     */
    List<CustomerOrder> findByUserUserIdOrderByCreatedAtDesc(Integer userId);
    
    /**
     * Find customer orders by user ordered by creation date descending
     * @param user the user
     * @return list of customer orders
     */
    List<CustomerOrder> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Find customer orders by user and status ordered by creation date descending
     * @param user the user
     * @param status the order status
     * @return list of customer orders
     */
    List<CustomerOrder> findByUserAndStatusOrderByCreatedAtDesc(User user, OrderStatus status);
    
    /**
     * Find customer order by ID and user
     * @param customerOrderId the customer order ID
     * @param user the user
     * @return optional customer order
     */
    Optional<CustomerOrder> findByCustomerOrderIdAndUser(Integer customerOrderId, User user);
    
    /**
     * Find customer orders by payment status
     * @param paymentStatus the payment status
     * @return list of customer orders
     */
    @Query("SELECT co FROM CustomerOrder co WHERE co.paymentStatus = :paymentStatus")
    List<CustomerOrder> findByPaymentStatus(@Param("paymentStatus") String paymentStatus);
    
    /**
     * Count customer orders by user
     * @param user the user
     * @return count of customer orders
     */
    long countByUser(User user);
    
    /**
     * Find customer orders with pending payment (for cleanup/monitoring)
     * @return list of customer orders with pending payment
     */
    @Query("SELECT co FROM CustomerOrder co WHERE co.paymentStatus = 'PENDING' AND co.status != 'CANCELLED'")
    List<CustomerOrder> findPendingPaymentOrders();
    
    /**
     * Find customer orders by status
     * @param status the order status
     * @return list of customer orders
     */
    List<CustomerOrder> findByStatus(OrderStatus status);
    
    /**
     * Find customer orders by user and date range
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return list of customer orders
     */
    @Query("SELECT co FROM CustomerOrder co WHERE co.user = :user AND co.createdAt BETWEEN :startDate AND :endDate ORDER BY co.createdAt DESC")
    List<CustomerOrder> findByUserAndDateRange(@Param("user") User user, 
                                              @Param("startDate") java.time.LocalDateTime startDate, 
                                              @Param("endDate") java.time.LocalDateTime endDate);
}
