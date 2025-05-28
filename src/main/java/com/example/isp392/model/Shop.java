package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_id")
    private int shopId;


    @Column(name = "shop_name", nullable = false, length = 255)
    private String shopName;

    @Column(name = "shop_description")
    private String shopDescription;

    @Column(name = "logo_url")
    private String logoUrl;

    //Not done yet
}
