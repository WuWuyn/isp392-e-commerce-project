package com.example.isp392.dto;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutDTO {
    private String recipientName;
    private String recipientPhone;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private String shippingAddressDetail;
    private String shippingCompany;
    private Integer shippingAddressType;
    private String paymentMethod;
    private String notes;
    private List<ShopOrderDTO> shopOrders;
    
    /**
     * Get the recipient name
     * @return the recipient name
     */
    public String getRecipientName() {
        return recipientName;
    }
    
    /**
     * Get the recipient phone
     * @return the recipient phone
     */
    public String getRecipientPhone() {
        return recipientPhone;
    }
    
    /**
     * Get the shipping province
     * @return the shipping province
     */
    public String getShippingProvince() {
        return shippingProvince;
    }
    
    /**
     * Get the shipping district
     * @return the shipping district
     */
    public String getShippingDistrict() {
        return shippingDistrict;
    }
    
    /**
     * Get the shipping ward
     * @return the shipping ward
     */
    public String getShippingWard() {
        return shippingWard;
    }
    
    /**
     * Get the shipping address detail
     * @return the shipping address detail
     */
    public String getShippingAddressDetail() {
        return shippingAddressDetail;
    }
    
    /**
     * Get the shipping company
     * @return the shipping company
     */
    public String getShippingCompany() {
        return shippingCompany;
    }
    
    /**
     * Get the shipping address type
     * @return the shipping address type
     */
    public Integer getShippingAddressType() {
        return shippingAddressType;
    }
    
    /**
     * Get the payment method
     * @return the payment method
     */
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    /**
     * Get the shop orders
     * @return the shop orders
     */
    public List<ShopOrderDTO> getShopOrders() {
        return shopOrders;
    }
} 