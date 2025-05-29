package com.example.isp392.controller.admin;

import com.example.isp392.model.Shop;
import com.example.isp392.repository.ShopRepository;
import com.example.isp392.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý seller cho admin
 */
@Controller
@RequestMapping("/admin/sellers")
public class AdminSellerController {

    private final ShopRepository shopRepository;

    public AdminSellerController(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    /**
     * Hiển thị danh sách seller theo trạng thái
     */
    @GetMapping
    public String listSellers(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        // Chỉ cho phép admin truy cập
        if (currentUser == null || !currentUser.hasRole("ADMIN")) {
            return "redirect:/error/403";
        }

        List<Shop> pendingSellers = shopRepository.findByStatus("PENDING");
        List<Shop> approvedSellers = shopRepository.findByStatus("APPROVED");
        List<Shop> rejectedSellers = shopRepository.findByStatus("REJECTED");

        model.addAttribute("pendingSellers", pendingSellers);
        model.addAttribute("approvedSellers", approvedSellers);
        model.addAttribute("rejectedSellers", rejectedSellers);

        return "admin/seller-management";
    }

    /**
     * Duyệt seller
     */
    @PostMapping("/{shopId}/approve")
    public String approveSeller(@PathVariable int shopId, @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null || !currentUser.hasRole("ADMIN")) {
            return "redirect:/error/403";
        }

        Shop shop = shopRepository.findById(shopId).orElseThrow(() ->
                new RuntimeException("Shop not found with id: " + shopId));

        shop.setStatus("APPROVED");
        shopRepository.save(shop);

        return "redirect:/admin/sellers";
    }

    /**
     * Từ chối seller
     */
    @PostMapping("/{shopId}/reject")
    public String rejectSeller(@PathVariable int shopId,
                               @RequestParam String rejectionReason,
                               @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null || !currentUser.hasRole("ADMIN")) {
            return "redirect:/error/403";
        }

        Shop shop = shopRepository.findById(shopId).orElseThrow(() ->
                new RuntimeException("Shop not found with id: " + shopId));

        shop.setStatus("REJECTED");
        shop.setRejectionReason(rejectionReason);
        shopRepository.save(shop);

        return "redirect:/admin/sellers";
    }

    /**
     * Xem chi tiết seller
     */
    @GetMapping("/{shopId}/view")
    public String viewSellerDetails(@PathVariable int shopId,
                                    @AuthenticationPrincipal CurrentUser currentUser,
                                    Model model) {
        if (currentUser == null || !currentUser.hasRole("ADMIN")) {
            return "redirect:/error/403";
        }

        Shop shop = shopRepository.findById(shopId).orElseThrow(() ->
                new RuntimeException("Shop not found with id: " + shopId));

        model.addAttribute("shop", shop);
        return "admin/seller-details";
    }

}