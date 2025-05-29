package com.example.isp392.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private int book_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String title;

    @Column(name = "publication_date")
    private Date publicationDate;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(name = "number_of_pages")
    private int numberOfPages;

    @Lob
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Lob // For authors, if it can be very long.
    @Column(name = "authors", columnDefinition = "NVARCHAR(MAX)") // Storing authors as a single string.
    // Consider a separate Author entity and a ManyToMany relationship
    // if you need to manage authors individually.
    private String authors;

    @Column(name = "cover_img_url", length = 500)
    private String coverImgUrl;

    // Foreign Key relationship to Publisher
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id") // Can be nullable if a book might not have a publisher
    private Publisher publisher;

    @Column(name = "dimensions", columnDefinition = "NVARCHAR(20)")
    private String dimensions;

    @Column(name = "sku", columnDefinition = "VARCHAR(50)")
    private String sku;

    @Column(name = "average_rating", precision = 3, scale = 2) // decimal(3,2) means 3 total digits, 2 after decimal
    private BigDecimal averageRating;

    @Column(name = "total_reviews")
    private int totalReviews;

    @Column(name = "date_added")
    private Date dateAdded;

    @Column(name = "original_price", columnDefinition = "BIGINT")
    private BigInteger originalPrice;

    @Column(name = "selling_price", columnDefinition = "BIGINT")
    private BigInteger sellingPrice;

    @Column(name = "stock_quantity")
    private int stockQuantity;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "book_categories", // Name of the join table
            joinColumns = @JoinColumn(name = "book_id"),         // Foreign key for Book in join table
            inverseJoinColumns = @JoinColumn(name = "category_id") // Foreign key for Category in join table
    )
    private Set<Category> categories = new HashSet<>();
}
