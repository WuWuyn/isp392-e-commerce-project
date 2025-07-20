package com.example.isp392.dto;

import com.example.isp392.model.OrderStatus;
import lombok.Data;

@Data
public class OrderStatusUpdateDTO {
    private OrderStatus status;
    private String notes;
    private String adminNotes;
    private String refundReason;
} 