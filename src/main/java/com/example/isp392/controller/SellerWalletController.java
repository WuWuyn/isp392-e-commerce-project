package com.example.isp392.controller;

import com.example.isp392.dto.WalletBalanceDTO;
import com.example.isp392.dto.WalletTransactionDTO;
import com.example.isp392.dto.WalletTransactionHistoryDTO;
import com.example.isp392.dto.WithdrawalRequestDTO;
import com.example.isp392.model.User;
import com.example.isp392.model.Wallet;
import com.example.isp392.model.WalletTransaction;
import com.example.isp392.model.enums.WalletTransactionType;
import com.example.isp392.service.UserService;
import com.example.isp392.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller for seller wallet-related operations
 */
@Controller
@RequestMapping("/seller/wallet")
@RequiredArgsConstructor
@Slf4j
public class SellerWalletController {

    private final WalletService walletService;
    private final UserService userService;

    /**
     * Get current authenticated user (supports both local and OAuth2 authentication)
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = null;
        if (authentication.getPrincipal() instanceof UserDetails) {
            email = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = oauth2User.getAttribute("email");
        }

        if (email != null) {
            return userService.findByEmail(email).orElse(null);
        }
        return null;
    }

    /**
     * Convert WalletTransaction to DTO for display
     */
    private WalletTransactionDTO convertToDTO(WalletTransaction transaction) {
        WalletTransactionDTO dto = new WalletTransactionDTO();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setWalletId(transaction.getWallet().getWalletId());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setAmount(transaction.getAmount());
        dto.setBalanceBefore(transaction.getBalanceBefore());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setDescription(transaction.getDescription());
        dto.setReferenceType(transaction.getReferenceType());
        dto.setReferenceId(transaction.getReferenceId());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setCreatedBy(transaction.getCreatedBy());
        dto.setStatus(transaction.getStatus());

        // Set created by name if available
        if (transaction.getCreatedBy() != null) {
            try {
                Optional<User> createdByUser = userService.findById(transaction.getCreatedBy());
                dto.setCreatedByName(createdByUser.map(User::getFullName).orElse("System"));
            } catch (Exception e) {
                dto.setCreatedByName("System");
            }
        } else {
            dto.setCreatedByName("System");
        }

        return dto;
    }

    /**
     * Display transaction history with pagination and filtering
     */
    @GetMapping("/transactions")
    public String showTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) WalletTransactionType type,
            Model model) {

        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        try {
            // Ensure user has a wallet
            userService.ensureUserHasWallet(user);

            // Create pageable without sorting (repository method already has OrderBy)
            Pageable pageable = PageRequest.of(page, size);

            // Get transaction history with optional type filtering
            Page<WalletTransaction> transactionPage;
            if (type != null) {
                transactionPage = walletService.getTransactionHistoryByType(user, type, pageable);
            } else {
                transactionPage = walletService.getTransactionHistory(user, pageable);
            }

            // Convert to DTOs
            Page<WalletTransactionDTO> transactionDTOs = transactionPage.map(this::convertToDTO);

            // Get current balance
            BigDecimal balance = walletService.getBalance(user);
            WalletTransactionHistoryDTO historyDTO = new WalletTransactionHistoryDTO(balance, transactionDTOs);

            model.addAttribute("user", user);
            model.addAttribute("transactionHistory", historyDTO);
            model.addAttribute("selectedType", type);
            model.addAttribute("transactionTypes", WalletTransactionType.values());
            model.addAttribute("activeMenu", "wallet");

            return "seller/wallet-transactions";
        } catch (Exception e) {
            log.error("Error displaying transaction history for seller {}: {}", user.getEmail(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Unable to load transaction history. Please try again.");
            return "seller/wallet-transactions";
        }
    }

    /**
     * Display wallet overview page (main wallet page)
     */
    @GetMapping("")
    public String showWalletOverview(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        try {
            // Ensure user has a wallet
            userService.ensureUserHasWallet(user);

            BigDecimal balance = walletService.getBalance(user);
            Optional<Wallet> walletOpt = walletService.getWalletByUser(user);

            // Get recent transactions (last 5)
            Pageable recentTransactionsPageable = PageRequest.of(0, 5);
            Page<WalletTransaction> recentTransactions = walletService.getTransactionHistory(user, recentTransactionsPageable);
            Page<WalletTransactionDTO> recentTransactionDTOs = recentTransactions.map(this::convertToDTO);

            WalletBalanceDTO walletBalanceDTO = new WalletBalanceDTO(balance, walletOpt.isPresent());

            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            model.addAttribute("walletBalance", walletBalanceDTO);
            model.addAttribute("recentTransactions", recentTransactionDTOs);
            model.addAttribute("activeMenu", "wallet");

            return "seller/wallet-overview";
        } catch (Exception e) {
            log.error("Error displaying wallet overview for seller {}: {}", user.getEmail(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Unable to load wallet information. Please try again.");
            return "seller/wallet-overview";
        }
    }

    /**
     * Show withdrawal form
     */
    @GetMapping("/withdraw")
    public String showWithdrawForm(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/seller/login";
        }

        try {
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }

            // Ensure user has a wallet
            userService.ensureUserHasWallet(user);

            BigDecimal balance = walletService.getBalance(user);
            WalletBalanceDTO walletBalanceDTO = new WalletBalanceDTO(balance, true);

            model.addAttribute("walletBalance", walletBalanceDTO);
            model.addAttribute("withdrawalRequest", new WithdrawalRequestDTO());
            model.addAttribute("activeMenu", "wallet");

            return "seller/wallet-withdraw";
        } catch (Exception e) {
            log.error("Error showing withdrawal form for seller: {}", authentication.getName(), e);
            model.addAttribute("errorMessage", "Unable to load withdrawal form. Please try again.");
            return "seller/wallet-overview";
        }
    }

    /**
     * Process withdrawal request
     */
    @PostMapping("/withdraw")
    public String processWithdrawal(
            @Valid @ModelAttribute WithdrawalRequestDTO withdrawalRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/seller/login";
        }

        try {
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }

            // Get current balance for validation
            BigDecimal currentBalance = walletService.getBalance(user);

            // Validate amount against current balance
            if (withdrawalRequest.getAmount() != null && withdrawalRequest.getAmount().compareTo(currentBalance) > 0) {
                bindingResult.rejectValue("amount", "insufficient.balance",
                        "Insufficient balance. Available: " + String.format("%,d VND", currentBalance.longValue()));
            }

            // If validation errors, return to form
            if (bindingResult.hasErrors()) {
                WalletBalanceDTO walletBalanceDTO = new WalletBalanceDTO(currentBalance, true);
                model.addAttribute("walletBalance", walletBalanceDTO);
                model.addAttribute("activeMenu", "wallet");
                return "seller/wallet-withdraw";
            }

            // Process the withdrawal
            WalletTransaction transaction = walletService.processWithdrawal(user, withdrawalRequest);

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Withdrawal request of %s has been processed successfully. Transaction ID: %d",
                            withdrawalRequest.getFormattedAmount(), transaction.getTransactionId()));

            log.info("Withdrawal processed successfully for seller: {} - Amount: {}",
                    user.getEmail(), withdrawalRequest.getFormattedAmount());

            return "redirect:/seller/wallet";
        } catch (Exception e) {
            log.error("Error processing withdrawal for seller: {}", authentication.getName(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to process withdrawal. Please try again.");
            return "redirect:/seller/wallet/withdraw";
        }
    }
}
