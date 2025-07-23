/**
 * validator.js
 * Common form validation utilities for ReadHub application
 */

// Email validation
function isValidEmail(email) {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(email);
}

// Phone number validation (Vietnam format)
function isValidPhoneNumber(phone) {
    const phoneRegex = /^(\+84|84|0)[1-9][0-9]{8}$/;
    return phoneRegex.test(phone);
}

// ISBN validation (ISBN-10 or ISBN-13)
function isValidISBN(isbn) {
    // Remove hyphens and spaces
    isbn = isbn.replace(/[- ]/g, '');
    
    // Check length - must be either 10 or 13 digits
    if (isbn.length !== 10 && isbn.length !== 13) {
        return false;
    }
    
    // Validate ISBN-10
    if (isbn.length === 10) {
        // Check if first 9 characters are digits
        if (!/^\d{9}/.test(isbn)) {
            return false;
        }
        
        // Calculate checksum
        let sum = 0;
        for (let i = 0; i < 9; i++) {
            sum += parseInt(isbn.charAt(i)) * (10 - i);
        }
        
        // Check digit (last character) can be 'X' or a digit
        let checkDigit = isbn.charAt(9);
        if (checkDigit === 'X') {
            sum += 10;
        } else if (/^\d$/.test(checkDigit)) {
            sum += parseInt(checkDigit);
        } else {
            return false;
        }
        
        return sum % 11 === 0;
    }
    
    // Validate ISBN-13
    if (isbn.length === 13) {
        // All characters must be digits
        if (!/^\d{13}$/.test(isbn)) {
            return false;
        }
        
        // Calculate checksum
        let sum = 0;
        for (let i = 0; i < 12; i++) {
            sum += parseInt(isbn.charAt(i)) * (i % 2 === 0 ? 1 : 3);
        }
        
        // Check digit (last digit)
        const checkDigit = (10 - (sum % 10)) % 10;
        return parseInt(isbn.charAt(12)) === checkDigit;
    }
    
    return false;
}

// Price validation - checks if price is within 50%-150% of original price
function isValidSellingPrice(originalPrice, sellingPrice) {
    const minPrice = originalPrice * 0.5;
    const maxPrice = originalPrice * 1.5;
    return sellingPrice >= minPrice && sellingPrice <= maxPrice;
}

// Dimensions validation (format: WxHxD, all numbers)
function isValidDimensions(dimensions) {
    const dimensionsRegex = /^\d+(\.\d+)?x\d+(\.\d+)?x\d+(\.\d+)?$/;
    return dimensionsRegex.test(dimensions);
}

// Form validation - checks if all required fields are filled
function validateRequiredFields(formElement, fieldIds) {
    let isValid = true;
    
    fieldIds.forEach(fieldId => {
        const field = formElement.querySelector('#' + fieldId);
        if (!field || !field.value.trim()) {
            if (field) {
                field.classList.add('is-invalid');
            }
            isValid = false;
        }
    });
    
    return isValid;
}

// Add error messages to a container
function addErrorMessage(container, message) {
    if (!container) return;
    
    const errorElement = document.createElement('div');
    errorElement.className = 'invalid-feedback d-block';
    errorElement.textContent = message;
    container.appendChild(errorElement);
}

// Clear all error messages from a container
function clearErrorMessages(container) {
    if (!container) return;
    
    const errorMessages = container.querySelectorAll('.invalid-feedback');
    errorMessages.forEach(element => element.remove());
}

// Format number as currency (VND)
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'decimal',
        maximumFractionDigits: 0
    }).format(amount) + ' đ';
}

// Remove currency formatting
function parseCurrencyValue(formattedValue) {
    return parseFloat(formattedValue.replace(/[^\d]/g, ''));
} 