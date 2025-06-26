/**
 * ReadHub - Book Validation JavaScript
 * This file provides client-side validation for the book form submissions
 * It validates all fields according to requirements before form submission
 */

document.addEventListener('DOMContentLoaded', function() {
    // Get form reference - support both add and edit forms
    const bookForm = document.getElementById('addBookForm') || document.getElementById('editBookForm');
    if (!bookForm) return; // Exit if form not found
    
    // Log form ID for debugging
    console.log("Form initialized:", bookForm.id);

    // Form fields to validate
    const fields = {
        title: {
            element: document.getElementById('title'),
            errorMsg: 'Book title is required',
            validator: (value) => value.trim().length > 0
        },
        authors: {
            element: document.getElementById('authors'),
            errorMsg: 'Author name(s) is required',
            validator: (value) => value.trim().length > 0
        },
        publisherId: {
            element: document.getElementById('publisherId'),
            errorMsg: 'Please select a publisher',
            validator: (value) => value && value.trim() !== ''
        },
        originalPrice: {
            element: document.getElementById('originalPrice'),
            errorMsg: 'Original price must be at least 1,000 VND',
            validator: (value) => !isNaN(value) && parseInt(value) >= 1000
        },
        sellingPrice: {
            element: document.getElementById('sellingPrice'),
            errorMsg: 'Selling price must be at least 1,000 VND',
            validator: (value) => !isNaN(value) && parseInt(value) >= 1000
        },
        description: {
            element: document.getElementById('description'),
            errorMsg: 'Description is required (max 2000 characters)',
            validator: (value) => value.trim().length > 0 && value.length <= 2000
        },
        publicationDate: {
            element: document.getElementById('publicationDate'),
            errorMsg: 'Valid publication date is required (not in the future)',
            validator: (value) => {
                if (!value) return false;
                const selectedDate = new Date(value);
                const today = new Date();
                return selectedDate <= today;
            }
        },
        isbn: {
            element: document.getElementById('isbn'),
            errorMsg: 'Valid ISBN (10 or 13 digits) is required',
            validator: (value) => {
                // Simple ISBN validation (more complex validation could be added)
                const cleanedValue = value.replace(/[-\s]/g, '');
                return /^(\d{10}|\d{13})$/.test(cleanedValue);
            }
        },
        numberOfPages: {
            element: document.getElementById('numberOfPages'),
            errorMsg: 'Number of pages must be greater than 0',
            validator: (value) => !isNaN(value) && parseInt(value) > 0
        },
        dimensions: {
            element: document.getElementById('dimensions'),
            errorMsg: 'Dimensions are required (format: width x height x thickness)',
            validator: (value) => /^\d+x\d+x\d+(\.\d+)?$/.test(value)
        },
        sku: {
            element: document.getElementById('sku'),
            errorMsg: 'SKU is required',
            validator: (value) => value.trim().length > 0
        },
        stockQuantity: {
            element: document.getElementById('stockQuantity'),
            errorMsg: 'Stock quantity must be 0 or greater',
            validator: (value) => !isNaN(value) && parseInt(value) >= 0
        },
        categoryIds: {
            // Special case: multiple checkboxes
            errorMsg: 'At least one category must be selected',
            validator: () => {
                const checkboxes = document.querySelectorAll('input[name="categoryIds"]');
                return Array.from(checkboxes).some(cb => cb.checked);
            }
        },
        coverImageFile: {
            element: document.getElementById('coverImageFile'),
            errorMsg: 'Please upload a cover image (JPG, PNG, GIF, or WEBP)',
            validator: (value, element) => {
                // For new book without image, file is required
                const isAddForm = bookForm.id === 'addBookForm';
                if (isAddForm && (!element.files || element.files.length === 0)) {
                    console.log("Cover image validation failed: No file selected for new book");
                    return false;
                }
                
                // If no file selected (and has existing image), skip validation
                if (!element.files || element.files.length === 0) return true;
                
                // Validate file type
                const file = element.files[0];
                const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
                const isValidType = validTypes.includes(file.type);
                
                if (!isValidType) {
                    console.log("Cover image validation failed: Invalid file type", file.type);
                }
                
                return isValidType;
            }
        }
    };

    /**
     * Validate all form fields and show errors
     * @returns {boolean} Whether form is valid
     */
    function validateForm() {
        let isFormValid = true;
        const errorList = [];
        
        console.log("Running form validation");

        // Validate each field
        Object.entries(fields).forEach(([fieldName, field]) => {
            let isFieldValid = true;
            
            // Special case for category checkboxes
            if (fieldName === 'categoryIds') {
                isFieldValid = field.validator();
                const categorySection = document.querySelector('.category-selection');
                if (!isFieldValid) {
                    categorySection.classList.add('is-invalid');
                    errorList.push(field.errorMsg);
                    console.log("Validation failed: No categories selected");
                } else {
                    categorySection.classList.remove('is-invalid');
                }
            }
            // Normal field validation
            else if (field.element) {
                isFieldValid = field.validator(field.element.value, field.element);
                
                if (!isFieldValid) {
                    field.element.classList.add('is-invalid');
                    // If field has next sibling with invalid-feedback class, use that
                    const feedbackElement = field.element.nextElementSibling?.classList.contains('invalid-feedback') 
                        ? field.element.nextElementSibling 
                        : null;
                    
                    if (feedbackElement) {
                        feedbackElement.textContent = field.errorMsg;
                    }
                    errorList.push(field.errorMsg);
                    console.log(`Validation failed for ${fieldName}: ${field.errorMsg}`);
                } else {
                    field.element.classList.remove('is-invalid');
                }
            }

            // Update overall form validity
            isFormValid = isFormValid && isFieldValid;
        });

        // Special validation for price relation
        const originalPrice = parseFloat(fields.originalPrice.element.value);
        const sellingPrice = parseFloat(fields.sellingPrice.element.value);
        
        if (!isNaN(originalPrice) && !isNaN(sellingPrice)) {
            // Selling price should be between 50% and 150% of original price
            const lowerBound = originalPrice * 0.5;
            const upperBound = originalPrice * 1.5;
            
            if (sellingPrice < lowerBound || sellingPrice > upperBound) {
                fields.sellingPrice.element.classList.add('is-invalid');
                const feedbackElement = fields.sellingPrice.element.closest('.input-group').nextElementSibling;
                if (feedbackElement && feedbackElement.classList.contains('invalid-feedback')) {
                    feedbackElement.textContent = 'Selling price must be between 50% and 150% of original price';
                }
                errorList.push('Selling price must be between 50% and 150% of original price');
                isFormValid = false;
                console.log("Validation failed: Selling price outside allowed range");
            }
        }

        // Display validation summary if there are errors
        const validationSummary = document.getElementById('validationErrorSummary');
        const errorListElement = document.getElementById('errorList');
        
        if (validationSummary && errorListElement) {
            if (errorList.length > 0) {
                errorListElement.innerHTML = '';
                errorList.forEach(error => {
                    const li = document.createElement('li');
                    li.textContent = error;
                    errorListElement.appendChild(li);
                });
                validationSummary.style.display = 'block';
                console.log("Showing validation summary with errors:", errorList);
            } else {
                validationSummary.style.display = 'none';
            }
        }
        
        console.log("Form validation result:", isFormValid);
        return isFormValid;
    }

    // Add field-level validation on blur
    Object.entries(fields).forEach(([fieldName, field]) => {
        if (field.element) {
            field.element.addEventListener('blur', function() {
                if (field.validator(this.value, this)) {
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                } else {
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                    
                    // Update error message
                    let feedbackElement;
                    
                    // Special handling for fields inside input groups
                    if (this.closest('.input-group')) {
                        feedbackElement = this.closest('.input-group').nextElementSibling;
                        if (feedbackElement && !feedbackElement.classList.contains('invalid-feedback')) {
                            feedbackElement = feedbackElement.nextElementSibling;
                        }
                    } else {
                        feedbackElement = this.nextElementSibling;
                    }
                    
                    if (feedbackElement && feedbackElement.classList.contains('invalid-feedback')) {
                        feedbackElement.textContent = field.errorMsg;
                    }
                }
            });
        }
    });

    // Special handling for category checkboxes
    const categoryCheckboxes = document.querySelectorAll('input[name="categoryIds"]');
    categoryCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            // Only need ONE category selected, not all
            const isValid = Array.from(categoryCheckboxes).some(cb => cb.checked);
            const categorySection = document.querySelector('.category-selection');
            
            if (isValid) {
                categorySection.classList.remove('is-invalid');
                categorySection.classList.add('is-valid');
                console.log("Category validation passed: At least one selected");
            } else {
                categorySection.classList.add('is-invalid');
                categorySection.classList.remove('is-valid');
                console.log("Category validation failed: None selected");
            }
        });
    });

    // Set today as max date for publication date
    const publicationDateField = document.getElementById('publicationDate');
    if (publicationDateField) {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');
        publicationDateField.setAttribute('max', `${year}-${month}-${day}`);
    }

    // Watch selling price and original price for relationship validation
    const originalPriceField = document.getElementById('originalPrice');
    const sellingPriceField = document.getElementById('sellingPrice');
    
    if (originalPriceField && sellingPriceField) {
        const validatePrices = function() {
            const originalPrice = parseFloat(originalPriceField.value);
            const sellingPrice = parseFloat(sellingPriceField.value);
            
            if (!isNaN(originalPrice) && !isNaN(sellingPrice)) {
                // Validate range (50-150% of original price)
                const lowerBound = originalPrice * 0.5;
                const upperBound = originalPrice * 1.5;
                
                if (sellingPrice < lowerBound || sellingPrice > upperBound) {
                    sellingPriceField.classList.add('is-invalid');
                    sellingPriceField.classList.remove('is-valid');
                    
                    const feedbackElement = sellingPriceField.closest('.input-group').nextElementSibling;
                    if (feedbackElement && feedbackElement.classList.contains('invalid-feedback')) {
                        feedbackElement.textContent = 'Selling price must be between 50% and 150% of original price';
                    }
                } else {
                    sellingPriceField.classList.remove('is-invalid');
                    // Only add is-valid class if the field is not empty
                    if (sellingPrice > 0) {
                        sellingPriceField.classList.add('is-valid');
                    }
                }
            }
        };
        
        originalPriceField.addEventListener('input', validatePrices);
        sellingPriceField.addEventListener('input', validatePrices);
    }

    // Watch character count in description
    const descriptionField = document.getElementById('description');
    const charCountDisplay = document.getElementById('charCount');
    
    if (descriptionField && charCountDisplay) {
        descriptionField.addEventListener('input', function() {
            const count = this.value.length;
            charCountDisplay.textContent = count;
            
            if (count > 2000) {
                this.classList.add('is-invalid');
                this.classList.remove('is-valid');
            } else if (count > 0) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
            } else {
                this.classList.add('is-invalid');
                this.classList.remove('is-valid');
            }
        });
        
        // Set initial count
        charCountDisplay.textContent = descriptionField.value.length;
    }

    // Submit handler with validation
    bookForm.addEventListener('submit', function(event) {
        console.log("Form submission attempt");
        if (!validateForm()) {
            event.preventDefault();
            event.stopPropagation();
            console.log("Form submission prevented due to validation errors");
            
            // Scroll to top where errors are displayed
            window.scrollTo({ top: 0, behavior: 'smooth' });
            
            // Show notification for validation errors
            showNotification("Please fix the validation errors before submitting.", "danger");
        } else {
            console.log("Form validation passed, proceeding with submission");
        }
    });
    
    // Function to show notifications
    window.showNotification = function(message, type) {
        const container = document.getElementById('notificationContainer');
        if (!container) return;

        // Create notification
        const notification = document.createElement('div');
        notification.className = `notification alert alert-${type}`;
        notification.innerHTML = `
            <div class="d-flex">
                <div class="me-3">
                    <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'} fa-2x"></i>
                </div>
                <div>
                    <h5>${type === 'success' ? 'Success!' : 'Error!'}</h5>
                    <p class="mb-0">${message}</p>
                </div>
                <button type="button" class="btn-close ms-auto" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;

        // Add to container
        container.appendChild(notification);

        // Show with animation
        setTimeout(() => {
            notification.classList.add('show');
        }, 10);

        // Auto-hide after 5 seconds
        setTimeout(() => {
            notification.classList.remove('show');
            setTimeout(() => {
                notification.remove();
            }, 300);
        }, 5000);
    };
    
    // Show notifications on page load if messages exist
    document.addEventListener('DOMContentLoaded', function() {
        const successMessage = document.querySelector('.alert-success');
        const errorMessage = document.querySelector('.alert-danger:not(#validationErrorSummary)');

        if (successMessage) {
            showNotification(successMessage.querySelector('span').textContent, 'success');
            successMessage.remove(); // Remove original message
        }

        if (errorMessage) {
            showNotification(errorMessage.querySelector('span').textContent, 'danger');
            errorMessage.remove(); // Remove original message
        }
    });
}); 