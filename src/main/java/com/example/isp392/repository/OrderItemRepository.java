package com.example.isp392.repository;

import com.example.isp392.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OrderItem entity
 * Provides methods for CRUD operations on order items
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    
    /**
     * Find all order items for a specific order
     * @param orderId the order ID
     * @return list of order items belonging to the order
     */
    List<OrderItem> findByOrderOrderId(Integer orderId);
    
    /**
     * Find all order items for orders belonging to a specific user
     * @param userId the user ID
     * @return list of order items from the user's orders
     */
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE o.customerOrder.user.userId = :userId")
    List<OrderItem> findByUserUserId(@Param("userId") Integer userId);
    
    /**
     * Count the number of times a user has purchased a specific book
     * @param userId the user ID
     * @param bookId the book ID
     * @return count of purchases of the book by the user
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o WHERE o.customerOrder.user.userId = :userId AND oi.book.bookId = :bookId")
    Integer countBookPurchasesByUser(@Param("userId") Integer userId, @Param("bookId") Integer bookId);
} 