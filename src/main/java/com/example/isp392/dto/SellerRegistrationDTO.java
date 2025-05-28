package com.example.isp392.dto;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class SellerRegistrationDTO {

    // Thông tin cửa hàng
    @NotBlank(message = "Tên cửa hàng không được để trống")
    @Size(min = 2, max = 100, message = "Tên cửa hàng phải từ 2-100 ký tự")
    private String shopName;

    @NotBlank(message = "Vui lòng chọn danh mục kinh doanh")
    private String shopCategory;

    @NotBlank(message = "Mô tả cửa hàng không được để trống")
    @Size(min = 10, max = 1000, message = "Mô tả phải từ 10-1000 ký tự")
    private String shopDescription;

    private MultipartFile shopLogoFile;
    private String shopLogoPath;

    // Thông tin liên hệ
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 50, message = "Họ tên phải từ 2-50 ký tự")
    private String contactName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String contactPhone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String contactEmail;

    private String contactWebsite;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 10, max = 200, message = "Địa chỉ phải từ 10-200 ký tự")
    private String contactAddress;

    // Thông tin ngân hàng
    @NotBlank(message = "Vui lòng chọn ngân hàng")
    private String bankName;

    @NotBlank(message = "Số tài khoản không được để trống")
    @Pattern(regexp = "^[0-9]{6,20}$", message = "Số tài khoản không hợp lệ")
    private String bankAccountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(min = 2, max = 50, message = "Tên chủ tài khoản phải từ 2-50 ký tự")
    private String bankAccountName;

    private String bankBranch;

    // Thông tin giấy phép kinh doanh
    @NotBlank(message = "Vui lòng chọn loại hình kinh doanh")
    private String businessType;

    private String businessLicenseNumber;
    private MultipartFile businessLicenseFile;
    private String businessLicenseFilePath;
    private String taxCode;

    // Đồng ý điều khoản
    @AssertTrue(message = "Bạn phải đồng ý với các quy định")
    private boolean agreeTerms;

    private boolean agreeMarketing;

    // Thông tin hệ thống
    private LocalDateTime registrationDate;
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
    private String rejectionReason;

    // Constructors
    public SellerRegistrationDTO() {
        this.registrationDate = LocalDateTime.now();
    }

    // Getters và Setters
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

    public String getContactWebsite() {
        return contactWebsite;
    }

    public void setContactWebsite(String contactWebsite) {
        this.contactWebsite = contactWebsite;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessLicenseNumber() {
        return businessLicenseNumber;
    }

    public void setBusinessLicenseNumber(String businessLicenseNumber) {
        this.businessLicenseNumber = businessLicenseNumber;
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

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
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

    @Override
    public String toString() {
        return "SellerRegistrationDTO{" +
                "shopName='" + shopName + '\'' +
                ", shopCategory='" + shopCategory + '\'' +
                ", contactName='" + contactName + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", status='" + status + '\'' +
                ", registrationDate=" + registrationDate +
                '}';
    }
}
