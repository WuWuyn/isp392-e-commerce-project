package com.example.isp392.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk inventory updates
 */
@Data
@NoArgsConstructor
public class BulkInventoryUpdateDTO {
    @NotEmpty(message = "Inventory updates list cannot be empty")
    @Valid
    private List<InventoryUpdateDTO> inventoryUpdates;
    
    private String globalReason;
    private String globalNotes;
}
