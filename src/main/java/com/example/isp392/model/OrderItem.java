package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Entity
@Getter
@Setter
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private int orderItemId;

    // Many OrderItems belong to One Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Each OrderItem refers to One Book
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, columnDefinition = "BIGINT")
    // Price of a single unit of the book AT THE TIME OF ORDER.
    // This is important because book prices can change.
    private BigInteger unitPrice;

    @Column(name = "subtotal", nullable = false, columnDefinition = "BIGINT")
    // Calculated as quantity * unit_price. Can be calculated or stored.
    // Storing it can simplify some queries but introduces redundancy.
    // For this entity, we'll assume it's calculated and set before saving.
    private BigInteger subtotal;

    // Trong OrderItem.java
    @OneToOne(mappedBy = "orderItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BookReview review;

}