package com.example.isp392.repository;

import com.example.isp392.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    /**
     * Tìm tất cả các đơn hàng (Order) có chứa ít nhất một sản phẩm (Book)
     * thuộc về một cửa hàng (Shop) của người bán (User) cụ thể.
     * Luồng quan hệ: Order -> OrderItem -> Book -> Shop -> User
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.book b JOIN b.shop s WHERE s.user.userId = :sellerId ORDER BY o.orderDate DESC")
    List<Order> findOrdersBySellerId(@Param("sellerId") Integer sellerId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Integer orderId);
}