package com.example.isp392.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual inventory update
 */
@Data
@NoArgsConstructor
public class InventoryUpdateDTO {
    @NotNull(message = "Book ID is required")
    private Integer bookId;
    
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    
    private String reason;
    private String notes;
}
