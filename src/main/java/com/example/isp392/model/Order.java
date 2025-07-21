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
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    //Tổng tiền của tất cả sản phẩm
    @Column(name = "sub_total", nullable = false, precision = 18, scale = 0)
    private BigDecimal subTotal;

    //Phí ship
    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 0)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    //Tiền khi áp mã giảm giá
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    //Mã giảm giá được áp dụng
    @Column(name = "discount_code", length = 50)
    private String discountCode;

    //sub_total + shipping_fee - discount_amount
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 50, nullable = false)
    private OrderStatus orderStatus = OrderStatus.PROCESSING;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "NVARCHAR(MAX)")
    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Kiểm tra xem đơn hàng có thể hủy không
     * @return true nếu có thể hủy, false nếu không
     */
    public boolean canCancel() {
        // Chỉ có thể hủy nếu đơn hàng đang ở trạng thái PROCESSING (chưa ship)
        return orderStatus == OrderStatus.PROCESSING;
    }
    
    /**
     * Get the total amount
     * @return the total amount
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    /**
     * Get the order items
     * @return the order items
     */
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }
    

    
    /**
     * Set the shop
     * @param shop the shop to set
     */
    public void setShop(Shop shop) {
        this.shop = shop;
    }
    
    /**
     * Set the customer order
     * @param customerOrder the customer order to set
     */
    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }
    

    

    
    /**
     * Set the sub total
     * @param subTotal the sub total to set
     */
    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }
    
    /**
     * Set the shipping fee
     * @param shippingFee the shipping fee to set
     */
    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }
    
    /**
     * Set the discount amount
     * @param discountAmount the discount amount to set
     */
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    /**
     * Set the total amount
     * @param totalAmount the total amount to set
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    /**
     * Set the order status
     * @param orderStatus the order status to set
     */
    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
    

    
    /**
     * Set the notes
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Set the order date
     * @param orderDate the order date to set
     */
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    
    /**
     * Set the order items
     * @param orderItems the order items to set
     */
    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}

