package com.example.isp392.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "book_reviews")
public class BookReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private int reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Entity User đã được định nghĩa trước đó

    @OneToOne(fetch = FetchType.LAZY) // Hoặc @OneToOne nếu một order_item chỉ có một review
    @JoinColumn(name = "order_item_id", nullable = true) // Có thể NULL nếu cho phép review không cần mua hàng
    // Hoặc `nullable = false` nếu bắt buộc mua hàng
    private OrderItem orderItem; // Entity OrderItem đã được định nghĩa trước đó

    @Column(name = "rating", nullable = false)
    private int rating; // Thường là 1-5

    @Column(name = "title", columnDefinition = "NVARCHAR(500)")
    private String title;

    @Lob // Dùng cho kiểu dữ liệu lớn như NVARCHAR(MAX) hoặc TEXT
    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "created_date", nullable = false)
    private Timestamp createdDate;

    @Column(name = "img_url_1", columnDefinition = "NVARCHAR(MAX)")
    private String imgUrl1;

    @Column(name = "img_url_2", columnDefinition = "NVARCHAR(MAX)")
    private String imgUrl2;

    @Column(name = "img_url_3", columnDefinition = "NVARCHAR(MAX)")
    private String imgUrl3;

}
