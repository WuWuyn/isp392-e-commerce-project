package com.example.isp392.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String categoryName;

    @Column(name = "category_description", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String categoryDescription;

    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    private Set<Book> books = new HashSet<>(); // Use Set to avoid duplicate books in a category's list

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

}
