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

    // --- Chức năng Xóa Tài khoản ---

    /**
     * Tạo và gửi một mã OTP cho mục đích xóa tài khoản.
     * Phương thức này sẽ tìm hoặc tạo một token mới và gửi nó qua email.
     * @param user Đối tượng User đang yêu cầu xóa tài khoản.
     * @return true nếu OTP được gửi thành công, false nếu thất bại.
     */
    @Transactional
    public boolean generateAndSendOtpForAccountDeletion(User user) {
        String otp = generateOtp();

        OtpToken otpToken = otpTokenRepository.findByUser(user)
                .orElse(new OtpToken(otp, user));

        // Cập nhật token với mã OTP mới và reset lại trạng thái
        otpToken.setOtp(otp);
        otpToken.setExpiryDate(new Date(System.currentTimeMillis() + OtpToken.EXPIRATION));
        otpToken.setUsed(false);
        otpToken.setAttempts(0);

        otpTokenRepository.save(otpToken);

        return emailService.sendOtpEmail(user.getEmail(), otp);
    }

    /**
     * Xác thực mã OTP do người dùng cung cấp cho việc xóa tài khoản.
     * @param providedOtp Mã OTP người dùng nhập vào.
     * @param user Người dùng đang thực hiện xác thực.
     * @return true nếu OTP hợp lệ và chưa hết hạn/sử dụng, ngược lại trả về false.
     */
    @Transactional
    public boolean validateOtpForUser(String providedOtp, User user) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findByUser(user);

        if (tokenOpt.isEmpty()) {
            log.warn("Attempt to validate OTP for user {} but no token found.", user.getEmail());
            return false;
        }

        OtpToken token = tokenOpt.get();

        if (token.isUsed() || token.isExpired() || !token.getOtp().equals(providedOtp)) {
            token.incrementAttempts();
            otpTokenRepository.save(token);
            log.warn("Invalid OTP attempt for user {}. OTP used: {}, expired: {}, provided: {}, actual: {}",
                    user.getEmail(), token.isUsed(), token.isExpired(), providedOtp, token.getOtp());
            return false;
        }

        token.setUsed(true);
        otpTokenRepository.save(token);

        return true;
    }

    // --- Chức năng Đặt lại Mật khẩu ---

    /**
     * Tạo OTP cho việc đặt lại mật khẩu và gửi đến email của người dùng.
     * @param email Email của người dùng.
     * @return true nếu OTP được tạo và gửi thành công, false nếu không.
     */
    @Transactional
    public boolean generateOtpForPasswordReset(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || userOpt.get().isOAuth2User()) {
            log.info("Password reset requested for non-existent or OAuth2 user: {}", email);
            return false;
        }

        User user = userOpt.get();
        String otp = generateOtp();

        OtpToken otpToken = otpTokenRepository.findByUser(user)
                .orElse(new OtpToken(otp, user));

        otpToken.setOtp(otp);
        otpToken.setExpiryDate(new Date(System.currentTimeMillis() + OtpToken.EXPIRATION));
        otpToken.setUsed(false);
        otpToken.setAttempts(0);

        otpTokenRepository.save(otpToken);

        return emailService.sendOtpEmail(user.getEmail(), otp);
    }

    /**
     * Xác thực OTP và đặt lại mật khẩu nếu hợp lệ.
     * @param otp Mã OTP.
     * @param newPassword Mật khẩu mới.
     * @return Kết quả xác thực (SUCCESS, INVALID_OTP, etc.).
     */
    @Transactional
    public VerificationResult verifyOtpAndResetPassword(String otp, String newPassword) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findByOtp(otp);

        if (tokenOpt.isEmpty()) {
            return VerificationResult.INVALID_OTP;
        }

        OtpToken token = tokenOpt.get();

        if (token.isUsed()) return VerificationResult.OTP_ALREADY_USED;
        if (token.isExpired()) return VerificationResult.OTP_EXPIRED;

        token.incrementAttempts();
        if (token.getAttempts() > maxAttempts) {
            otpTokenRepository.save(token);
            return VerificationResult.MAX_ATTEMPTS_REACHED;
        }

        if (!otp.equals(token.getOtp())) {
            otpTokenRepository.save(token);
            return VerificationResult.INVALID_OTP;
        }

        User user = token.getUser();
        userService.changeUserPassword(user, newPassword);

        token.setUsed(true);
        otpTokenRepository.save(token);

        return VerificationResult.SUCCESS;
    }

    // --- Phương thức private và Enum ---

    /**
     * Tạo một mã OTP ngẫu nhiên gồm 6 chữ số.
     * @return Chuỗi OTP 6 chữ số.
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Enum định nghĩa các kết quả có thể xảy ra khi xác thực OTP (dùng cho đặt lại mật khẩu).
     */
    public enum VerificationResult {
        SUCCESS,
        INVALID_OTP,
        OTP_EXPIRED,
        OTP_ALREADY_USED,
        MAX_ATTEMPTS_REACHED
    }
}
