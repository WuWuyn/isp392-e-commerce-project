package com.example.isp392.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class BookFormDTO {
    private Integer shopId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotNull(message = "Publication date is required")
    @PastOrPresent(message = "Publication date cannot be in the future")
    private LocalDate publicationDate;
    
    @NotBlank(message = "ISBN is required")
    private String isbn;
    
    @NotNull(message = "Number of pages is required")
    @Min(value = 1, message = "Book must have at least 1 page")
    private Integer numberOfPages;
    
    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @NotBlank(message = "Author(s) is required")
    private String authors;
    
    private MultipartFile coverImageFile;
    
    @NotNull(message = "Publisher is required")
    private Integer publisherId;
    
    @NotBlank(message = "Dimensions are required")
    @Pattern(regexp = "^\\d+x\\d+x\\d+(\\.\\d+)?$", message = "Dimensions must be in format: 16x24x4.2")
    private String dimensions;
    
    @NotBlank(message = "SKU is required")
    private String sku;
    
    @NotNull(message = "Original price is required")
    @DecimalMin(value = "1000", message = "Original price must be at least 1,000 VND")
    private BigDecimal originalPrice;
    
    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "1000", message = "Selling price must be at least 1,000 VND")
    private BigDecimal sellingPrice;
    
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    
    @NotEmpty(message = "At least one category must be selected")
    private List<Integer> categoryIds;
} 