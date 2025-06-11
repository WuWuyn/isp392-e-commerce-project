package com.example.isp392.controller;

import com.example.isp392.model.User;
import com.example.isp392.service.OtpService;
import com.example.isp392.service.OtpService.VerificationResult;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/password-reset")
public class PasswordResetController {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);
    
    private final OtpService otpService;
    private final UserService userService;
    
    public PasswordResetController(OtpService otpService, UserService userService) {
        this.otpService = otpService;
        this.userService = userService;
    }
    
    /**
     * Display forgot password form
     */
    @GetMapping("/forgot")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }
    
    /**
     * Process forgot password request
     */
    @PostMapping("/forgot")
    public String processForgotPasswordRequest(@RequestParam("email") String email, 
                                              HttpSession session, 
                                              RedirectAttributes redirectAttributes) {
        log.info("Password reset requested for email: {}", email);
        
        // Always show success message to prevent user enumeration
        // But only generate OTP if user exists and is not OAuth2
        boolean otpSent = otpService.generateOtpForPasswordReset(email);
        
        if (otpSent) {
            // Store email in session for the OTP verification step
            session.setAttribute("resetEmail", email);
            log.info("OTP sent successfully to: {}", email);
        } else {
            log.info("OTP not sent - user not found or OAuth2 user: {}", email);
        }
        
        // Always redirect to OTP page with success message
        redirectAttributes.addFlashAttribute("successMessage", 
                "If your email exists in our system, an OTP has been sent to it.");
        return "redirect:/password-reset/otp";
    }
    
    /**
     * Display OTP verification form
     */
    @GetMapping("/otp")
    public String showOtpForm(Model model, HttpSession session) {
        // Check if we have an email in session
        if (session.getAttribute("resetEmail") == null) {
            return "redirect:/password-reset/forgot";
        }
        
        return "otp";
    }
    
    /**
     * Process OTP verification
     */
    @PostMapping("/otp")
    public String verifyOtp(@RequestParam("fullOtp") String otp,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("resetEmail");
        
        // If no email in session, redirect to forgot password
        if (email == null) {
            return "redirect:/password-reset/forgot";
        }
        
        // Store OTP in session for the reset password step
        session.setAttribute("verifiedOtp", otp);
        
        // Redirect to new password form
        return "redirect:/password-reset/new-password";
    }
    
    /**
     * Display new password form
     */
    @GetMapping("/new-password")
    public String showNewPasswordForm(HttpSession session, Model model) {
        // Check if we have both email and OTP in session
        if (session.getAttribute("resetEmail") == null || 
            session.getAttribute("verifiedOtp") == null) {
            return "redirect:/password-reset/forgot";
        }
        
        return "reset-password";
    }
    
    /**
     * Process new password submission
     */
    @PostMapping("/reset")
    public String processNewPassword(@RequestParam("newPassword") String newPassword,
                                   @RequestParam("confirmPassword") String confirmPassword,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("resetEmail");
        String otp = (String) session.getAttribute("verifiedOtp");
        
        // If missing email or OTP, redirect to forgot password
        if (email == null || otp == null) {
            return "redirect:/password-reset/forgot";
        }
        
        // Validate password
        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("errorMessage", "Password must be at least 6 characters long.");
            return "reset-password";
        }
        
        // Check password complexity using regex
        if (!newPassword.matches("^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}$")) {
            model.addAttribute("errorMessage", "Password must have at least 6 character long and contain both number and character");
            return "reset-password";
        }
        
        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "reset-password";
        }
        
        // Check if new password is same as old password
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isPresent() && userService.checkIfValidOldPassword(userOpt.get(), newPassword)) {
            model.addAttribute("errorMessage", "New password cannot be the same as your old password.");
            return "reset-password";
        }
        
        // Verify OTP and reset password
        VerificationResult result = otpService.verifyOtpAndResetPassword(otp, newPassword);
        
        // Process result
        switch (result) {
            case SUCCESS:
                // Show success message before redirect
                session.setAttribute("passwordResetSuccess", true);
                // Clean up session data
                session.removeAttribute("resetEmail");
                session.removeAttribute("verifiedOtp");
                
                redirectAttributes.addFlashAttribute("successMessage", 
                        "Your password has been reset successfully. You can now login with your new password.");
                return "redirect:/buyer/login?success=true";
                
            case INVALID_OTP:
                model.addAttribute("errorMessage", "Invalid OTP code. Please try again.");
                return "reset-password";
                
            case OTP_EXPIRED:
                model.addAttribute("errorMessage", "OTP code has expired. Please request a new one.");
                return "reset-password";
                
            case OTP_ALREADY_USED:
                model.addAttribute("errorMessage", "OTP code has already been used. Please request a new one.");
                return "reset-password";
                
            case MAX_ATTEMPTS_REACHED:
                model.addAttribute("errorMessage", "Too many failed attempts. Please request a new OTP code.");
                return "reset-password";
                
            default:
                model.addAttribute("errorMessage", "An error occurred. Please try again.");
                return "reset-password";
        }
    }
    
    /**
     * Resend OTP
     */
    @PostMapping("/resend-otp")
    @ResponseBody
    public String resendOtp(HttpSession session) {
        String email = (String) session.getAttribute("resetEmail");
        
        // If no email in session, return error
        if (email == null) {
            return "{\"success\": false, \"message\": \"Session expired. Please start again.\"}";
        }
        
        boolean otpSent = otpService.generateOtpForPasswordReset(email);
        
        if (otpSent) {
            return "{\"success\": true, \"message\": \"OTP sent successfully.\"}";
        } else {
            return "{\"success\": false, \"message\": \"Failed to send OTP. Please try again.\"}";
        }
    }
}