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

    @Column(name = "vnpay_transaction_id", length = 100)
    private String vnpayTransactionId;

    // Order totals
    @Column(name = "original_total_amount", precision = 18, scale = 0)
    private BigDecimal originalTotalAmount; // Total before discount (subtotal + shipping)

    @Column(name = "final_total_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal finalTotalAmount = BigDecimal.ZERO; // Total after discount (original - discount)

    // Legacy total_amount field for database compatibility
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalAmount = BigDecimal.ZERO; // Should match finalTotalAmount

    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 0)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // Promotion code that was applied to this customer order
    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    // Overall status (aggregated from individual orders)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PROCESSING;

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
        // Can only cancel if status is PROCESSING (before shipping)
        return status == OrderStatus.PROCESSING;
    }
    
    /**
     * Calculate final total amount from all orders
     * @return calculated final total amount
     */
    public BigDecimal calculateFinalTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        if (orders != null) {
            for (Order order : orders) {
                if (order.getTotalAmount() != null) {
                    total = total.add(order.getTotalAmount());
                }
            }
        }
        // Individual order totals already include shipping fees and discounts
        return total;
    }

    /**
     * Calculate total amount (legacy method - redirects to calculateFinalTotalAmount)
     * @return calculated final total amount
     */
    public BigDecimal calculateTotalAmount() {
        return calculateFinalTotalAmount();
    }

    /**
     * Calculate and set the original and final total amounts
     * This method should be called whenever totals are updated
     */
    public void calculateAndSetTotalAmounts() {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalShipping = BigDecimal.ZERO;

        // Calculate subtotal and shipping from orders
        if (orders != null) {
            for (Order order : orders) {
                if (order.getSubTotal() != null) {
                    subtotal = subtotal.add(order.getSubTotal());
                }
                if (order.getShippingFee() != null) {
                    totalShipping = totalShipping.add(order.getShippingFee());
                }
            }
        }

        // Set calculated values
        this.shippingFee = totalShipping;
        this.originalTotalAmount = subtotal.add(totalShipping);
        this.finalTotalAmount = this.originalTotalAmount.subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);
        this.totalAmount = this.finalTotalAmount; // Keep totalAmount in sync
    }
    
    /**
     * Update status based on individual order statuses
     */
    /**
     * Update overall status based on individual order statuses
     * Business logic: CustomerOrder status reflects the "least advanced" individual order status
     */
    public void updateStatusFromOrders() {
        if (orders == null || orders.isEmpty()) {
            this.status = OrderStatus.PROCESSING;
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
        } else {
            // Default to PROCESSING (orders are paid and ready for preparation)
            this.status = OrderStatus.PROCESSING;
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
     * Get the total amount (returns the actual totalAmount field)
     * @return the total amount
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * Set the total amount (sets both totalAmount and finalTotalAmount for consistency)
     * @param totalAmount the total amount to set
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        this.finalTotalAmount = totalAmount; // Keep them in sync
    }

    /**
     * Get the original total amount (before discount)
     * @return the original total amount
     */
    public BigDecimal getOriginalTotalAmount() {
        return originalTotalAmount;
    }

    /**
     * Set the original total amount (before discount)
     * @param originalTotalAmount the original total amount to set
     */
    public void setOriginalTotalAmount(BigDecimal originalTotalAmount) {
        this.originalTotalAmount = originalTotalAmount;
    }

    /**
     * Get the final total amount (after discount)
     * @return the final total amount
     */
    public BigDecimal getFinalTotalAmount() {
        return finalTotalAmount;
    }

    /**
     * Set the final total amount (after discount)
     * @param finalTotalAmount the final total amount to set
     */
    public void setFinalTotalAmount(BigDecimal finalTotalAmount) {
        this.finalTotalAmount = finalTotalAmount;
        this.totalAmount = finalTotalAmount; // Keep them in sync
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
