package com.example.isp392.controller;

import com.example.isp392.dto.AddressDTO;
import com.example.isp392.model.User;
import com.example.isp392.service.AddressService;
import com.example.isp392.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Controller for handling address management
 * Manages user addresses with proper authentication
 */
@Controller
@RequestMapping("/buyer")
public class AddressController {

    // Services used for business logic
    private final AddressService addressService;
    private final UserService userService;

    /**
     * Constructor for dependency injection (Constructor Injection instead of @Autowired)
     * @param addressService service for address operations
     * @param userService service for user operations
     */
    public AddressController(AddressService addressService, UserService userService) {
        this.addressService = addressService;
        this.userService = userService;
    }

    /**
     * Display all addresses for the logged-in user
     * @param model the Spring MVC model
     * @return the address list view
     */
    @GetMapping("/addresses")
    public String viewAddresses(Model model) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "redirect:/buyer/login";
        }
        
        User user = userOptional.get();

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
     * Show form to add new address
     * @param model the Spring MVC model
     * @return the new address form view
     */
    @GetMapping("/addresses/new")
    public String showAddAddressForm(Model model) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "redirect:/buyer/login";
        }
        
        User user = userOptional.get();

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
     * Process the form to create a new address
     * @param addressDTO the address data from form
     * @param bindingResult validation results
     * @param redirectAttributes attributes for redirect
     * @param model the Spring MVC model
     * @return redirect to address list or form with errors
     */
    @PostMapping("/addresses/create")
    public String createAddress(
            @Valid @ModelAttribute("address") AddressDTO addressDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "redirect:/buyer/login";
        }
        
        User user = userOptional.get();
        
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
     * Show form to edit an existing address
     * @param addressId the address ID
     * @param model the Spring MVC model
     * @return the edit address form view
     */
    @GetMapping("/addresses/edit/{addressId}")
    public String showEditAddressForm(
            @PathVariable int addressId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "redirect:/buyer/login";
        }
        
        User user = userOptional.get();

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
     * Process the form to update an existing address
     * @param addressId the address ID
     * @param addressDTO the address data from form
     * @param bindingResult validation results
     * @param redirectAttributes attributes for redirect
     * @param model the Spring MVC model
     * @return redirect to address list or form with errors
     */
    @PostMapping("/addresses/update/{addressId}")
    public String updateAddress(
            @PathVariable int addressId,
            @Valid @ModelAttribute("address") AddressDTO addressDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "redirect:/buyer/login";
        }
        
        User user = userOptional.get();

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
     * Delete an address
     * @param addressId the address ID to delete
     * @param redirectAttributes attributes for redirect
     * @return redirect to address list
     */
    @GetMapping("/addresses/delete/{addressId}")
    public String deleteAddress(
            @PathVariable int addressId,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "redirect:/buyer/login";
        }
        
        User user = userOptional.get();

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
     * Set an address as default
     * @param addressId the address ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to address list
     */
    @GetMapping("/addresses/set-default/{addressId}")
    public String setDefaultAddress(
            @PathVariable int addressId,
            RedirectAttributes redirectAttributes
    ) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "redirect:/buyer/login";
        }
        
        User user = userOptional.get();

        try {
            // Set as default
            addressService.setDefaultAddress(addressId, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Default address updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/buyer/addresses";
    }
}
