package com.example.isp392.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for user registration
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).*$", message = "Password must contain at least one letter and one number")
    private String password;
    
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    private String phoneNumber;
    
    @NotBlank(message = "Date of birth is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in format YYYY-MM-DD")
    private String dateOfBirth;
    
    @NotBlank(message = "Gender is required")
    private String gender;
}
