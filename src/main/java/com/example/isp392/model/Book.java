package com.example.isp392.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


@Getter
@Setter
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Integer book_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String title;

    @Column(name = "normalized_title", length = 500)
    private String normalizedTitle;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Lob
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "views_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer viewsCount = 0;

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

    @Column(name = "average_rating", precision = 2, scale = 1) // decimal(3,1) means 3 total digits, 2 after decimal
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    private Integer totalReviews;

    @Column(name = "date_added")
    private LocalDate dateAdded;

    @Column(name = "original_price", nullable = false, precision = 18, scale = 0)
    private BigDecimal originalPrice;

    @Column(name = "selling_price", precision = 18, scale = 0)
    private BigDecimal sellingPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    // Kiểm tra xem comment bài viết có bị khóa không?
    @Column(name = "is_active", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean isActive = false;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "book_categories", // Name of the join table
            joinColumns = @JoinColumn(name = "book_id"),         // Foreign key for Book in join table
            inverseJoinColumns = @JoinColumn(name = "category_id") // Foreign key for Category in join table
    )
    private Set<Category> categories = new HashSet<>();

    @PrePersist
    @PreUpdate
    public void updateNormalizedTitle() {
        if (this.title != null && !this.title.isEmpty()) {
            String normalized = Normalizer.normalize(this.title, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            this.normalizedTitle = pattern.matcher(normalized).replaceAll("").toLowerCase();
        } else {
            this.normalizedTitle = null;
        }
    }
}
