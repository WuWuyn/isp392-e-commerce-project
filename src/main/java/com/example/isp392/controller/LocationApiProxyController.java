package com.example.isp392.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller that acts as a proxy for the Vietnamese Provinces API
 * to avoid CORS issues when calling from the browser.
 */
@RestController
@RequestMapping("/api/location")
public class LocationApiProxyController {
    
    private final String API_BASE_URL = "https://provinces.open-api.vn/api/";
    private final RestTemplate restTemplate;
    
    public LocationApiProxyController() {
        // Configure RestTemplate with increased timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(5000); // 5 seconds
        
        this.restTemplate = new RestTemplate(factory);
    }
    
    /**
     * Get all provinces
     */
    @GetMapping("/provinces")
    public ResponseEntity<Object> getProvinces(@RequestParam(required = false) Integer depth) {
        try {
            String url = API_BASE_URL;
            if (depth != null) {
                url += "?depth=" + depth;
            }
            return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
        } catch (RestClientException e) {
            return handleApiError("Failed to fetch provinces", e);
        }
    }
    
    /**
     * Get a specific province by code
     */
    @GetMapping("/provinces/{code}")
    public ResponseEntity<Object> getProvince(
            @PathVariable String code,
            @RequestParam(required = false) Integer depth) {
        
        try {
            String url = API_BASE_URL + "p/" + code;
            if (depth != null) {
                url += "?depth=" + depth;
            }
            
            return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
        } catch (RestClientException e) {
            return handleApiError("Failed to fetch province: " + code, e);
        }
    }
    
    /**
     * Get a specific district by code
     */
    @GetMapping("/districts/{code}")
    public ResponseEntity<Object> getDistrict(
            @PathVariable String code,
            @RequestParam(required = false) Integer depth) {
        
        try {
            String url = API_BASE_URL + "d/" + code;
            if (depth != null) {
                url += "?depth=" + depth;
            }
            
            return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
        } catch (RestClientException e) {
            return handleApiError("Failed to fetch district: " + code, e);
        }
    }
    
    /**
     * Handle API errors and return a meaningful error response
     */
    private ResponseEntity<Object> handleApiError(String message, Exception e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("message", e.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }
}
