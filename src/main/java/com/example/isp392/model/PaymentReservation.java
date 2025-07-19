package com.example.isp392.model;

import com.example.isp392.model.enums.PaymentReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to track payment reservations before order creation
 * This prevents phantom orders when users abandon payment
 */
@Entity
@Table(name = "payment_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Integer reservationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "vnpay_txn_ref", unique = true)
    private String vnpayTxnRef;
    
    @Column(name = "reservation_data", columnDefinition = "NVARCHAR(MAX)")
    private String reservationData; // JSON string containing cart items, shipping info, etc.
    
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentReservationStatus status;
    
    @Column(name = "shipping_address_detail", length = 500)
    private String shippingAddressDetail;
    
    @Column(name = "shipping_ward")
    private String shippingWard;
    
    @Column(name = "shipping_district")
    private String shippingDistrict;
    
    @Column(name = "shipping_province")
    private String shippingProvince;
    
    @Column(name = "recipient_name")
    private String recipientName;
    
    @Column(name = "recipient_phone")
    private String recipientPhone;
    
    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            // Default 15 minutes expiration for VNPay
            expiresAt = createdAt.plusMinutes(15);
        }
        if (status == null) {
            status = PaymentReservationStatus.PENDING;
        }
    }
    
    /**
     * Check if this reservation has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if this reservation can be confirmed
     */
    public boolean canBeConfirmed() {
        return status == PaymentReservationStatus.PENDING && !isExpired();
    }
    
    /**
     * Mark reservation as confirmed
     */
    public void confirm() {
        if (!canBeConfirmed()) {
            throw new IllegalStateException("Cannot confirm reservation in current state: " + status);
        }
        this.status = PaymentReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
    
    /**
     * Mark reservation as cancelled
     */
    public void cancel() {
        if (status == PaymentReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel confirmed reservation");
        }
        this.status = PaymentReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
    
    /**
     * Mark reservation as expired
     */
    public void expire() {
        if (status == PaymentReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot expire confirmed reservation");
        }
        this.status = PaymentReservationStatus.EXPIRED;
        this.cancelledAt = LocalDateTime.now();
    }
}
