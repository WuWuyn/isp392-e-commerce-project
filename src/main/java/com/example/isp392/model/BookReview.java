package com.example.isp392.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

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
    @JoinColumn(name = "book_id", nullable = false)
    private Book book; // Entity Book đã được định nghĩa trước đó

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Entity User đã được định nghĩa trước đó

    @ManyToOne(fetch = FetchType.LAZY) // Hoặc @OneToOne nếu một order_item chỉ có một review
    @JoinColumn(name = "order_item_id", nullable = true) // Có thể NULL nếu cho phép review không cần mua hàng
    // Hoặc `nullable = false` nếu bắt buộc mua hàng
    private OrderItem orderItem; // Entity OrderItem đã được định nghĩa trước đó

    @Column(name = "rating", nullable = false)
    private int rating; // Thường là 1-5

    @Column(name = "title", length = 255)
    private String title;

    @Lob // Dùng cho kiểu dữ liệu lớn như NVARCHAR(MAX) hoặc TEXT
    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "img_url_1", length = 500)
    private String imgUrl1;

    @Column(name = "img_url_2", length = 500)
    private String imgUrl2;

    @Column(name = "img_url_3", length = 500)
    private String imgUrl3;

}
