package com.example.isp392.controller;

import com.example.isp392.model.User;
import com.example.isp392.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.example.isp392.model.Blog;
import com.example.isp392.model.Book;
import com.example.isp392.model.Category;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.service.BlogService;
import com.example.isp392.service.BookService;
import org.springframework.data.domain.Page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Optional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for homepage-related operations
 */
@Controller
public class HomepageController {
    
    private static final Logger log = LoggerFactory.getLogger(HomepageController.class);
    
    private final UserService userService;
    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final BlogService blogService;

    /**
     * Constructor with explicit dependency injection
     */
    public HomepageController(BookService bookService, CategoryRepository categoryRepository, 
                             BlogService blogService, UserService userService) {
        this.bookService = bookService;
        this.categoryRepository = categoryRepository;
        this.blogService = blogService;
        this.userService = userService;
    }

    /**
     * Home page mapping
     * @param model the model to add attributes to
     * @return the home page view
     */
    @GetMapping("/")
    public String index(Model model) {
        // Load user information if authenticated
        loadAuthenticatedUserInfo(model);
        
        // Get top rated books (People's Choice)
        List<Book> topRatedBooks = bookService.getTopRatedBooks(5);
        model.addAttribute("topRatedBooks", topRatedBooks);

        // Get new additions
        List<Book> newAdditions = bookService.getNewAdditions(5);
        model.addAttribute("newAdditions", newAdditions);

        // Get discounted books
        List<Book> discountedBooks = bookService.getDiscountedBooks(5);
        model.addAttribute("discountedBooks", discountedBooks);

        // Get active categories
        List<Category> categories = categoryRepository.findByIsActiveTrue();
        model.addAttribute("categories", categories);
        
        // Get recent blog posts
        Page<Blog> recentBlogs = blogService.getBlogsSorted("latest", 0, 4);
        model.addAttribute("recentBlogs", recentBlogs.getContent());

        return "home";
    }
    
    /**
     * Helper method to load authenticated user information into the model
     * Works for both local and OAuth2 users
     * 
     * @param model the model to add user attributes to
     */
    private void loadAuthenticatedUserInfo(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
            
            // Get email based on authentication type
            String email;
            boolean isOAuth2User = false;
            
            // Check if authentication is from OAuth2 (Google)
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
                email = oauth2User.getAttribute("email");
                isOAuth2User = true;
                
                // Debug information
                log.info("OAuth2 user detected: {}", email);
            } else {
                // Regular form login user
                email = authentication.getName();
                log.info("Form login user detected: {}", email);
            }
            
            // Find user by email in the database
            Optional<User> userOptional = userService.findByEmail(email);
            
            // Add user and roles to model if found
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                model.addAttribute("user", user);
                model.addAttribute("userRoles", userService.getUserRoles(user));
                model.addAttribute("isOAuth2User", isOAuth2User);
                
                // Debug user information
                log.info("User loaded: id={}, name={}", 
                        user.getUserId(), user.getFullName());
            } else {
                log.warn("No user found for email: {}", email);
            }
        }
    }

    /**
     * Alternative home page mapping
     * @param model the model to add attributes to
     * @return redirect to home page
     */
    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String homepage(Model model) {
        // Redirect to index to avoid duplicate code
        return "redirect:/";
    }

    /**
     * About/Contact page mapping
     * @return the about-contact page view
     */
    @RequestMapping(value = "/about-contact", method = RequestMethod.GET)
    public String aboutContact() {
        return "about-contact";
    }

    /**
     * Terms and policy page mapping
     * @return the terms-policy page view
     */
    @RequestMapping(value = "/terms-policy", method = RequestMethod.GET)
    public String termsPolicy() {
        return "terms-policy";
    }
}
