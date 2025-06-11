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
    @JoinColumn(name = "user_id", nullable = false) // An order must belong to a user
    private User user;

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
    private BigDecimal shippingFee;

    //Tiền khi áp mã giảm giá
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal discountAmount;

    //sub_total + shipping_fee - discount_amount
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 0) // Example: 12345678.90
    private BigDecimal totalAmount;


    // Lưu ý món này
    @Enumerated(EnumType.STRING) // Store enum name as string, or ORDINAL for integer
    @Column(name = "order_status", length = 50, nullable = false)
    private OrderStatus orderStatus;

    // Billing Information (could be same as shipping, or separate)
    // For simplicity, you might omit these if billing always matches shipping,
    // or add similar fields: billing_address_line1, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod; // e.g., "Credit Card", "PayPal", "COD"

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus; // e.g., PENDING, PAID, FAILED

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)") // For longer customer notes
    private String notes;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    // One Order has Many OrderItems
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "NVARCHAR(MAX)")
    private String cancellationReason;
}