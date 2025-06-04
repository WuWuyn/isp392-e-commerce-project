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
import java.util.Date;
import java.util.Optional;

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
        Optional<OtpToken> existingTokenOpt = otpTokenRepository.findByUser(user);
        OtpToken otpToken;
        
        // Generate 6-digit OTP
        String otp = generateOtp();
        
        if (existingTokenOpt.isPresent()) {
            // Update existing token
            otpToken = existingTokenOpt.get();
            otpToken.setOtp(otp);
            otpToken.setExpiryDate(new Date(System.currentTimeMillis() + OtpToken.EXPIRATION));
            otpToken.setUsed(false);
            otpToken.setAttempts(0);
        } else {
            // Create new token
            otpToken = new OtpToken(otp, user);
        }
        
        // Save the token
        otpTokenRepository.save(otpToken);
        
        // Send OTP via email
        return emailService.sendOtpEmail(user.getEmail(), otp);
    }
    
    /**
     * Verify OTP and reset password if valid
     * @param otp OTP code
     * @param newPassword New password
     * @return Verification result
     */
    @Transactional
    public VerificationResult verifyOtpAndResetPassword(String otp, String newPassword) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findByOtp(otp);
        
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
        if (!otp.equals(token.getOtp())) {
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
     * Generate 6-digit OTP
     * @return 6-digit OTP code
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
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
