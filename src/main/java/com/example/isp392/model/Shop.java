package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "shops")
public class Shop {

    /**
     * Enum representing the approval status of a shop
     */
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        SUSPENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_id")
    private Integer shopId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "shop_name", nullable = false, length = 150, columnDefinition = "NVARCHAR(500)")
    private String shopName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)") // Specific for SQL Server if needed
    private String description;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("processedAt DESC")
    private List<ShopApprovalHistory> approvalHistory = new ArrayList<>();

    @Column(name = "logo_url", columnDefinition = "NVARCHAR(MAX)")
    private String logoUrl;

    @Column(name = "cover_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String coverImageUrl;

    @Column(name = "shop_detail_address", columnDefinition = "NVARCHAR(500)")
    private String shopDetailAddress;

    @Column(name = "shop_ward", columnDefinition = "NVARCHAR(255)")
    private String shopWard;

    @Column(name = "shop_district", columnDefinition = "NVARCHAR(255)")
    private String shopDistrict;

    @Column(name = "shop_province", columnDefinition = "NVARCHAR(255)")
    private String shopProvince;

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "tax_code", nullable = false, length = 15)
    private String taxCode;

    @Column(name = "identification_file_url", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String identificationFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approval_status;

    @Column(name = "reason_for_status", columnDefinition = "NVARCHAR(MAX)")
    private String reasonForStatus;

    @Column(name = "request_at")
    private LocalDateTime requestAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "views_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer viewsCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_approver_id")
    private User adminApproverId;

    // Trường này sẽ được set sau khi đăng ký thành công và được admin phê duyệt
    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();

}

