package com.example.isp392.model;

/**
 * Enum representing common cancellation reasons for orders
 */
public enum CancellationReason {
    WRONG_PRODUCT("Đặt sai sản phẩm"),
    WANT_TO_APPLY_DISCOUNT("Muốn áp dụng mã giảm giá"),
    NO_LONGER_NEEDED("Không còn nhu cầu"),
    WRONG_ADDRESS("Sai địa chỉ giao hàng"),
    FOUND_BETTER_PRICE("Tìm được giá tốt hơn"),
    CHANGE_OF_MIND("Thay đổi ý định"),
    DUPLICATE_ORDER("Đặt trùng đơn hàng"),
    OTHER("Khác");

    private final String displayName;

    CancellationReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get CancellationReason from display name
     */
    public static CancellationReason fromDisplayName(String displayName) {
        for (CancellationReason reason : values()) {
            if (reason.getDisplayName().equals(displayName)) {
                return reason;
            }
        }
        return OTHER; // Default to OTHER if not found
    }

    /**
     * Check if this reason requires additional details
     */
    public boolean requiresAdditionalDetails() {
        return this == OTHER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
