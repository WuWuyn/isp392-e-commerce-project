package com.example.isp392.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for cart items to be serialized in PaymentReservation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Integer bookId;
    private Integer shopId;
    private Integer quantity;
    private String bookTitle;
    private String shopName;
    
    /**
     * Create CartItemDTO from CartItem entity
     */
    public static CartItemDTO fromCartItem(com.example.isp392.model.CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setBookId(cartItem.getBook().getBookId());
        dto.setShopId(cartItem.getShop().getShopId());
        dto.setQuantity(cartItem.getQuantity());
        dto.setBookTitle(cartItem.getBook().getTitle());
        dto.setShopName(cartItem.getShop().getShopName());
        return dto;
    }
}
