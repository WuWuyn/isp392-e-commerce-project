package com.example.isp392.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigInteger;
import java.util.Date;
import java.util.Set;

@Data // Annotation của Lombok để tự tạo getter/setter, toString...
public class ProductDTO {

    @NotEmpty(message = "Book title cannot be empty.")
    @Size(max = 500, message = "Title cannot be longer than 500 characters.")
    private String title;

    private String authors;

    @NotNull(message = "Publisher must be selected.")
    private Integer publisherId;

    @NotEmpty(message = "Please select at least one category.")
    private Set<Integer> categoryIds;

    @Lob
    private String description;

    @NotNull(message = "Stock quantity is required.")
    @Min(value = 0, message = "Stock quantity cannot be negative.")
    private Integer stockQuantity;

    @NotNull(message = "Selling price is required.")
    private BigInteger sellingPrice;

    private BigInteger originalPrice;

    // Các trường khác bạn muốn thêm vào form
    private String isbn;
    private Integer numberOfPages;
    private Date publicationDate;
    private String dimensions;
    private String sku;
}