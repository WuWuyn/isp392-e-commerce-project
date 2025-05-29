package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_id")
    private int shopId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "shop_name", length = 150, nullable = false)
    private String shopName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)") // Specific for SQL Server if needed
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "identification_file_url", length = 500)
    private String identificationFileUrl;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "registration_date", nullable = false, updatable = false)
    private Date registrationDate;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();

}
