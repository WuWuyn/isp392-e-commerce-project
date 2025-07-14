package com.example.isp392.controller;

import com.example.isp392.model.Shop;
import com.example.isp392.model.TokenType;
import com.example.isp392.model.User;
import com.example.isp392.service.EmailService;
import com.example.isp392.service.OtpService;
import com.example.isp392.service.ShopService;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Optional;

@Controller
public class AccountDeletionController {

    private final UserService userService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final ShopService shopService;

    public AccountDeletionController(UserService userService, EmailService emailService, OtpService otpService, ShopService shopService) {
        this.userService = userService;
        this.emailService = emailService;
        this.otpService = otpService;
        this.shopService = shopService;
    }

    @GetMapping("/account-delete")
    public String showDeleteAccountPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User currentUser = userService.findByEmailDirectly(principal.getName());
        if (currentUser != null) {
            Optional<Shop> shopOptional = shopService.findShopByUserId(currentUser.getUserId());
            if (shopOptional.isPresent() && shopOptional.get().isActive()) {
                model.addAttribute("hasActiveShop", true);
                model.addAttribute("shopDeletionMessage", "You own an active shop. Please delete your shop first before deleting your account.");
            }
        }
        return "buyer/delete-account";
    }

    @PostMapping("/account-delete-request")
    public String requestAccountDeletion(@RequestParam("password") String password,
                                         Principal principal,
                                         RedirectAttributes redirectAttributes,
                                         HttpServletRequest request) {
        if (principal == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByEmailDirectly(principal.getName());
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/account-delete";
        }

        // Check if user has an active shop before proceeding with account deletion
        Optional<Shop> shopOptional = shopService.findShopByUserId(currentUser.getUserId());
        if (shopOptional.isPresent() && shopOptional.get().isActive()) {
            redirectAttributes.addFlashAttribute("error", "You own an active shop. Please delete your shop first before deleting your account.");
            return "redirect:/account-delete"; // Redirect back to account deletion page with error
        }

        if (!userService.checkIfValidOldPassword(currentUser, password)) {
            redirectAttributes.addFlashAttribute("error", "Invalid password. Please try again.");
            return "redirect:/account-delete";
        }


        // Generate confirmation token using OtpService
        String confirmationToken = otpService.generateToken(currentUser, TokenType.ACCOUNT_DELETION);

        String baseUrl = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString())
                .replacePath(null)
                .build().toUriString();
        String confirmationLink = baseUrl + "/account-delete-confirm?token=" + confirmationToken;

        emailService.sendAccountDeletionConfirmationEmail(currentUser.getEmail(), confirmationLink);

        redirectAttributes.addFlashAttribute("success", "A confirmation email has been sent to your email address. Please click the link in the email to complete the deletion.");
        return "redirect:/account-delete";
    }

    @GetMapping("/account-delete-confirm")
    public String confirmAccountDeletion(@RequestParam("token") String token,
                                         RedirectAttributes redirectAttributes,
                                         HttpServletRequest request) {

        Optional<User> userOpt = otpService.validateToken(token, TokenType.ACCOUNT_DELETION);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired account deletion link.");
            return "redirect:/login";
        }

        User userToSoftDelete = userOpt.get();

        // Perform soft delete
        userService.softDeleteUser(userToSoftDelete.getUserId());

        // Invalidate user session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear security context
        SecurityContextHolder.clearContext();

        emailService.sendAccountDeletionSuccessEmail(userToSoftDelete.getEmail());

        redirectAttributes.addFlashAttribute("success", "Your account has been successfully deleted.");
        return "redirect:/login";
    }
}
 