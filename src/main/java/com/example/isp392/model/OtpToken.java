package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@Getter
@Setter
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String token;
    
    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int attempts = 0;
    
    @Column(nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean used = false;

    @Column(name = "token_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;
    
    // Default 5-minute expiration (in seconds)
    public static final int EXPIRATION = 5 * 60;
    
    // Add setter for expiryDate
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public OtpToken() {
    }
    
    public OtpToken(String token, User user, TokenType tokenType) {
        this.token = token;
        this.user = user;
        this.tokenType = tokenType;
        this.expiryDate = LocalDateTime.now().plusSeconds(EXPIRATION);
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
    
    public void incrementAttempts() {
        this.attempts++;
    }
}
