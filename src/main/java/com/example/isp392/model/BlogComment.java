package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "blog_comments")
@Getter
@Setter
public class BlogComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Integer commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blogPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String content;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    // Kiểm tra xem comment bài viết có bị khóa không?
    @Column(name = "is_hidden", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean isHidden = false;

    @Column(name = "is_approved", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean isApproved = false;

}