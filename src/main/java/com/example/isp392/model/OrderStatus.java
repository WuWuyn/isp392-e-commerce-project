package com.example.isp392.model;

/**
 * Enum for Order Status
 *
 * Business Flow with PaymentReservation:
 * 1. User pays → Inventory reserved → Order created with PROCESSING status
 * 2. Seller prepares order → SHIPPED
 * 3. Order delivered → DELIVERED
 * 4. Order can be CANCELLED only when PROCESSING (before shipping)
 *    - Once SHIPPED, order cannot be cancelled
 *    - Once DELIVERED, order cannot be cancelled
 */
public enum OrderStatus {
    PROCESSING,     // Order paid and ready for seller to prepare
    SHIPPED,        // Order has been shipped
    DELIVERED,      // Order has been delivered
    CANCELLED,      // Order was cancelled
}
