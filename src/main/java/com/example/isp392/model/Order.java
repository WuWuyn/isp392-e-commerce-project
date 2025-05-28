package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
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

    @Column(name = "order_date", nullable = false)
    private Timestamp orderDate;

    @Column(name = "total_amount", nullable = false, columnDefinition = "BIGINT") // Example: 12345678.90
    private BigInteger totalAmount;

    @Enumerated(EnumType.STRING) // Store enum name as string, or ORDINAL for integer
    @Column(name = "order_status", length = 50, nullable = false)
    private OrderStatus orderStatus;

    // Shipping Information
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", nullable = false)
    private UserAddress shippingAddress;

    // Billing Information (could be same as shipping, or separate)
    // For simplicity, you might omit these if billing always matches shipping,
    // or add similar fields: billing_address_line1, etc.

    @Column(name = "payment_method", columnDefinition = "NVARCHAR(255)")
    private String paymentMethod; // e.g., "Credit Card", "PayPal", "COD"

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus; // e.g., PENDING, PAID, FAILED

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)") // For longer customer notes
    private String notes;

    // One Order has Many OrderItems
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "updated_at")
    private Timestamp updatedAt;


}

// Enum for Order Status
enum OrderStatus {
    PENDING,        // Order placed, awaiting payment or processing
    PROCESSING,     // Order is being prepared
    SHIPPED,        // Order has been shipped
    DELIVERED,      // Order has been delivered
    CANCELLED,      // Order was cancelled
    RETURNED        // Order was returned
}

// Enum for Payment Status
enum PaymentStatus {
    PENDING,        // Payment initiated but not yet confirmed
    PAID,           // Payment successful
    FAILED,         // Payment attempt failed
    REFUNDED,       // Payment was refunded
}