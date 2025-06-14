package com.example.isp392.model;

// Enum for Order Status
public enum OrderStatus {
    PENDING,        // Order placed, awaiting payment or processing
    PROCESSING,     // Order is being prepared
    SHIPPED,        // Order has been shipped
    DELIVERED,      // Order has been delivered
    CANCELLED,      // Order was cancelled
}
