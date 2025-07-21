/**
 * VND Currency Formatter Utility
 * Provides functions to format and parse Vietnamese Dong currency values
 */

/**
 * Format a number to VND currency format with comma separators
 * @param {number|string} value - The value to format
 * @returns {string} Formatted currency string (e.g., "1,000,000")
 */
function formatVND(value) {
    if (value === null || value === undefined || value === '') {
        return '';
    }
    
    // Convert to number if string
    const numValue = typeof value === 'string' ? parseFloat(value.replace(/,/g, '')) : value;
    
    if (isNaN(numValue)) {
        return '';
    }
    
    // Format with comma separators
    return numValue.toLocaleString('en-US');
}

/**
 * Parse VND formatted string to number
 * @param {string} formattedValue - The formatted string (e.g., "1,000,000")
 * @returns {number} Parsed number value
 */
function parseVND(formattedValue) {
    if (!formattedValue || typeof formattedValue !== 'string') {
        return null; // Return null instead of 0 for empty values
    }

    // Remove all commas and parse
    const cleanValue = formattedValue.replace(/,/g, '').trim();

    if (cleanValue === '') {
        return null; // Return null for empty strings
    }

    const numValue = parseFloat(cleanValue);

    return isNaN(numValue) ? null : numValue;
}

/**
 * Format input field value as user types
 * @param {HTMLInputElement} input - The input element
 */
function formatVNDInput(input) {
    const cursorPosition = input.selectionStart;
    const oldValue = input.value;
    const oldLength = oldValue.length;
    
    // Parse and reformat
    const numValue = parseVND(oldValue);
    const newValue = formatVND(numValue);
    
    // Update input value
    input.value = newValue;
    
    // Restore cursor position (accounting for added/removed commas)
    const newLength = newValue.length;
    const lengthDiff = newLength - oldLength;
    const newCursorPosition = Math.max(0, cursorPosition + lengthDiff);
    
    // Set cursor position after a short delay to ensure the value is updated
    setTimeout(() => {
        input.setSelectionRange(newCursorPosition, newCursorPosition);
    }, 0);
}

/**
 * Initialize VND formatting for input fields
 * @param {string} selector - CSS selector for input fields
 */
function initVNDFormatting(selector) {
    document.querySelectorAll(selector).forEach(input => {
        // Format on input
        input.addEventListener('input', function() {
            formatVNDInput(this);
        });
        
        // Format on blur
        input.addEventListener('blur', function() {
            formatVNDInput(this);
        });
        
        // Format initial value if exists
        if (input.value) {
            formatVNDInput(input);
        }
    });
}

/**
 * Validate VND input
 * @param {string} value - The value to validate
 * @param {number} min - Minimum allowed value (optional)
 * @param {number} max - Maximum allowed value (optional)
 * @returns {object} Validation result with isValid and message
 */
function validateVND(value, min = null, max = null) {
    const numValue = parseVND(value);
    
    if (isNaN(numValue) || numValue < 0) {
        return {
            isValid: false,
            message: 'Vui lòng nhập số tiền hợp lệ'
        };
    }
    
    if (min !== null && numValue < min) {
        return {
            isValid: false,
            message: `Số tiền tối thiểu là ${formatVND(min)} VND`
        };
    }
    
    if (max !== null && numValue > max) {
        return {
            isValid: false,
            message: `Số tiền tối đa là ${formatVND(max)} VND`
        };
    }
    
    return {
        isValid: true,
        message: ''
    };
}

/**
 * Create a VND input field with formatting
 * @param {HTMLInputElement} input - The input element to enhance
 * @param {object} options - Configuration options
 */
function createVNDInput(input, options = {}) {
    const {
        placeholder = 'Nhập số tiền...',
        min = null,
        max = null,
        required = false
    } = options;
    
    // Set attributes
    input.setAttribute('placeholder', placeholder);
    input.setAttribute('type', 'text');
    input.setAttribute('inputmode', 'numeric');
    
    if (required) {
        input.setAttribute('required', 'required');
    }
    
    // Add CSS class for styling
    input.classList.add('vnd-input');
    
    // Initialize formatting
    initVNDFormatting(`#${input.id}`);
    
    // Add validation on blur
    input.addEventListener('blur', function() {
        const validation = validateVND(this.value, min, max);
        
        // Remove previous validation classes
        this.classList.remove('is-valid', 'is-invalid');
        
        // Add appropriate class
        if (this.value && !validation.isValid) {
            this.classList.add('is-invalid');
            
            // Show error message
            let errorDiv = this.parentNode.querySelector('.invalid-feedback');
            if (!errorDiv) {
                errorDiv = document.createElement('div');
                errorDiv.className = 'invalid-feedback';
                this.parentNode.appendChild(errorDiv);
            }
            errorDiv.textContent = validation.message;
        } else if (this.value) {
            this.classList.add('is-valid');
            
            // Remove error message
            const errorDiv = this.parentNode.querySelector('.invalid-feedback');
            if (errorDiv) {
                errorDiv.remove();
            }
        }
    });
}

// Export functions for use in other scripts
window.VNDFormatter = {
    format: formatVND,
    parse: parseVND,
    formatInput: formatVNDInput,
    init: initVNDFormatting,
    validate: validateVND,
    createInput: createVNDInput
};
