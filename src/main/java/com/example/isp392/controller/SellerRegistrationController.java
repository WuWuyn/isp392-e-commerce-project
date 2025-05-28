package com.example.isp392.controller;

import com.example.isp392.dto.SellerRegistrationDTO;
import com.example.isp392.service.SellerRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/seller")
public class SellerRegistrationController {

    @Autowired
    private SellerRegistrationService sellerRegistrationService;

    /**
     * Hiển thị form đăng ký seller
     */
    @GetMapping("/registration")
    public String showRegistrationForm(Model model) {
        model.addAttribute("sellerRegistration", new SellerRegistrationDTO());
        return "seller/seller-registration";
    }

    /**
     * Xử lý đăng ký seller
     */
    @PostMapping("/registration")
    public String processRegistration(
            @Valid @ModelAttribute("sellerRegistration") SellerRegistrationDTO sellerRegistrationDTO,
            BindingResult bindingResult,
            @RequestParam(value = "shopLogoFile", required = false) MultipartFile shopLogoFile,
            @RequestParam(value = "businessLicenseFile", required = false) MultipartFile businessLicenseFile,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin đã nhập!");
            return "seller/seller-registration";
        }

        try {
            // Xử lý file upload
            if (shopLogoFile != null && !shopLogoFile.isEmpty()) {
                sellerRegistrationDTO.setShopLogoFile(shopLogoFile);
            }

            if (businessLicenseFile != null && !businessLicenseFile.isEmpty()) {
                sellerRegistrationDTO.setBusinessLicenseFile(businessLicenseFile);
            }

            // Lưu thông tin đăng ký seller
            sellerRegistrationService.registerSeller(sellerRegistrationDTO);

            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký thành công! Chúng tôi sẽ xem xét và phản hồi trong vòng 24-48 giờ.");

            return "redirect:/seller/registration-success";

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "seller/seller-registration";
        }
    }

    /**
     * Trang thông báo đăng ký thành công
     */
    @GetMapping("/registration-success")
    public String registrationSuccess() {
        return "seller/registration-success";
    }

    /**
     * API kiểm tra tên cửa hàng đã tồn tại
     */



    /**
     * API lấy danh sách ngân hàng
     */


}
