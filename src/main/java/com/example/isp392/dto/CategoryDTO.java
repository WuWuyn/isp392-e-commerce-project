package com.example.isp392.dto;

import lombok.Data;

@Data
public class CategoryDTO {
    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private boolean isActive;
} 