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
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_order_id")
    private Integer customerOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "customerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // Shipping information (moved from Order)
    @Column(name = "recipient_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;

    @Column(name = "shipping_province", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String shippingProvince;

    @Column(name = "shipping_district", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String shippingDistrict;

    @Column(name = "shipping_ward", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String shippingWard;

    @Column(name = "shipping_address_detail", columnDefinition = "NVARCHAR(500)", nullable = false)
    private String shippingAddressDetail;

    @Column(name = "shipping_company", columnDefinition = "NVARCHAR(255)")
    private String shippingCompany;

    @Column(name = "shipping_address_type")
    private Integer shippingAddressType;
    //0: Nha rieng, 1: company

    // Payment information (moved from Order)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_url")
    private String paymentUrl;

    // Order totals
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 0)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // Overall status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // Notes for the entire customer order
    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

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
     * Check if the customer order can be cancelled
     * @return true if can be cancelled, false otherwise
     */
    public boolean canCancel() {
        // Can only cancel if status is PENDING or PROCESSING
        return status == OrderStatus.PENDING || status == OrderStatus.PROCESSING;
    }
    
    /**
     * Calculate total amount from all orders
     * @return calculated total amount
     */
    public BigDecimal calculateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        if (orders != null) {
            for (Order order : orders) {
                if (order.getTotalAmount() != null) {
                    total = total.add(order.getTotalAmount());
                }
            }
        }
        return total.add(shippingFee).subtract(discountAmount);
    }
    
    /**
     * Update status based on individual order statuses
     */
    public void updateStatusFromOrders() {
        if (orders == null || orders.isEmpty()) {
            this.status = OrderStatus.PENDING;
            return;
        }
        
        boolean allCancelled = orders.stream().allMatch(order -> order.getOrderStatus() == OrderStatus.CANCELLED);
        boolean allCompleted = orders.stream().allMatch(order -> order.getOrderStatus() == OrderStatus.DELIVERED);
        boolean anyShipped = orders.stream().anyMatch(order -> order.getOrderStatus() == OrderStatus.SHIPPED);
        boolean anyProcessing = orders.stream().anyMatch(order -> order.getOrderStatus() == OrderStatus.PROCESSING);
        
        if (allCancelled) {
            this.status = OrderStatus.CANCELLED;
        } else if (allCompleted) {
            this.status = OrderStatus.DELIVERED;
        } else if (anyShipped) {
            this.status = OrderStatus.SHIPPED;
        } else if (anyProcessing) {
            this.status = OrderStatus.PROCESSING;
        } else {
            this.status = OrderStatus.PENDING;
        }
    }
    
    /**
     * Get the customer order ID
     * @return the customer order ID
     */
    public Integer getCustomerOrderId() {
        return customerOrderId;
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
    
    /**
     * Add an order to this customer order
     * @param order the order to add
     */
    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomerOrder(this);
    }
    
    /**
     * Remove an order from this customer order
     * @param order the order to remove
     */
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setCustomerOrder(null);
    }
}
