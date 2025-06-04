package com.example.isp392.repository;

import com.example.isp392.model.OtpToken;
import com.example.isp392.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByOtp(String otp);
    Optional<OtpToken> findByUser(User user);
    void deleteByUser(User user);
}
