package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "group_orders")
public class GroupOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_order_id")
    private Integer groupOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "groupOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "payment_url")
    private String paymentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Kiểm tra xem đơn hàng nhóm có thể hủy không
     * @return true nếu có thể hủy, false nếu không
     */
    public boolean canCancel() {
        // Chỉ có thể hủy nếu đơn hàng đang ở trạng thái PENDING hoặc PROCESSING
        return status == OrderStatus.PENDING || status == OrderStatus.PROCESSING;
    }
    
    /**
     * Get the group order ID
     * @return the group order ID
     */
    public Integer getGroupOrderId() {
        return groupOrderId;
    }
    
    /**
     * Get the total amount
     * @return the total amount
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    /**
     * Get the orders
     * @return the orders
     */
    public List<Order> getOrders() {
        return orders;
    }
} 