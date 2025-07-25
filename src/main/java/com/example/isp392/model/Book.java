package com.example.isp392.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private Integer bookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String title;

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
    
    /**
     * Phương thức tương thích ngược để đảm bảo code cũ vẫn hoạt động
     * @return ID của sách
     */
    public Integer getBook_id() {
        return this.bookId;
    }
    
    /**
     * Kiểm tra xem giá bán có hợp lệ không (không lớn hơn giá gốc)
     * @return true nếu giá bán hợp lệ, false nếu không
     */
    public boolean isValidPrice() {
        if (originalPrice == null || sellingPrice == null) {
            return false;
        }
        return sellingPrice.compareTo(originalPrice) <= 0;
    }
    
    /**
     * Tính phần trăm giảm giá
     * @return Phần trăm giảm giá, hoặc 0 nếu không có giảm giá
     */
    public int getDiscountPercentage() {
        if (originalPrice == null || sellingPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        if (originalPrice.equals(sellingPrice)) {
            return 0;
        }
        
        BigDecimal discount = originalPrice.subtract(sellingPrice);
        BigDecimal percentage = discount.multiply(new BigDecimal(100)).divide(originalPrice, 0, RoundingMode.HALF_UP);
        return percentage.intValue();
    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean getActive() {
        return this.isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    /**
     * Phương thức tương thích ngược để đảm bảo code cũ vẫn hoạt động
     * @return ID của sách
     */
    public Integer getBookId() {
        return this.bookId;
    }
}