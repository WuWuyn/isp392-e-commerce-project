package com.example.isp392.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for inventory report data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportDTO {
    private Integer bookId;
    private String title;
    private String authors;
    private String isbn;
    private String sku;
    private Integer stockQuantity;
    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;
    private String coverImgUrl;
    private Boolean active;

    /**
     * Get stock status based on stock quantity
     * @return Stock status string
     */
    public String getStockStatus() {
        if (stockQuantity == null || stockQuantity == 0) {
            return "OUT_OF_STOCK";
        } else if (stockQuantity <= 5) {
            return "LOW_STOCK";
        } else {
            return "IN_STOCK";
        }
    }
}
