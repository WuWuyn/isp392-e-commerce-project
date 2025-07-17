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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = true)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_order_id", nullable = true)
    private GroupOrder groupOrder;

    // Thông tin người nhận hàng
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

    //Tổng tiền của tất cả sản phẩm
    @Column(name = "sub_total", nullable = false, precision = 18, scale = 0)
    private BigDecimal subTotal;

    //Phí ship
    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 0)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    //Tiền khi áp mã giảm giá
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal discountAmount;

    //sub_total + shipping_fee - discount_amount
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 50, nullable = false)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus;

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
        // Chỉ có thể hủy nếu đơn hàng đang ở trạng thái PENDING hoặc PROCESSING
        return orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PROCESSING;
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
     * Set the user
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }
    
    /**
     * Set the shop
     * @param shop the shop to set
     */
    public void setShop(Shop shop) {
        this.shop = shop;
    }
    
    /**
     * Set the group order
     * @param groupOrder the group order to set
     */
    public void setGroupOrder(GroupOrder groupOrder) {
        this.groupOrder = groupOrder;
    }
    
    /**
     * Set the recipient name
     * @param recipientName the recipient name to set
     */
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
    
    /**
     * Set the recipient phone
     * @param recipientPhone the recipient phone to set
     */
    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }
    
    /**
     * Set the shipping province
     * @param shippingProvince the shipping province to set
     */
    public void setShippingProvince(String shippingProvince) {
        this.shippingProvince = shippingProvince;
    }
    
    /**
     * Set the shipping district
     * @param shippingDistrict the shipping district to set
     */
    public void setShippingDistrict(String shippingDistrict) {
        this.shippingDistrict = shippingDistrict;
    }
    
    /**
     * Set the shipping ward
     * @param shippingWard the shipping ward to set
     */
    public void setShippingWard(String shippingWard) {
        this.shippingWard = shippingWard;
    }
    
    /**
     * Set the shipping address detail
     * @param shippingAddressDetail the shipping address detail to set
     */
    public void setShippingAddressDetail(String shippingAddressDetail) {
        this.shippingAddressDetail = shippingAddressDetail;
    }
    
    /**
     * Set the shipping company
     * @param shippingCompany the shipping company to set
     */
    public void setShippingCompany(String shippingCompany) {
        this.shippingCompany = shippingCompany;
    }
    
    /**
     * Set the shipping address type
     * @param shippingAddressType the shipping address type to set
     */
    public void setShippingAddressType(Integer shippingAddressType) {
        this.shippingAddressType = shippingAddressType;
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
     * Set the payment method
     * @param paymentMethod the payment method to set
     */
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    /**
     * Set the payment status
     * @param paymentStatus the payment status to set
     */
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
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

