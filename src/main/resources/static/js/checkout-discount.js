/**
 * Checkout Discount System
 * Handles detailed discount breakdown and promotion management in checkout
 */
class CheckoutDiscountSystem {
    constructor() {
        this.orderData = [];
        this.currentBreakdown = null;
        this.isLoading = false;
        
        this.initializeElements();
        this.bindEvents();
        this.loadCurrentPromotion();
    }

    initializeElements() {
        this.orderItemsContainer = document.getElementById('orderItemsContainer');
        this.promoInput = document.getElementById('checkoutPromoCode');
        this.applyBtn = document.getElementById('applyCheckoutPromoBtn');
        this.removeBtn = document.getElementById('removePromoBtn');
        this.appliedPromotionDisplay = document.getElementById('appliedPromotionDisplay');
        this.promotionInput = document.getElementById('promotionInput');
        this.discountBreakdownSection = document.getElementById('discountBreakdownSection');
        this.discountBreakdownContent = document.getElementById('discountBreakdownContent');

        // Summary elements
        this.summarySubtotal = document.getElementById('summarySubtotal');
        this.summaryShipping = document.getElementById('summaryShipping');
        this.summaryDiscount = document.getElementById('summaryDiscount');
        this.summaryDiscountRow = document.getElementById('summaryDiscountRow');
        this.summaryTotal = document.getElementById('summaryTotal');

        // Error/success elements
        this.errorElement = document.getElementById('checkoutPromoError');
        this.successElement = document.getElementById('checkoutPromoSuccess');

        // Debug: Log which elements were found
        console.log('CheckoutDiscountSystem elements:', {
            promoInput: !!this.promoInput,
            applyBtn: !!this.applyBtn,
            removeBtn: !!this.removeBtn,
            appliedPromotionDisplay: !!this.appliedPromotionDisplay,
            promotionInput: !!this.promotionInput,
            discountBreakdownSection: !!this.discountBreakdownSection,
            discountBreakdownContent: !!this.discountBreakdownContent,
            summaryDiscount: !!this.summaryDiscount,
            summaryDiscountRow: !!this.summaryDiscountRow
        });
    }

