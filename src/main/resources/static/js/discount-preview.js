/**
 * Discount Preview System
 * Provides real-time discount calculations and preview functionality
 */
class DiscountPreviewSystem {
    constructor() {
        this.currentPromotion = null;
        this.previewData = null;
        this.debounceTimer = null;
        this.isLoading = false;
        
        this.initializeElements();
        this.bindEvents();
    }

    initializeElements() {
        this.promoInput = document.getElementById('promoCode');
        this.applyBtn = document.getElementById('applyPromoBtn');
        this.previewContainer = document.getElementById('discountPreview');
        this.errorContainer = document.getElementById('promoError');
        this.successContainer = document.getElementById('promoSuccess');
        this.loadingIndicator = document.getElementById('promoLoading');
    }

    bindEvents() {
        if (this.promoInput) {
            // Real-time preview as user types (with debounce)
            this.promoInput.addEventListener('input', (e) => {
                this.debouncePreview(e.target.value.trim());
            });

            // Apply promotion on Enter key
            this.promoInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.applyPromotion();
                }
            });
        }

        if (this.applyBtn) {
            this.applyBtn.addEventListener('click', () => {
                this.applyPromotion();
            });
        }
    }

    /**
     * Debounced preview calculation
     */
    debouncePreview(promoCode) {
        clearTimeout(this.debounceTimer);
        
        if (!promoCode) {
            this.hidePreview();
            return;
        }

        this.debounceTimer = setTimeout(() => {
            this.calculatePreview(promoCode);
        }, 500); // 500ms debounce
    }

    /**
     * Calculate discount preview
     */
    async calculatePreview(promoCode) {
        if (this.isLoading) return;

        this.isLoading = true;
        this.showLoading();

        try {
            const orders = this.getSelectedOrders();
            if (orders.length === 0) {
                this.showError('Vui lòng chọn ít nhất một sản phẩm');
                return;
            }

            const response = await fetch('/api/promotion/preview', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...this.getCsrfHeaders()
                },
                body: JSON.stringify({
                    promoCode: promoCode,
                    orders: orders,
                    totalOrderValue: this.calculateTotalOrderValue(orders)
                })
            });

            const data = await response.json();

            if (data.success) {
                this.previewData = data;
                this.displayPreview(data);
                this.hideError();
            } else {
                this.hidePreview();
                this.showError(data.message || 'Mã giảm giá không hợp lệ');
            }
        } catch (error) {
            console.error('Error calculating preview:', error);
            this.hidePreview();
            this.showError('Không thể tính toán giảm giá. Vui lòng thử lại.');
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    /**
     * Apply promotion code
     */
    async applyPromotion() {
        const promoCode = this.promoInput.value.trim();
        
        if (!promoCode) {
            this.showError('Vui lòng nhập mã giảm giá');
            return;
        }

        if (!this.previewData || !this.previewData.success) {
            this.showError('Mã giảm giá không hợp lệ');
            return;
        }

        // Store promotion in session/localStorage for checkout
        this.storePromotionForCheckout(this.previewData);
        
        // Show success message
        this.showSuccess('Mã giảm giá đã được áp dụng!');
        
        // Update UI to show applied state
        this.showAppliedState();
    }

    /**
     * Display simplified discount preview for cart page
     */
    displayPreview(data) {
        if (!this.previewContainer) return;

        const previewHTML = `
            <div class="discount-preview-content simplified">
                <div class="preview-header">
                    <div class="savings-badge">
                        <i class="fas fa-tag"></i>
                        <span class="savings-amount">Tiết kiệm ${this.formatVND(data.totalSavings)} VND</span>
                    </div>
                    <div class="promotion-info">
                        <span class="promo-name">${data.promotionName}</span>
                        <span class="promo-code">(${data.promoCode})</span>
                    </div>
                </div>

                <div class="simple-summary">
                    <div class="summary-row">
                        <span>Mã giảm giá được áp dụng</span>
                        <span class="discount-amount">-${this.formatVND(data.totalDiscount)} VND</span>
                    </div>
                    <div class="checkout-note">
                        <small class="text-muted">
                            <i class="fas fa-info-circle"></i>
                            Chi tiết giảm giá sẽ được hiển thị trong trang thanh toán
                        </small>
                    </div>
                </div>

                <button class="btn-remove-promo" onclick="window.discountPreview.removePromotion()">
                    <i class="fas fa-times"></i> Bỏ mã giảm giá
                </button>
            </div>
        `;

        this.previewContainer.innerHTML = previewHTML;
        this.previewContainer.style.display = 'block';

        // Add animation
        this.previewContainer.classList.add('fade-in');
    }

    /**
     * Create HTML for individual order breakdown
     */
    createOrderBreakdownHTML(order) {
        return `
            <div class="order-item ${order.eligible ? 'eligible' : 'ineligible'}">
                <div class="order-header">
                    <span class="shop-name">
                        <i class="fas fa-store"></i>
                        ${order.shopName}
                    </span>
                    ${order.eligible ? 
                        `<span class="savings-badge">-${this.formatVND(order.savingsAmount)} VND</span>` :
                        `<span class="ineligible-badge">Không đủ điều kiện</span>`
                    }
                </div>
                
                ${order.eligible ? `
                    <div class="price-breakdown">
                        <div class="price-row">
                            <span class="original-price">${this.formatVND(order.originalTotal)} VND</span>
                            <span class="arrow">→</span>
                            <span class="discounted-price">${this.formatVND(order.discountedTotal)} VND</span>
                        </div>
                        <div class="discount-info">
                            <span class="discount-percentage">${order.discountPercentage} giảm</span>
                        </div>
                    </div>
                ` : `
                    <div class="ineligible-reason">
                        ${order.ineligibilityReason || 'Không đủ điều kiện áp dụng'}
                    </div>
                `}
            </div>
        `;
    }

    /**
     * Get selected orders from cart
     */
    getSelectedOrders() {
        // This should be implemented based on your cart structure
        // Return array of order objects with required fields
        const orders = [];
        
        // Example implementation - adapt to your cart structure
        const selectedItems = document.querySelectorAll('.cart-item input[type="checkbox"]:checked');
        
        // Group items by shop and create order objects
        const shopGroups = {};
        
        selectedItems.forEach(checkbox => {
            const cartItem = checkbox.closest('.cart-item');
            const shopName = cartItem.dataset.shopName || 'Default Shop';
            const subtotal = parseFloat(cartItem.dataset.subtotal || '0');
            const shippingFee = parseFloat(cartItem.dataset.shippingFee || '30000');
            
            if (!shopGroups[shopName]) {
                shopGroups[shopName] = {
                    orderId: Math.random(), // Temporary ID for preview
                    shopName: shopName,
                    subtotal: 0,
                    shippingFee: shippingFee,
                    items: []
                };
            }
            
            shopGroups[shopName].subtotal += subtotal;
        });
        
        return Object.values(shopGroups);
    }

    /**
     * Calculate total order value
     */
    calculateTotalOrderValue(orders) {
        return orders.reduce((total, order) => {
            return total + order.subtotal + (order.shippingFee || 0);
        }, 0);
    }

    /**
     * Format VND currency
     */
    formatVND(amount) {
        if (window.VNDFormatter) {
            return VNDFormatter.format(amount);
        }
        return new Intl.NumberFormat('en-US').format(amount);
    }

    /**
     * Store promotion data for checkout
     */
    storePromotionForCheckout(promotionData) {
        sessionStorage.setItem('appliedPromotion', JSON.stringify(promotionData));
    }

    /**
     * Show/hide methods
     */
    showLoading() {
        if (this.loadingIndicator) {
            this.loadingIndicator.style.display = 'block';
        }
    }

    hideLoading() {
        if (this.loadingIndicator) {
            this.loadingIndicator.style.display = 'none';
        }
    }

    showError(message) {
        if (this.errorContainer) {
            this.errorContainer.textContent = message;
            this.errorContainer.style.display = 'block';
        }
    }

    hideError() {
        if (this.errorContainer) {
            this.errorContainer.style.display = 'none';
        }
    }

    showSuccess(message) {
        if (this.successContainer) {
            this.successContainer.textContent = message;
            this.successContainer.style.display = 'block';
        }
        this.hideError();

        // Auto hide success message after 3 seconds
        setTimeout(() => {
            if (this.successContainer) {
                this.successContainer.style.display = 'none';
            }
        }, 3000);
    }

    hidePreview() {
        if (this.previewContainer) {
            this.previewContainer.style.display = 'none';
        }
    }

    showAppliedState() {
        // Update UI to show that promotion is applied
        if (this.applyBtn) {
            this.applyBtn.textContent = 'Đã áp dụng';
            this.applyBtn.disabled = true;
            this.applyBtn.classList.add('applied');
        }
    }

    /**
     * Remove applied promotion
     */
    removePromotion() {
        // Clear promotion data
        this.previewData = null;
        this.currentPromotion = null;

        // Clear input
        if (this.promoInput) {
            this.promoInput.value = '';
        }

        // Reset apply button
        if (this.applyBtn) {
            this.applyBtn.textContent = 'Áp dụng';
            this.applyBtn.disabled = false;
            this.applyBtn.classList.remove('applied');
        }

        // Hide preview
        this.hidePreview();
        this.hideError();

        // Clear from session storage
        sessionStorage.removeItem('appliedPromotion');

        // Show success message
        this.showSuccess('Đã bỏ mã giảm giá');

        // Update cart summary if needed
        if (typeof updateCartSummary === 'function') {
            updateCartSummary();
        }
    }

    /**
     * Get CSRF headers for requests
     */
    getCsrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        
        if (token && header) {
            return { [header]: token };
        }
        return {};
    }
}

// Initialize discount preview system when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.discountPreview = new DiscountPreviewSystem();
});
