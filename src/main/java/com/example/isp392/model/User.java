package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

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
    private int id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 255)
    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    private Date dateOfBirth;

    @Column(name = "gender", nullable = false)
    private boolean gender;

    @Column(name = "profile_pic_url")
    private String profilePicUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "registration_date", nullable = false)
    private Date registrationDate;

    @Column(name = "role_id", nullable = false)
    private int roleID;     //Coi 1: Buyer, 2: Seller, 3: Admin

    // Constructors
    public User() {
        this.registrationDate = new Date();
        this.isActive = true;
    }

}