    bindEvents() {
        if (this.applyBtn) {
            this.applyBtn.addEventListener('click', () => {
                this.applyPromotion();
            });
        }

        if (this.promoInput) {
            this.promoInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.applyPromotion();
                }
            });
        }

        if (this.removeBtn) {
            this.removeBtn.addEventListener('click', () => {
                this.removePromotion();
            });
        }
    }

    /**
     * Initialize checkout with order data
     */
    initializeWithData(orderData) {
        console.log('initializeWithData called with:', orderData);
        this.orderData = orderData;
        console.log('orderData set to:', this.orderData);
        this.renderOrderItems();
        this.updateSummary();
    }

    /**
     * Render order items grouped by shop
     */
    renderOrderItems() {
        if (!this.orderItemsContainer) return;

        let html = '';
        
        this.orderData.forEach(order => {
            html += this.createOrderSectionHTML(order);
        });

        this.orderItemsContainer.innerHTML = html;
    }

    /**
     * Create HTML for individual order section
     */
    createOrderSectionHTML(order) {
        const itemsHTML = order.items.map(item => `
            <div class="order-item">
                <div class="item-image">
                    <i class="fas fa-book"></i>
                </div>
                <div class="item-details">
                    <div class="item-title">${item.bookTitle}</div>
                    <div class="item-quantity">Số lượng: ${item.quantity}</div>
                </div>
                <div class="item-price">
                    <div class="item-unit-price">${this.formatVND(item.unitPrice)} VND</div>
                    <div class="item-total-price">${this.formatVND(item.totalPrice)} VND</div>
                </div>
            </div>
        `).join('');

        return `
            <div class="order-shop-section">
                <div class="shop-header">
                    <div class="shop-icon">
                        <i class="fas fa-store"></i>
                    </div>
                    <div class="shop-name">${order.shopName}</div>
                    <div class="shop-shipping">
                        <i class="fas fa-truck"></i> ${this.formatVND(order.shippingFee)} VND
                    </div>
                </div>
                <div class="order-items">
                    ${itemsHTML}
                </div>
                <div class="order-totals">
                    <div class="total-row">
                        <span>Tạm tính:</span>
                        <span>${this.formatVND(order.subtotal)} VND</span>
                    </div>
                    <div class="total-row">
                        <span>Phí vận chuyển:</span>
                        <span>${this.formatVND(order.shippingFee)} VND</span>
                    </div>
                    <div class="total-row">
                        <span>Tổng cộng:</span>
                        <span>${this.formatVND(order.subtotal + order.shippingFee)} VND</span>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Apply promotion code
     */
    async applyPromotion() {
        console.log('applyPromotion called');

        if (!this.promoInput) {
            console.error('Promo input element not found');
            return;
        }

        const promoCode = this.promoInput.value.trim();
        console.log('Applying promotion code:', promoCode);

        if (!promoCode) {
            this.showError('Vui lòng nhập mã giảm giá');
            return;
        }

        if (this.isLoading) return;

        this.isLoading = true;
        this.showLoading();

        try {
            const requestData = {
                promoCode: promoCode,
                orders: this.orderData,
                totalOrderValue: this.calculateTotalOrderValue()
            };
            console.log('Sending request to /api/checkout/promotion/apply:', requestData);

            const response = await fetch('/api/checkout/promotion/apply', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...this.getCsrfHeaders()
                },
                body: JSON.stringify(requestData)
            });

            console.log('Response status:', response.status);
            console.log('Response ok:', response.ok);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('Response data:', data);
            console.log('Data success field:', data.success);
            console.log('Data success type:', typeof data.success);

            if (data.success === true) {
                console.log('Promotion applied successfully, breakdown:', data.breakdown);
                this.currentBreakdown = data.breakdown;
                this.displayAppliedPromotion(data.breakdown);
                this.displayDiscountBreakdown(data.breakdown);
                this.updateSummary();
                this.showSuccess('Mã giảm giá đã được áp dụng thành công!');
                this.hidePromotionInput();
            } else {
                console.log('Promotion application failed:', data.message);
                console.log('Success field value:', data.success);
                this.showError(data.message || 'Không thể áp dụng mã giảm giá');
            }
        } catch (error) {
            console.error('Error applying promotion:', error);
            this.showError('Có lỗi xảy ra khi áp dụng mã giảm giá');
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    /**
     * Remove applied promotion
     */
    async removePromotion() {
        if (this.isLoading) return;

        this.isLoading = true;

        try {
            const response = await fetch('/api/checkout/promotion/remove', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...this.getCsrfHeaders()
                }
            });

            const data = await response.json();

            if (data.success) {
                console.log('Promotion removed successfully, resetting state');
                console.log('Before reset - currentBreakdown:', this.currentBreakdown);
                console.log('Before reset - orderData:', this.orderData);

                this.currentBreakdown = null;
                this.hideAppliedPromotion();
                this.hideDiscountBreakdown();
                this.updateSummary();
                this.showPromotionInput();
                this.showSuccess('Đã bỏ mã giảm giá');
                this.promoInput.value = '';

                console.log('After reset - currentBreakdown:', this.currentBreakdown);
            } else {
                console.log('Promotion removal failed:', data.message);
                this.showError(data.message || 'Không thể bỏ mã giảm giá');
            }
        } catch (error) {
            console.error('Error removing promotion:', error);
            this.showError('Có lỗi xảy ra khi bỏ mã giảm giá');
        } finally {
            this.isLoading = false;
        }
    }

    /**
     * Load current applied promotion
     */
    async loadCurrentPromotion() {
        try {
            const response = await fetch('/api/checkout/promotion/current', {
                method: 'GET',
                headers: this.getCsrfHeaders()
            });

            const data = await response.json();

            if (data.success && data.hasPromotion) {
                this.currentBreakdown = data.breakdown;
                this.displayAppliedPromotion(data.breakdown);
                this.displayDiscountBreakdown(data.breakdown);
                this.hidePromotionInput();
            } else {
                this.showPromotionInput();
            }
        } catch (error) {
            console.error('Error loading current promotion:', error);
            this.showPromotionInput();
        }
    }

    /**
     * Display applied promotion
     */
    displayAppliedPromotion(breakdown) {
        console.log('displayAppliedPromotion called with breakdown:', breakdown);
        console.log('appliedPromotionDisplay exists:', !!this.appliedPromotionDisplay);

        if (!this.appliedPromotionDisplay) {
            console.error('appliedPromotionDisplay element not found!');
            return;
        }

        // Update promotion code display (matches template ID)
        const promoCodeElement = document.getElementById('appliedPromoCode');
        if (promoCodeElement) {
            promoCodeElement.textContent = breakdown.promoCode;
            console.log('Promo code displayed:', breakdown.promoCode);
        } else {
            console.error('appliedPromoCode element not found!');
        }

        // Update promotion description (optional)
        const promoDescElement = document.getElementById('appliedPromoDesc');
        if (promoDescElement) {
            promoDescElement.textContent = `Tiết kiệm: ${this.formatVND(breakdown.totalDiscount)} VND`;
            console.log('Promo description displayed');
        }

        this.appliedPromotionDisplay.style.display = 'block';
        this.appliedPromotionDisplay.classList.add('fade-in');
        console.log('Applied promotion display shown');
    }

    /**
     * Hide applied promotion display
     */
    hideAppliedPromotion() {
        if (this.appliedPromotionDisplay) {
            this.appliedPromotionDisplay.style.display = 'none';
        }
    }

    /**
     * Show promotion input
     */
    showPromotionInput() {
        if (this.promotionInput) {
            this.promotionInput.style.display = 'block';
        }
    }

    /**
     * Hide promotion input
     */
    hidePromotionInput() {
        if (this.promotionInput) {
            this.promotionInput.style.display = 'none';
        }
    }

    /**
     * Display detailed discount breakdown
     */
    displayDiscountBreakdown(breakdown) {
        console.log('displayDiscountBreakdown called with breakdown:', breakdown);
        console.log('discountBreakdownContent exists:', !!this.discountBreakdownContent);
        console.log('breakdown.orders:', breakdown.orders);

        if (!this.discountBreakdownContent) {
            console.error('discountBreakdownContent element not found!');
            return;
        }

        let html = `
            <h6><i class="fas fa-calculator me-2"></i>Discount Breakdown</h6>
        `;

        breakdown.orders.forEach(order => {
            html += `
                <div class="order-breakdown">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <strong><i class="fas fa-store me-1"></i> ${order.shopName}</strong>
                        <span class="badge bg-success">-${this.formatVND(order.savingsAmount)} VND</span>
                    </div>
                    <div class="row text-sm">
                        <div class="col-6">
                            <div>Original: ${this.formatVND(order.originalTotal)} VND</div>
                            <div>Discount: -${this.formatVND(order.savingsAmount)} VND</div>
                        </div>
                        <div class="col-6 text-end">
                            <div>Final: ${this.formatVND(order.discountedTotal)} VND</div>
                            <div class="text-success fw-bold">Saved: ${this.formatVND(order.savingsAmount)} VND</div>
                        </div>
                    </div>
                </div>
            `;
        });

        html += `
            <div class="mt-3 p-2 bg-success text-white rounded text-center">
                <strong>Total Savings: ${this.formatVND(breakdown.totalSavings)} VND</strong>
            </div>
        `;

        this.discountBreakdownContent.innerHTML = html;
        this.discountBreakdownSection.style.display = 'block';
        this.discountBreakdownSection.classList.add('fade-in');

        // Update individual shop summaries
        this.updateShopSummaries(breakdown);
    }

    /**
     * Update individual shop summaries with discount information
     */
    updateShopSummaries(breakdown) {
        const shopGroups = document.querySelectorAll('.shop-group');

        shopGroups.forEach((shopGroup, index) => {
            const shopNameElement = shopGroup.querySelector('.text-primary.fw-bold');
            if (!shopNameElement) return;

            const shopName = shopNameElement.textContent.trim();
            const orderBreakdown = breakdown.orders.find(order => order.shopName === shopName);

            if (orderBreakdown && orderBreakdown.savingsAmount > 0) {
                // Show discount section
                const discountSection = shopGroup.querySelector('.shop-discount-section');
                const discountAmount = shopGroup.querySelector('.shop-discount-amount');
                const originalTotal = shopGroup.querySelector('.shop-original-total');
                const discountedTotal = shopGroup.querySelector('.shop-discounted-total');
                const shopTotal = shopGroup.querySelector('.shop-total');

                if (discountSection && discountAmount && originalTotal && discountedTotal && shopTotal) {
                    // Show discount section
                    discountSection.style.display = 'block';

                    // Update discount amount
                    discountAmount.textContent = `-${this.formatVND(orderBreakdown.savingsAmount)} VND`;

                    // Update original total (subtotal + shipping)
                    originalTotal.textContent = `${this.formatVND(orderBreakdown.originalTotal)} VND`;

                    // Update discounted total
                    discountedTotal.textContent = `${this.formatVND(orderBreakdown.discountedTotal)} VND`;

                    // Update shop total (final amount)
                    shopTotal.textContent = `${this.formatVND(orderBreakdown.discountedTotal)} VND`;

                    console.log(`Updated shop ${shopName}: Original ${orderBreakdown.originalTotal}, Discount ${orderBreakdown.savingsAmount}, Final ${orderBreakdown.discountedTotal}`);
                }
            }
        });
    }

    /**
     * Hide discount breakdown
     */
    hideDiscountBreakdown() {
        if (this.discountBreakdownSection) {
            this.discountBreakdownSection.style.display = 'none';
        }

        // Hide shop-level discount sections and reset shop totals
        const discountSections = document.querySelectorAll('.shop-discount-section');
        discountSections.forEach(section => {
            section.style.display = 'none';
        });

        // Reset shop totals to original amounts (subtotal + shipping)
        const shopGroups = document.querySelectorAll('.shop-group');
        shopGroups.forEach(shopGroup => {
            const shopSubtotalElement = shopGroup.querySelector('.shop-subtotal');
            const shopTotalElement = shopGroup.querySelector('.shop-total');

            if (shopSubtotalElement && shopTotalElement) {
                // Extract subtotal value
                const subtotalText = shopSubtotalElement.textContent.replace(/[^\d]/g, '');
                const subtotal = parseFloat(subtotalText) || 0;
                const originalTotal = subtotal + 30000; // Add shipping fee

                // Reset to original total
                shopTotalElement.textContent = this.formatVND(originalTotal) + ' VND';
            }
        });

        // Clear promotion code from summary
        const promoCodeSummary = document.getElementById('appliedPromoCodeSummary');
        if (promoCodeSummary) {
            promoCodeSummary.textContent = '';
        }
    }

    /**
     * Update payment summary
     */
    updateSummary() {
        console.log('updateSummary called');
        console.log('orderData:', this.orderData);
        console.log('currentBreakdown:', this.currentBreakdown);

        if (!this.orderData || !Array.isArray(this.orderData)) {
            console.error('orderData is not available or not an array:', this.orderData);
            return;
        }

        const totalSubtotal = this.orderData.reduce((sum, order) => sum + order.subtotal, 0);
        const totalShipping = this.orderData.reduce((sum, order) => sum + order.shippingFee, 0);
        const totalDiscount = this.currentBreakdown ? this.currentBreakdown.totalDiscount : 0;
        const finalTotal = totalSubtotal + totalShipping - totalDiscount;

        console.log('Calculated totals:', {
            totalSubtotal,
            totalShipping,
            totalDiscount,
            finalTotal
        });

        if (this.summarySubtotal) {
            this.summarySubtotal.textContent = this.formatVND(totalSubtotal) + ' VND';
        }
        if (this.summaryShipping) {
            this.summaryShipping.textContent = this.formatVND(totalShipping) + ' VND';
        }
        if (this.summaryDiscount) {
            this.summaryDiscount.textContent = '-' + this.formatVND(totalDiscount) + ' VND';

            // Show promotion code in summary
            const promoCodeSummary = document.getElementById('appliedPromoCodeSummary');
            if (promoCodeSummary && this.currentBreakdown && this.currentBreakdown.promoCode) {
                promoCodeSummary.textContent = `(${this.currentBreakdown.promoCode})`;
            }
        }
        // Only update total if server hasn't already calculated it correctly
        if (this.summaryTotal && !window.serverCalculatedTotal) {
            this.summaryTotal.textContent = this.formatVND(finalTotal) + ' VND';
            console.log('Updated total via CheckoutDiscountSystem:', finalTotal);
        } else if (window.serverCalculatedTotal) {
            console.log('Preserving server-calculated total:', window.serverCalculatedTotal);
        }

        // Show/hide discount row
        if (this.summaryDiscountRow) {
            this.summaryDiscountRow.style.display = totalDiscount > 0 ? 'flex' : 'none';
        }
    }

    /**
     * Calculate total order value
     */
    calculateTotalOrderValue() {
        return this.orderData.reduce((total, order) => {
            return total + order.subtotal + order.shippingFee;
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
     * Show/hide methods
     */
    showLoading() {
        if (this.applyBtn) {
            this.applyBtn.innerHTML = '<span class="loading-spinner"></span>Đang xử lý...';
            this.applyBtn.disabled = true;
        }
    }

    hideLoading() {
        if (this.applyBtn) {
            this.applyBtn.innerHTML = 'Áp dụng';
            this.applyBtn.disabled = false;
        }
    }

    showError(message) {
        console.log('showError called with message:', message);
        console.log('errorElement exists:', !!this.errorElement);

        if (this.errorElement) {
            this.errorElement.textContent = message;
            this.errorElement.style.display = 'block';
            console.log('Error message displayed:', message);
        } else {
            console.error('Error element not found!');
        }

        if (this.successElement) {
            this.successElement.style.display = 'none';
        }
    }

    showSuccess(message) {
        console.log('showSuccess called with message:', message);
        console.log('successElement exists:', !!this.successElement);

        if (this.successElement) {
            this.successElement.textContent = message;
            this.successElement.style.display = 'block';
            console.log('Success message displayed:', message);
        } else {
            console.error('Success element not found!');
        }

        if (this.errorElement) {
            this.errorElement.style.display = 'none';
        }

        // Auto hide success message after 3 seconds
        setTimeout(() => {
            if (this.successElement) {
                this.successElement.style.display = 'none';
            }
        }, 3000);
    }

    /**
     * Get CSRF headers for requests
     */
    getCsrfHeaders() {
        const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="csrf-header"]')?.getAttribute('content');
        
        if (token && header) {
            return { [header]: token };
        }
        return {};
    }
}

// Initialize checkout discount system when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.checkoutDiscount = new CheckoutDiscountSystem();
});
