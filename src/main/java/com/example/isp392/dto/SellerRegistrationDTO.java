package com.example.isp392.dto;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class SellerRegistrationDTO {

    // Store Information
    @NotBlank(message = "Shop name cannot be empty")
    @Size(min = 2, max = 100, message = "Shop name must be between 2-100 characters")
    private String shopName;

    @NotBlank(message = "Please select shop category")
    private String shopCategory;

    @NotBlank(message = "Shop description cannot be empty")
    @Size(min = 10, max = 1000, message = "Description must be between 10-1000 characters")
    private String shopDescription;

    private MultipartFile shopLogoFile;
    private String shopLogoPath;

    // Contact Information
    @NotBlank(message = "Full name cannot be empty")
    @Size(min = 2, max = 50, message = "Full name must be between 2-50 characters")
    private String contactName;

    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Invalid phone number")
    private String contactPhone;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email")
    private String contactEmail;

    @NotBlank(message = "Address cannot be empty")
    @Size(min = 10, max = 200, message = "Address must be between 10-200 characters")
    private String contactAddress;

    // Tax Information & Verification
    @NotBlank(message = "Tax code cannot be empty")
    private String taxCode;

    private MultipartFile identificationFile;
    private String identificationFilePath;

    private MultipartFile businessLicenseFile; // ✅ Added field
    private String businessLicenseFilePath;

    // Terms & Conditions
    @AssertTrue(message = "You must agree to the terms")
    private boolean agreeTerms;

    private boolean agreeMarketing;

    // System Information
    private LocalDateTime registrationDate;
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
    private String rejectionReason;

    // Constructor
    public SellerRegistrationDTO() {
        this.registrationDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopCategory() {
        return shopCategory;
    }

    public void setShopCategory(String shopCategory) {
        this.shopCategory = shopCategory;
    }

    public String getShopDescription() {
        return shopDescription;
    }

    public void setShopDescription(String shopDescription) {
        this.shopDescription = shopDescription;
    }

    public MultipartFile getShopLogoFile() {
        return shopLogoFile;
    }

    public void setShopLogoFile(MultipartFile shopLogoFile) {
        this.shopLogoFile = shopLogoFile;
    }

    public String getShopLogoPath() {
        return shopLogoPath;
    }

    public void setShopLogoPath(String shopLogoPath) {
        this.shopLogoPath = shopLogoPath;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public MultipartFile getIdentificationFile() {
        return identificationFile;
    }

    public void setIdentificationFile(MultipartFile identificationFile) {
        this.identificationFile = identificationFile;
    }

    public String getIdentificationFilePath() {
        return identificationFilePath;
    }

    public void setIdentificationFilePath(String identificationFilePath) {
        this.identificationFilePath = identificationFilePath;
    }

    public MultipartFile getBusinessLicenseFile() {
        return businessLicenseFile;
    }

    public void setBusinessLicenseFile(MultipartFile businessLicenseFile) {
        this.businessLicenseFile = businessLicenseFile;
    }

    public String getBusinessLicenseFilePath() {
        return businessLicenseFilePath;
    }

    public void setBusinessLicenseFilePath(String businessLicenseFilePath) {
        this.businessLicenseFilePath = businessLicenseFilePath;
    }

    public boolean isAgreeTerms() {
        return agreeTerms;
    }

    public void setAgreeTerms(boolean agreeTerms) {
        this.agreeTerms = agreeTerms;
    }

    public boolean isAgreeMarketing() {
        return agreeMarketing;
    }

    public void setAgreeMarketing(boolean agreeMarketing) {
        this.agreeMarketing = agreeMarketing;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
