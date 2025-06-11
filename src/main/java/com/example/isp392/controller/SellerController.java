package com.example.isp392.controller;

import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.Order;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import com.example.isp392.service.OrderService;
import com.example.isp392.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/seller")
public class SellerController {

    private static final Logger log = LoggerFactory.getLogger(SellerController.class);
    private final UserService userService;
    private final OrderService orderService; // <<< THÊM OrderService

    // <<< CẬP NHẬT CONSTRUCTOR
    public SellerController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "seller/seller-login";
    }

    @GetMapping("/signup")
    public String showSignupPage(Model model) {
        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        return "seller/seller-signup";
    }


    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        return "seller/dashboard";
    }

//    @PostMapping("/signup")
//    public String registerSeller(
//            @Valid @ModelAttribute("userRegistrationDTO") UserRegistrationDTO userRegistrationDTO,
//            BindingResult bindingResult,
//            RedirectAttributes redirectAttributes) {
//        if (bindingResult.hasErrors()) {
//            return "seller/seller-signup";
//        }
//        try {
//            userService.registerNewUser(userRegistrationDTO, "SELLER");
//            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
//            return "redirect:/seller/login";
//        } catch (Exception e) {
//            log.error("Error registering seller: {}", e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            return "redirect:/seller/signup";
//        }
//    }


    @GetMapping("/account")
    public String showAccountInfo(Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        return "seller/account-info";
    }

    @GetMapping("/edit-info")
    public String showEditInfoPage(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }
        model.addAttribute("user", user);
        return "seller/account-edit-info";
    }


//    @PostMapping("/edit-info")
//    public String updateUserInfo(
//            @ModelAttribute("fullName") String fullName,
//            @ModelAttribute("phone") String phone,
//            @ModelAttribute("gender") int gender,
//            @ModelAttribute("dateOfBirth") String dateOfBirth,
//            @RequestParam(value = "profilePictureFile", required = false) MultipartFile profilePictureFile,
//            RedirectAttributes redirectAttributes,
//            HttpServletRequest request) {
//        try {
//            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//            String email = auth.getName();
//            boolean updated = userService.updateUserInfo(email, fullName, phone, gender, dateOfBirth, profilePictureFile, request);
//            if (updated) {
//                redirectAttributes.addFlashAttribute("successMessage", "Your information has been updated successfully.");
//            } else {
//                redirectAttributes.addFlashAttribute("errorMessage", "Failed to update information.");
//            }
//            return "redirect:/seller/edit-info";
//        } catch (Exception e) {
//            log.error("Error updating seller info: {}", e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            return "redirect:/seller/edit-info";
//        }
//    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isOAuth2User = auth instanceof OAuth2AuthenticationToken;
            model.addAttribute("isOAuth2User", isOAuth2User);
            String email;
            if (isOAuth2User) {
                OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
                email = oauth2User.getAttribute("email");
                log.debug("OAuth2 seller accessing change password page: {}", email);
            } else {
                email = auth.getName();
                log.debug("Regular seller accessing change password page: {}", email);
            }
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                return "seller/account-change-password";
            } else {
                return "redirect:/seller/login";
            }
        } catch (Exception e) {
            log.error("Error displaying change password form: {}", e.getMessage());
            return "redirect:/seller/login";
        }
    }

    @PostMapping("/update-password")
    public String updatePassword(
            @ModelAttribute("currentPassword") String currentPassword,
            @ModelAttribute("newPassword") String newPassword,
            @ModelAttribute("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isOAuth2User = auth instanceof OAuth2AuthenticationToken;
        if (isOAuth2User) {
            log.warn("Google OAuth2 seller attempted to change password");
            redirectAttributes.addFlashAttribute("errorMessage", "Google account users cannot change their password here. Please use your Google account settings.");
            return "redirect:/seller/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirmation do not match.");
            return "redirect:/seller/change-password";
        }
        try {
            String email = auth.getName();
            boolean updated = userService.updatePassword(email, currentPassword, newPassword);
            if (updated) {
                redirectAttributes.addFlashAttribute("successMessage", "Your password has been updated successfully.");
                return "redirect:/seller/change-password";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect.");
                return "redirect:/seller/change-password";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/change-password";
        }
    }

    @GetMapping("/orders")
    public String showOrdersPage(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/seller/login";
        }

        // Lấy danh sách đơn hàng thật từ database
        List<Order> orders = orderService.getOrdersForSeller(currentUser.getUserId());

        model.addAttribute("user", currentUser); // Dùng cho sidebar và topbar
        model.addAttribute("roles", userService.getUserRoles(currentUser)); // Dùng cho sidebar
        model.addAttribute("orders", orders); // Gửi danh sách đơn hàng sang view
        model.addAttribute("allStatuses", OrderStatus.values()); // Gửi tất cả các trạng thái sang view

        return "seller/orders";
    }
    @GetMapping("/orders/{id}")
    public String showOrderDetailPage(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/seller/login";
        }

        Optional<Order> orderOpt = orderService.findOrderById(id);
        if (orderOpt.isPresent()) {
            model.addAttribute("order", orderOpt.get());
            model.addAttribute("user", currentUser);
            return "seller/order-detail"; // Trả về trang chi tiết mới
        } else {
            // Xử lý trường hợp không tìm thấy đơn hàng
            return "redirect:/seller/orders";
        }
    }

    // <<< THÊM PHƯƠNG THỨC MỚI ĐỂ CẬP NHẬT TRẠNG THÁI
    @PostMapping("/orders/update-status/{orderId}")
    public String updateOrderStatus(@PathVariable("orderId") Integer orderId,
                                    @RequestParam("status") OrderStatus newStatus,
                                    RedirectAttributes redirectAttributes) {

        // Bạn có thể thêm logic kiểm tra xem người bán có quyền cập nhật đơn hàng này không

        boolean isUpdated = orderService.updateOrderStatus(orderId, newStatus);

        if (isUpdated) {
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update order status. Order not found.");
        }

        return "redirect:/seller/orders";
    }





    @GetMapping("/cart")
    public String showCart(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                return "seller/cart";
            } else {
                return "redirect:/seller/login";
            }
        } catch (Exception e) {
            log.error("Error displaying seller cart: {}", e.getMessage());
            return "redirect:/seller/login";
        }
    }

    // Helper methods for authentication
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getCurrentUser(auth);
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String email = null;
        if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
            email = oauth2User.getAttribute("email");
        } else {
            email = auth.getName();
        }
        if (email == null) return null;
        Optional<User> userOpt = userService.findByEmail(email);
        return userOpt.orElse(null);
    }
}
