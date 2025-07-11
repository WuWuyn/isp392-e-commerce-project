package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blogs")
@Getter
@Setter
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id")
    private Integer blogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "views_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer viewsCount = 0;

    // Kiểm tra xem bài viết có bị khóa không?
    @Column(name = "is_locked", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean isLocked = false;

    @Column(name = "is_pinned", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean isPinned = false;

    @OneToMany(mappedBy = "blogPost", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BlogComment> comments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_category_id")
    private BlogCategory category;
}
