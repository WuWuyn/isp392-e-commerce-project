package com.example.isp392.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_registrations")
public class SellerRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thông tin cửa hàng
    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(name = "shop_category", nullable = false, length = 50)
    private String shopCategory;

    @Column(name = "shop_description", nullable = false, length = 1000)
    private String shopDescription;

    @Column(name = "shop_logo_path", length = 255)
    private String shopLogoPath;

    // Thông tin liên hệ
    @Column(name = "contact_name", nullable = false, length = 50)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 15)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_website", length = 255)
    private String contactWebsite;

    @Column(name = "contact_address", nullable = false, length = 200)
    private String contactAddress;

    // Thông tin ngân hàng
    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "bank_account_number", nullable = false, length = 20)
    private String bankAccountNumber;

    @Column(name = "bank_account_name", nullable = false, length = 50)
    private String bankAccountName;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    // Thông tin giấy phép kinh doanh
    @Column(name = "business_type", nullable = false, length = 20)
    private String businessType;

    @Column(name = "business_license_number", length = 50)
    private String businessLicenseNumber;

    @Column(name = "business_license_file_path", length = 255)
    private String businessLicenseFilePath;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    // Đồng ý điều khoản
    @Column(name = "agree_terms", nullable = false)
    private boolean agreeTerms;

    @Column(name = "agree_marketing")
    private boolean agreeMarketing;

    // Thông tin hệ thống
    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    // Constructors
    public SellerRegistration() {
        this.registrationDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters và Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getContactWebsite() {
        return contactWebsite;
    }

    public void setContactWebsite(String contactWebsite) {
        this.contactWebsite = contactWebsite;
    }

    public String getContactAddress() {
        return contactAddress;
    }


    public void setBusinessLicenseNumber(String businessLicenseNumber) {
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setAgreeTerms(boolean agreeTerms) {
        this.agreeTerms = agreeTerms;
    }

    public boolean isAgreeTerms() {
        return agreeTerms;
    }

    public void setAgreeMarketing(boolean agreeMarketing) {
        this.agreeMarketing = agreeMarketing;
    }

    public boolean isAgreeMarketing() {
        return agreeMarketing;
    }

    public Object getRejectionReason() {
        return rejectionReason;
    }


    public void setBusinessLicenseFilePath(String s) {
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
