package com.example.isp392.model.enums;

/**
 * Status of payment reservations
 */
public enum PaymentReservationStatus {
    PENDING,    // Reservation created, waiting for payment
    CONFIRMED,  // Payment successful, order created
    CANCELLED,  // Payment failed or cancelled by user
    EXPIRED     // Reservation expired without payment
}
