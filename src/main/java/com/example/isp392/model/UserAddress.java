package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "user_address")
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private int addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;

    @Column(name = "province", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String province;

    @Column(name = "district", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String district;

    @Column(name = "ward", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String ward;

    @Column(name = "address_detail", columnDefinition = "NVARCHAR(500)", nullable = false)
    private String addressDetail;

    @Column(name = "company", columnDefinition = "NVARCHAR(255)")
    private String company;

    @Column(name = "address_type")
    private int address_type;       //0: Nha rieng, 1: company

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

}
