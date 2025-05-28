package com.example.isp392.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Controller that acts as a proxy for the Vietnamese Provinces API
 * to avoid CORS issues when calling from the browser.
 */
@RestController
@RequestMapping("/api/location")
public class LocationApiProxyController {
    
    private final String API_BASE_URL = "https://provinces.open-api.vn/api/";
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Get all provinces
     */
    @GetMapping("/provinces")
    public ResponseEntity<Object> getProvinces(@RequestParam(required = false) Integer depth) {
        String url = API_BASE_URL;
        if (depth != null) {
            url += "?depth=" + depth;
        }
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }
    
    /**
     * Get a specific province by code
     */
    @GetMapping("/provinces/{code}")
    public ResponseEntity<Object> getProvince(
            @PathVariable String code,
            @RequestParam(required = false) Integer depth) {
        
        String url = API_BASE_URL + "p/" + code;
        if (depth != null) {
            url += "?depth=" + depth;
        }
        
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }
    
    /**
     * Get a specific district by code
     */
    @GetMapping("/districts/{code}")
    public ResponseEntity<Object> getDistrict(
            @PathVariable String code,
            @RequestParam(required = false) Integer depth) {
        
        String url = API_BASE_URL + "d/" + code;
        if (depth != null) {
            url += "?depth=" + depth;
        }
        
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }
}
