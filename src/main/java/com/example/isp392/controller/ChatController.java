package com.example.isp392.controller;

import com.example.isp392.model.ChatMessage;
import com.example.isp392.model.User;
import com.example.isp392.service.ChatService;
import com.example.isp392.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for chat functionality
 * Simple API design focusing on reliability and ease of use
 */
@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final UserService userService;

    @Autowired
    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    /**
     * Send a message to the chatbot
     * Accessible to all users (authenticated and anonymous)
     */
    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String message = payload.get("message");
            String sessionToken = payload.get("sessionToken");
            
            // Validate input
            if (message == null || message.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Message cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate message length
            if (message.length() > 2000) {
                response.put("success", false);
                response.put("message", "Message too long. Please keep it under 2000 characters.");
                return ResponseEntity.badRequest().body(response);
            }

            // Generate session token if not provided
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                sessionToken = chatService.generateSessionToken();
            }

            // Get authenticated user (required)
            User user = getAuthenticatedUser();
            if (user == null) {
                response.put("success", false);
                response.put("message", "Bạn cần đăng nhập để sử dụng chatbot");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Process the message
            String botResponse = chatService.processMessage(message.trim(), sessionToken, user);

            // Build response
            response.put("success", true);
            response.put("message", botResponse);
            response.put("sessionToken", sessionToken);
            response.put("timestamp", LocalDateTime.now().toString());

            logger.debug("Chat message processed successfully. Session: {}", sessionToken);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while processing your message");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get chat history for a session
     */
    @GetMapping("/api/chat/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChatHistory(@RequestParam String sessionToken) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Session token is required");
                return ResponseEntity.badRequest().body(response);
            }

            List<ChatMessage> history = chatService.getChatHistory(sessionToken);
            
            response.put("success", true);
            response.put("messages", history);
            response.put("totalMessages", history.size());
            
            logger.debug("Chat history retrieved for session: {}", sessionToken);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving chat history: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while retrieving chat history");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Generate a new session token
     */
    @PostMapping("/api/chat/session")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSession() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sessionToken = chatService.generateSessionToken();
            
            response.put("success", true);
            response.put("sessionToken", sessionToken);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating session: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while creating session");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/api/chat/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "ReadHub Chat Service");
        return ResponseEntity.ok(response);
    }



    /**
     * Get authenticated user (returns null if not authenticated)
     * Supports both form login and OAuth2
     */
    private User getAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            String email = null;

            // Handle OAuth2 authentication
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                email = oauth2User.getAttribute("email");
            }
            // Handle form-based authentication
            else if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                email = userDetails.getUsername();
            }
            // Handle other authentication types
            else if (authentication.getPrincipal() instanceof String) {
                email = (String) authentication.getPrincipal();
            }

            if (email != null) {
                return userService.findByEmail(email).orElse(null);
            }

        } catch (Exception e) {
            logger.debug("Could not get authenticated user: {}", e.getMessage());
        }
        
        return null;
    }
}
