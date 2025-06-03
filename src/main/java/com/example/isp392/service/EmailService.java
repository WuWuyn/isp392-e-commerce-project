package com.example.isp392.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Send OTP email for password reset
     * @param toEmail recipient email
     * @param otp one-time password
     * @return true if email was sent successfully
     */
    public boolean sendOtpEmail(String toEmail, String otp) {
        String subject = "Your Password Reset OTP Code";
        String message = "Dear User,\n\n" +
                        "Your OTP code for password reset is: " + otp + "\n\n" +
                        "This code will expire in 5 minutes.\n" +
                        "If you didn't request this, please ignore this email or contact support.\n\n" +
                        "Regards,\nReadHub Team";
        
        return sendEmail(toEmail, subject, message);
    }
    
    /**
     * Send a simple email
     * @param to recipient email
     * @param subject email subject
     * @param text email body
     * @return true if email was sent successfully
     */
    private boolean sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Email sent to: {}", to);
            return true;
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }
}
