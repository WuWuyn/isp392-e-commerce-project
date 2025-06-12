package com.example.isp392.controller;

import com.example.isp392.dto.AddressDTO;
import com.example.isp392.model.User;
import com.example.isp392.repository.UserRepository;
import com.example.isp392.service.AddressService;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for handling address management
 * Manages user addresses with proper authentication
 * Supports both regular and OAuth2 users
 * Handles both buyer and seller addresses
 */
@Controller
public class AddressController {

    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    
    // Services and repositories used for business logic
    private final AddressService addressService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final HttpSession httpSession;

    /**
     * Constructor for dependency injection (Constructor Injection instead of @Autowired)
     * @param addressService service for address operations
     * @param userService service for user operations
     * @param userRepository repository for direct user database access
     * @param httpSession HTTP session for retrieving session attributes
     */
    public AddressController(AddressService addressService, UserService userService, UserRepository userRepository, HttpSession httpSession) {
        this.addressService = addressService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.httpSession = httpSession;
    }

    /**
     * Display all addresses for the logged-in buyer
     * @param model the Spring MVC model
     * @return the address list view
     */
    @GetMapping("/buyer/addresses")
    public String viewBuyerAddresses(Model model) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }

        // Get user's roles for sidebar
        List<String> roles = userService.getUserRoles(user);
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        
        // Get all addresses for the user
        List<AddressDTO> addresses = addressService.getAllAddressesByUser(user.getUserId());
        model.addAttribute("addresses", addresses);
        
        return "buyer/account-edit-address";
    }

    /**
     * Display all addresses for the logged-in seller
     * @param model the Spring MVC model
     * @return the address list view
     */
    @GetMapping("/seller/addresses")
    public String viewSellerAddresses(Model model) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        // Get user's roles for sidebar
        List<String> roles = userService.getUserRoles(user);
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        
        // Get all addresses for the user
        List<AddressDTO> addresses = addressService.getAllAddressesByUser(user.getUserId());
        model.addAttribute("addresses", addresses);
        
        return "seller/account-edit-address";
    }

    /**
     * Show form to add new address for buyer
     * @param model the Spring MVC model
     * @return the new address form view
     */
    @GetMapping("/buyer/addresses/new")
    public String showAddBuyerAddressForm(Model model) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }

        // Get user's roles for sidebar
        List<String> roles = userService.getUserRoles(user);
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        
        // Add empty DTO for form binding
        model.addAttribute("address", new AddressDTO());
        model.addAttribute("isEdit", false);
        
        return "buyer/address";
    }

    /**
     * Show form to add new address for seller
     * @param model the Spring MVC model
     * @return the new address form view
     */
    @GetMapping("/seller/addresses/new")
    public String showAddSellerAddressForm(Model model) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        // Get user's roles for sidebar
        List<String> roles = userService.getUserRoles(user);
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        
        // Add empty DTO for form binding
        model.addAttribute("address", new AddressDTO());
        model.addAttribute("isEdit", false);
        
        return "seller/address";
    }

    /**
     * Process the form to create a new address for buyer
     * @param addressDTO the address data from form
     * @param bindingResult validation results
     * @param redirectAttributes attributes for redirect
     * @param model the Spring MVC model
     * @return redirect to address list or form with errors
     */
    @PostMapping("/buyer/addresses/create")
    public String createBuyerAddress(
            @Valid @ModelAttribute("address") AddressDTO addressDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            // Add user and roles to model for sidebar
            List<String> roles = userService.getUserRoles(user);
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
            model.addAttribute("isEdit", false);
            return "buyer/address"; // Return to form with errors
        }

        try {
            // Create the address
            addressService.createAddress(addressDTO, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Address saved successfully!");
            return "redirect:/buyer/addresses?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/addresses/new?error";
        }
    }

    /**
     * Process the form to create a new address for seller
     * @param addressDTO the address data from form
     * @param bindingResult validation results
     * @param redirectAttributes attributes for redirect
     * @param model the Spring MVC model
     * @return redirect to address list or form with errors
     */
    @PostMapping("/seller/addresses/create")
    public String createSellerAddress(
            @Valid @ModelAttribute("address") AddressDTO addressDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            // Add user and roles to model for sidebar
            List<String> roles = userService.getUserRoles(user);
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
            model.addAttribute("isEdit", false);
            return "seller/address"; // Return to form with errors
        }

        try {
            // Create the address
            addressService.createAddress(addressDTO, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Address saved successfully!");
            return "redirect:/seller/addresses?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/addresses/new?error";
        }
    }

    /**
     * Show form to edit an existing address for buyer
     * @param addressId the address ID
     * @param model the Spring MVC model
     * @return the edit address form view
     */
    @GetMapping("/buyer/addresses/edit/{addressId}")
    public String showEditBuyerAddressForm(
            @PathVariable int addressId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }

        // Get user's roles for sidebar
        List<String> roles = userService.getUserRoles(user);
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        
        // Get the address for editing
        AddressDTO addressDTO = addressService.getAddressByIdAndUser(addressId, user.getUserId())
                .orElse(null);
                
        if (addressDTO == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Address not found");
            return "redirect:/buyer/addresses";
        }
        
        model.addAttribute("address", addressDTO);
        model.addAttribute("isEdit", true);
        
        return "buyer/address";
    }

    /**
     * Show form to edit an existing address for seller
     * @param addressId the address ID
     * @param model the Spring MVC model
     * @return the edit address form view
     */
    @GetMapping("/seller/addresses/edit/{addressId}")
    public String showEditSellerAddressForm(
            @PathVariable int addressId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        // Get user's roles for sidebar
        List<String> roles = userService.getUserRoles(user);
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        
        // Get the address for editing
        AddressDTO addressDTO = addressService.getAddressByIdAndUser(addressId, user.getUserId())
                .orElse(null);
                
        if (addressDTO == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Address not found");
            return "redirect:/seller/addresses";
        }
        
        model.addAttribute("address", addressDTO);
        model.addAttribute("isEdit", true);
        
        return "seller/address";
    }

    /**
     * Process the form to update an existing address for buyer
     * @param addressId the address ID
     * @param addressDTO the address data from form
     * @param bindingResult validation results
     * @param redirectAttributes attributes for redirect
     * @param model the Spring MVC model
     * @return redirect to address list or form with errors
     */
    @PostMapping("/buyer/addresses/update/{addressId}")
    public String updateBuyerAddress(
            @PathVariable int addressId,
            @Valid @ModelAttribute("address") AddressDTO addressDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            // Add user and roles to model for sidebar
            List<String> roles = userService.getUserRoles(user);
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
            model.addAttribute("isEdit", true); // Mark as edit mode
            return "buyer/address"; // Return to form with errors
        }

        try {
            // Update the address
            addressService.updateAddress(addressId, addressDTO, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Address updated successfully!");
            return "redirect:/buyer/addresses?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/addresses/edit/" + addressId + "?error";
        }
    }

    /**
     * Process the form to update an existing address for seller
     * @param addressId the address ID
     * @param addressDTO the address data from form
     * @param bindingResult validation results
     * @param redirectAttributes attributes for redirect
     * @param model the Spring MVC model
     * @return redirect to address list or form with errors
     */
    @PostMapping("/seller/addresses/update/{addressId}")
    public String updateSellerAddress(
            @PathVariable int addressId,
            @Valid @ModelAttribute("address") AddressDTO addressDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            // Add user and roles to model for sidebar
            List<String> roles = userService.getUserRoles(user);
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
            model.addAttribute("isEdit", true); // Mark as edit mode
            return "seller/address"; // Return to form with errors
        }

        try {
            // Update the address
            addressService.updateAddress(addressId, addressDTO, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Address updated successfully!");
            return "redirect:/seller/addresses?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/addresses/edit/" + addressId + "?error";
        }
    }

    /**
     * Delete an address for buyer
     * @param addressId the address ID to delete
     * @param redirectAttributes attributes for redirect
     * @return redirect to address list
     */
    @GetMapping("/buyer/addresses/delete/{addressId}")
    public String deleteBuyerAddress(
            @PathVariable int addressId,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }

        try {
            // Delete the address
            addressService.deleteAddress(addressId, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Address deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/buyer/addresses";
    }

    /**
     * Delete an address for seller
     * @param addressId the address ID to delete
     * @param redirectAttributes attributes for redirect
     * @return redirect to address list
     */
    @GetMapping("/seller/addresses/delete/{addressId}")
    public String deleteSellerAddress(
            @PathVariable int addressId,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        try {
            // Delete the address
            addressService.deleteAddress(addressId, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Address deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/seller/addresses";
    }

    /**
     * Set an address as default for buyer
     * @param addressId the address ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to address list
     */
    @GetMapping("/buyer/addresses/set-default/{addressId}")
    public String setDefaultBuyerAddress(
            @PathVariable int addressId,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }

        try {
            // Set as default
            addressService.setDefaultAddress(addressId, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Default address updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/buyer/addresses";
    }
    
    /**
     * Set an address as default for seller
     * @param addressId the address ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to address list
     */
    @GetMapping("/seller/addresses/set-default/{addressId}")
    public String setDefaultSellerAddress(
            @PathVariable int addressId,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        try {
            // Set as default
            addressService.setDefaultAddress(addressId, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Default address updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/seller/addresses";
    }
    
    /**
     * Helper method to get the current authenticated user, supporting both regular and OAuth2 users
     * @return User object or null if not authenticated or not found
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        
        // First try to get user ID from session (set by OAuth2LoginSuccessHandler)
        Integer userId = (Integer) httpSession.getAttribute("USER_ID");
        if (userId != null) {
            Optional<User> userById = userRepository.findById(userId);
            if (userById.isPresent()) {
                User user = userById.get();
                log.debug("Found user from session USER_ID: id={}, name={}", user.getUserId(), user.getFullName());
                return user;
            }
        }
        
        String email;
        
        // Check if authentication is from OAuth2 (Google)
        if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
            email = oauth2User.getAttribute("email");
            log.debug("Getting OAuth2 user with email: {}", email);
        } else {
            // Regular form login user
            email = auth.getName();
            log.debug("Getting regular user with email: {}", email);
        }
        
        if (email == null) {
            log.warn("Could not extract email from authentication: {}", auth.getPrincipal());
            return null;
        }
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            log.warn("No user found for email: {}", email);
            return null;
        }
        
        User user = userOptional.get();
        log.debug("Found user: id={}, name={}", user.getUserId(), user.getFullName());
        return user;
    }
}
