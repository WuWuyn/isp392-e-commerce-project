package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Integer orderItemId;

    // Many OrderItems belong to One Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Each OrderItem refers to One Book
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 0)
    // Price of a single unit of the book AT THE TIME OF ORDER.
    // This is important because book prices can change.
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 18, scale = 0)
    // Calculated as quantity * unit_price. Can be calculated or stored.
    // Storing it can simplify some queries but introduces redundancy.
    // For this entity, we'll assume it's calculated and set before saving.
    private BigDecimal subtotal;

    // Trong OrderItem.java
    @OneToOne(mappedBy = "orderItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BookReview review;

}