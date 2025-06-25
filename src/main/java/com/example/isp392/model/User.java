package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "role"})
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String fullName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    /**
     * Gender: 0 - Male, 1 - Female, 2 - Other
     */
    @Column(name = "gender", nullable = false)
    private int gender;

    @Column(name = "profile_pic_url", columnDefinition = "NVARCHAR(MAX)")
    private String profilePicUrl;

    @Column(name = "is_oauth2_user", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean isOAuth2User = false;

    @Column(name = "is_active")
    private boolean isActive = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<UserAddress> addresses = new HashSet<>(); // Danh sách các địa chỉ của user

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Cart cart;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Shop shop;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookReview> reviewsWritten = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Blog> blogPosts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BlogComment> blogComments;
}