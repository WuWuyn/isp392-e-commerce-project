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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Controller for wallet-related operations following MVC pattern
 */
@Controller
@RequestMapping("/buyer/wallet")
@RequiredArgsConstructor
@Slf4j
public class UserWalletController {

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

        try {
            if (authentication instanceof OAuth2AuthenticationToken) {
                // OAuth2 authentication (Google login)
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                String email = oauth2User.getAttribute("email");
                return userService.findByEmail(email).orElse(null);
            } else {
                // Local authentication
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String email = userDetails.getUsername();
                return userService.findByEmail(email).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage(), e);
            return null;
        }
    }



    /**
     * Display wallet transaction history
     */
    @GetMapping("/transactions")
    public String showTransactionHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "type", required = false) String transactionType,
            Model model) {

        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }

        try {
            // Ensure user has a wallet
            userService.ensureUserHasWallet(user);

            Pageable pageable = PageRequest.of(page, size);
            Page<WalletTransaction> transactionPage;

            if (transactionType != null && !transactionType.isEmpty()) {
                WalletTransactionType type = WalletTransactionType.valueOf(transactionType.toUpperCase());
                transactionPage = walletService.getTransactionHistoryByType(user, type, pageable);
            } else {
                transactionPage = walletService.getTransactionHistory(user, pageable);
            }

            // Convert to DTOs
            Page<WalletTransactionDTO> transactionDTOPage = transactionPage.map(this::convertToDTO);

            BigDecimal currentBalance = walletService.getBalance(user);
            WalletTransactionHistoryDTO historyDTO = new WalletTransactionHistoryDTO(currentBalance, transactionDTOPage);

            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            model.addAttribute("transactionHistory", historyDTO);
            model.addAttribute("selectedType", transactionType);
            model.addAttribute("transactionTypes", WalletTransactionType.values());

            return "buyer/wallet-transactions";
        } catch (Exception e) {
            log.error("Error displaying transaction history for user {}: {}", user.getEmail(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Unable to load transaction history. Please try again.");
            return "buyer/wallet-transactions";
        }
    }

    /**
     * Display wallet overview page (main wallet page)
     */
    @GetMapping("")
    public String showWalletOverview(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
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

            return "buyer/wallet-overview";
        } catch (Exception e) {
            log.error("Error displaying wallet overview for user {}: {}", user.getEmail(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Unable to load wallet information. Please try again.");
            return "buyer/wallet-overview";
        }
    }

    /**
     * Convert WalletTransaction entity to DTO
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
     * Show withdrawal form
     */
    @GetMapping("/withdraw")
    public String showWithdrawalForm(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/buyer/login";
        }

        try {
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/buyer/login";
            }

            // Ensure user has a wallet
            userService.ensureUserHasWallet(user);

            BigDecimal balance = walletService.getBalance(user);
            WalletBalanceDTO walletBalanceDTO = new WalletBalanceDTO(balance, true);

            model.addAttribute("walletBalance", walletBalanceDTO);
            model.addAttribute("withdrawalRequest", new WithdrawalRequestDTO());

            return "buyer/wallet-withdraw";
        } catch (Exception e) {
            log.error("Error showing withdrawal form for user: {}", authentication.getName(), e);
            model.addAttribute("errorMessage", "Unable to load withdrawal form. Please try again.");
            return "buyer/wallet-overview";
        }
    }

    /**
     * Process withdrawal request
     */
    @PostMapping("/withdraw")
    public String processWithdrawal(@Valid @ModelAttribute("withdrawalRequest") WithdrawalRequestDTO withdrawalRequest,
                                    BindingResult bindingResult,
                                    Model model,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/buyer/login";
        }

        try {
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/buyer/login";
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
                return "buyer/wallet-withdraw";
            }

            // Process the withdrawal
            WalletTransaction transaction = walletService.processWithdrawal(user, withdrawalRequest);

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Withdrawal request of %s has been processed successfully. Transaction ID: %d",
                            withdrawalRequest.getFormattedAmount(), transaction.getTransactionId()));

            log.info("Withdrawal processed successfully for user: {} - Amount: {}",
                    user.getEmail(), withdrawalRequest.getFormattedAmount());

            return "redirect:/buyer/wallet";

        } catch (RuntimeException e) {
            log.error("Error processing withdrawal for user: {} - {}", authentication.getName(), e.getMessage());

            // Add error message and return to form
            try {
                User user = getCurrentUser();
                if (user != null) {
                    BigDecimal balance = walletService.getBalance(user);
                    WalletBalanceDTO walletBalanceDTO = new WalletBalanceDTO(balance, true);
                    model.addAttribute("walletBalance", walletBalanceDTO);
                }
            } catch (Exception balanceException) {
                log.error("Error getting balance for error display: {}", balanceException.getMessage());
            }
            model.addAttribute("errorMessage", e.getMessage());

            return "buyer/wallet-withdraw";
        } catch (Exception e) {
            log.error("Unexpected error processing withdrawal for user: {}", authentication.getName(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An unexpected error occurred while processing your withdrawal. Please try again.");
            return "redirect:/buyer/wallet";
        }
    }
}
