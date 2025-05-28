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
    private int categoryId;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

    @Column(name = "category_description")
    private String categoryDescription;

    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    private Set<Book> books = new HashSet<>(); // Use Set to avoid duplicate books in a category's list


}
