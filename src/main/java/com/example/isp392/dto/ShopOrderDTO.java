package com.example.isp392.dto;

import lombok.Data;
import java.util.List;

@Data
public class ShopOrderDTO {
    private Integer shopId;
    private List<Integer> cartItemIds;
    private String shippingMethod;
    private String shopNotes;
    
    /**
     * Get the shop ID
     * @return the shop ID
     */
    public Integer getShopId() {
        return shopId;
    }
    
    /**
     * Get the cart item IDs
     * @return the cart item IDs
     */
    public List<Integer> getCartItemIds() {
        return cartItemIds;
    }
    
    /**
     * Get the shipping method
     * @return the shipping method
     */
    public String getShippingMethod() {
        return shippingMethod;
    }
    
    /**
     * Get the shop notes
     * @return the shop notes
     */
    public String getShopNotes() {
        return shopNotes;
    }
} 