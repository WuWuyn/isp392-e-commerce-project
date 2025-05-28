package com.example.isp392.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    //@OneToMany with Book

}
