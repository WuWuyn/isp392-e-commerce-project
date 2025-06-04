package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "otp_tokens")
@Getter
@Setter
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String otp;
    
    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private Date expiryDate;
    
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int attempts = 0;
    
    @Column(nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean used = false;
    
    // Default 5-minute expiration (in milliseconds)
    public static final int EXPIRATION = 5 * 60 * 1000;
    
    // Add setter for expiryDate
    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public OtpToken() {
    }
    
    public OtpToken(String otp, User user) {
        this.otp = otp;
        this.user = user;
        this.expiryDate = new Date(System.currentTimeMillis() + EXPIRATION);
    }
    
    public boolean isExpired() {
        return new Date().after(this.expiryDate);
    }
    
    public void incrementAttempts() {
        this.attempts++;
    }
}
