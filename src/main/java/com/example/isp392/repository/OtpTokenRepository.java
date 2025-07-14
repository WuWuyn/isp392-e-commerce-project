package com.example.isp392.repository;

import com.example.isp392.model.OtpToken;
import com.example.isp392.model.User;
import com.example.isp392.model.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByToken(String token);
    Optional<OtpToken> findByUser(User user);
    Optional<OtpToken> findByUserAndTokenType(User user, TokenType tokenType);
    Optional<OtpToken> findByTokenAndTokenType(String token, TokenType tokenType);
    void deleteByUser(User user);
}
