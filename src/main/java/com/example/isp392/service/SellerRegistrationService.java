package com.example.isp392.service;

import com.example.isp392.dto.SellerRegistrationDTO;
import com.example.isp392.model.SellerRegistration;
import com.example.isp392.repository.SellerRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SellerRegistrationService {

    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final EmailService emailService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Autowired
    public SellerRegistrationService(SellerRegistrationRepository sellerRegistrationRepository,
                                     EmailService emailService) {
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.emailService = emailService;
    }

    public SellerRegistration registerSeller(SellerRegistrationDTO dto) throws Exception {
        if (sellerRegistrationRepository.existsByShopNameIgnoreCase(dto.getShopName())) {
            throw new IllegalArgumentException("Tên cửa hàng đã tồn tại!");
        }
        if (sellerRegistrationRepository.existsByContactEmailIgnoreCase(dto.getContactEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        SellerRegistration registration = new SellerRegistration();
        mapDtoToEntity(dto, registration);
        processFileUpload(dto.getShopLogoFile(), "logos", registration::setShopLogoPath);
        processFileUpload(dto.getBusinessLicenseFile(), "licenses", registration::setBusinessLicenseFilePath);
        registration.setRegistrationDate(LocalDateTime.now());
        registration.setStatus("PENDING");
        SellerRegistration saved = sellerRegistrationRepository.save(registration);
        try { sendConfirmationEmail(saved); } catch (Exception ignored) {}
        return saved;
    }

    public List<SellerRegistration> getAllRegistrations() {
        return sellerRegistrationRepository.findAllByOrderByRegistrationDateDesc();
    }

    public SellerRegistration getRegistrationById(Long id) {
        return sellerRegistrationRepository.findById(id).orElse(null);
    }

    public void approveRegistration(Long id, String approvedBy) {
        SellerRegistration reg = getRegistrationById(id);
        if (reg != null) {
            reg.setStatus("APPROVED");
            reg.setApprovedDate(LocalDateTime.now());
            reg.setApprovedBy(approvedBy);
            sellerRegistrationRepository.save(reg);
            sendApprovalEmail(reg);
        }
    }

    public void rejectRegistration(Long id, String reason) {
        SellerRegistration reg = getRegistrationById(id);
        if (reg != null) {
            reg.setStatus("REJECTED");
            reg.setRejectionReason(reason);
            sellerRegistrationRepository.save(reg);
            sendRejectionEmail(reg);
        }
    }

    private void mapDtoToEntity(SellerRegistrationDTO dto, SellerRegistration entity) {
        entity.setShopName(dto.getShopName());
        entity.setShopCategory(dto.getShopCategory());
        entity.setShopDescription(dto.getShopDescription());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setContactEmail(dto.getContactEmail());
        entity.setContactWebsite(dto.getContactWebsite());
        entity.setContactAddress(dto.getContactAddress());
        entity.setBankName(dto.getBankName());
        entity.setBankAccountNumber(dto.getBankAccountNumber());
        entity.setBankAccountName(dto.getBankAccountName());
        entity.setBankBranch(dto.getBankBranch());
        entity.setBusinessType(dto.getBusinessType());
        entity.setBusinessLicenseNumber(dto.getBusinessLicenseNumber());
        entity.setTaxCode(dto.getTaxCode());
        entity.setAgreeTerms(dto.isAgreeTerms());
        entity.setAgreeMarketing(dto.isAgreeMarketing());
    }

    private void processFileUpload(MultipartFile file, String folder, FileSetter setter) throws Exception {
        if (file != null && !file.isEmpty()) {
            try {
                String path = saveFile(file, folder);
                setter.set(path);
            } catch (IOException e) {
                throw new Exception("Lỗi upload file: " + e.getMessage(), e);
            }
        }
    }

    private String saveFile(MultipartFile file, String subfolder) throws IOException {
        Path dir = Paths.get(uploadDir, subfolder);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        String filename = UUID.randomUUID() + ext;
        Path dst = dir.resolve(filename);
        Files.copy(file.getInputStream(), dst);
        return subfolder + "/" + filename;
    }

    private void sendConfirmationEmail(SellerRegistration r) {
        String subject = "Xác nhận đăng ký Seller - " + r.getShopName();
        String content = buildEmailContent("confirmation", r);
        emailService.sendEmail(r.getContactEmail(), subject, content);
    }

    private void sendApprovalEmail(SellerRegistration r) {
        String subject = "Phê duyệt đăng ký Seller - " + r.getShopName();
        String content = buildEmailContent("approval", r);
        emailService.sendEmail(r.getContactEmail(), subject, content);
    }

    private void sendRejectionEmail(SellerRegistration r) {
        String subject = "Từ chối đăng ký Seller - " + r.getShopName();
        String content = buildEmailContent("rejection", r);
        emailService.sendEmail(r.getContactEmail(), subject, content);
    }

    private String buildEmailContent(String type, SellerRegistration r) {
        switch (type) {
            case "approval": return String.format("Xin chúc mừng %s! Hồ sơ của bạn đã được phê duyệt.", r.getContactName());
            case "rejection": return String.format("Xin lỗi %s! Đăng ký bị từ chối. Lý do: %s", r.getContactName(), r.getRejectionReason());
            default: return String.format("Xin chào %s! Chúng tôi đã nhận hồ sơ của bạn.", r.getContactName());
        }
    }

    @FunctionalInterface
    private interface FileSetter { void set(String path); }
}
