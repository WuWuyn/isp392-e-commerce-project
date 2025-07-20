package com.example.isp392.service;

import com.example.isp392.model.OtpToken;
import com.example.isp392.model.User;
import com.example.isp392.repository.OtpTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import com.example.isp392.model.TokenType;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    
    private final UserService userService;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    
    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;
    
    public OtpService(UserService userService, 
                     OtpTokenRepository otpTokenRepository,
                     EmailService emailService) {
        this.userService = userService;
        this.otpTokenRepository = otpTokenRepository;
        this.emailService = emailService;
    }
    
    /**
     * Generate OTP for password reset and send to user's email
     * @param email User's email
     * @return true if OTP was generated and sent, false otherwise
     */
    @Transactional
    public boolean generateOtpForPasswordReset(String email) {
        // Check if user exists and is not an OAuth2 user
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent user: {}", email);
            return false;
        }
        
        User user = userOpt.get();
        
        // Don't allow password reset for OAuth2 users
        if (user.isOAuth2User()) {
            log.info("Password reset requested for OAuth2 user: {}", email);
            return false;
        }
        
        // Find existing OTP token for this user
        Optional<OtpToken> existingTokenOpt = otpTokenRepository.findByUserAndTokenType(user, TokenType.PASSWORD_RESET);
        OtpToken otpToken;
        
        // Generate 6-digit OTP
        String otp = generateOtpCode();
        
        if (existingTokenOpt.isPresent()) {
            // Update existing token
            otpToken = existingTokenOpt.get();
            otpToken.setToken(otp);
            otpToken.setExpiryDate(LocalDateTime.now().plusSeconds(OtpToken.EXPIRATION));
            otpToken.setUsed(false);
            otpToken.setAttempts(0);
        } else {
            // Create new token
            otpToken = new OtpToken(otp, user, TokenType.PASSWORD_RESET);
        }
        
        // Save the token
        otpTokenRepository.save(otpToken);
        
        // Send OTP via email
        return emailService.sendOtpEmail(user.getEmail(), otp);
    }

    /**
     * Generate a unique token for a specific purpose (e.g., account deletion, shop deletion).
     * @param user The user associated with the token.
     * @param tokenType The type of token to generate.
     * @return The generated token string.
     */
    @Transactional
    public String generateToken(User user, TokenType tokenType) {
        // Invalidate any existing tokens of the same type for this user
        otpTokenRepository.findByUserAndTokenType(user, tokenType)
                .ifPresent(otpTokenRepository::delete);

        String token = generateUuidToken();
        OtpToken newToken = new OtpToken(token, user, tokenType);
        otpTokenRepository.save(newToken);
        return token;
    }
    
    /**
     * Generate OTP for a specific purpose with custom expiration time
     * @param email User's email
     * @param tokenTypeStr String representation of token type
     * @param expirationHours Expiration time in hours
     * @return Generated OTP token
     */
    @Transactional
    public String generateOtp(String email, String tokenTypeStr, int expirationHours) {
        // Find user by email
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        
        // Convert string to TokenType enum
        TokenType tokenType;
        try {
            tokenType = TokenType.valueOf(tokenTypeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token type: " + tokenTypeStr);
        }
        
        // Invalidate any existing tokens of the same type for this user
        otpTokenRepository.findByUserAndTokenType(user, tokenType)
                .ifPresent(otpTokenRepository::delete);
        
        // Generate token
        String token = generateUuidToken();
        
        // Create new token with custom expiration
        OtpToken newToken = new OtpToken(token, user, tokenType);
        newToken.setExpiryDate(LocalDateTime.now().plusHours(expirationHours));
        
        // Save token
        otpTokenRepository.save(newToken);
        
        return token;
    }
    
    /**
     * Verify OTP and reset password if valid
     * @param otp OTP code
     * @param newPassword New password
     * @return Verification result
     */
    @Transactional
    public VerificationResult verifyOtpAndResetPassword(String otp, String newPassword) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findByTokenAndTokenType(otp, TokenType.OTP);
        
        // OTP not found
        if (tokenOpt.isEmpty()) {
            return VerificationResult.INVALID_OTP;
        }
        
        OtpToken token = tokenOpt.get();
        
        // OTP already used
        if (token.isUsed()) {
            return VerificationResult.OTP_ALREADY_USED;
        }
        
        // OTP expired
        if (token.isExpired()) {
            return VerificationResult.OTP_EXPIRED;
        }
        
        // Increment attempts and check if max attempts reached
        token.incrementAttempts();
        if (token.getAttempts() > maxAttempts) {
            otpTokenRepository.save(token);
            return VerificationResult.MAX_ATTEMPTS_REACHED;
        }
        
        // Save the token with incremented attempts (if verification fails)
        if (!otp.equals(token.getToken())) {
            otpTokenRepository.save(token);
            return VerificationResult.INVALID_OTP;
        }
        
        // OTP is valid, reset password
        User user = token.getUser();
        userService.changeUserPassword(user, newPassword);
        
        // Mark OTP as used
        token.setUsed(true);
        otpTokenRepository.save(token);
        
        return VerificationResult.SUCCESS;
    }

    /**
     * Validate a token (e.g., for account deletion confirmation).
     * @param tokenString The token string to validate.
     * @param tokenType The type of token expected.
     * @return An Optional containing the User if the token is valid, empty otherwise.
     */
    @Transactional
    public Optional<User> validateToken(String tokenString, TokenType tokenType) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findByTokenAndTokenType(tokenString, tokenType);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        OtpToken token = tokenOpt.get();

        if (token.isUsed() || token.isExpired()) {
            return Optional.empty();
        }

        // Mark token as used after successful validation
        token.setUsed(true);
        otpTokenRepository.save(token);

        return Optional.of(token.getUser());
    }

    /**
     * Generate 6-digit OTP code
     * @return 6-digit OTP code
     */
    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Generate a unique UUID token.
     * @return A UUID string.
     */
    private String generateUuidToken() {
        return java.util.UUID.randomUUID().toString();
    }
    
    /**
     * Verification result enum
     */
    public enum VerificationResult {
        SUCCESS,
        INVALID_OTP,
        OTP_EXPIRED,
        OTP_ALREADY_USED,
        MAX_ATTEMPTS_REACHED
    }
}
