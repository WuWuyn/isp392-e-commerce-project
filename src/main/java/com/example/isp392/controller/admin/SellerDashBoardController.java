package com.example.isp392.controller.admin;

import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Dashboard cho seller
 */
@Controller
@RequestMapping("/seller")
class SellerDashboardController {

    /**
     * Hiển thị dashboard seller nếu đã được duyệt
     */
    @GetMapping("/dashboard")
    public String showDashboard(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        if (currentUser == null || !currentUser.hasRole("SELLER")) {
            return "redirect:/seller/registration";
        }
        User user = currentUser.getUser();
        Shop shop = user.getShop();

        // Nếu shop chưa được duyệt thì chuyển về trang trạng thái đăng ký
        if (shop == null || !"APPROVED".equals(shop.getStatus())) {
            return "redirect:/seller/registration-status";
        }

        model.addAttribute("shop", shop);
        return "seller/dashboard";
    }

    /**
     * Hiển thị trạng thái đăng ký seller
     */
    @GetMapping("/registration-status")
    public String showRegistrationStatus(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        if (currentUser == null || !currentUser.hasRole("SELLER")) {
            return "redirect:seller/registration-status";
        }
        User user = currentUser.getUser();
        Shop shop = user.getShop();


        model.addAttribute("shop", shop);
        return "seller/registration-status";
    }
}