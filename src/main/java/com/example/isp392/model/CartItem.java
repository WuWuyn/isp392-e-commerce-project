package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Integer cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = true)
    private Shop shop;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "selected")
    private Boolean selected = false;

    @PrePersist
    protected void onCreate() {
        if (book != null && book.getShop() != null) {
            shop = book.getShop();
        }
    }
    
    /**
     * Get the book associated with this cart item
     * @return the book
     */
    public Book getBook() {
        return this.book;
    }
    
    /**
     * Get the quantity of this cart item
     * @return the quantity
     */
    public Integer getQuantity() {
        return this.quantity;
    }
}
