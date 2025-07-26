/**
 * ReadHub Seller - Add Product JavaScript
 * This file contains all client-side validation and functionality 
 * for the seller product addition form
 */

// Preview uploaded image
function previewImage(event) {
    const file = event.target.files[0];
    const imagePreview = document.getElementById('imagePreview');
    const imageFormatError = document.getElementById('imageFormatError');
    
    // Check if file is an image
    const validFormats = ['image/jpeg', 'image/png', 'image/webp'];
    if (file && validFormats.includes(file.type)) {
        const reader = new FileReader();
        reader.onload = function() {
            imagePreview.src = reader.result;
            // Store image data in localStorage to retain on form resubmission
            localStorage.setItem('bookCoverImageData', reader.result);
        }
        reader.readAsDataURL(file);
        imageFormatError.style.display = 'none';
        event.target.setCustomValidity('');
    } else if (file) {
        imagePreview.src = "https://placehold.co/300x450/e2e8f0/dc3545?text=Invalid+Format";
        imageFormatError.style.display = 'block';
        event.target.setCustomValidity('Invalid file format');
        // Clear stored image data
        localStorage.removeItem('bookCoverImageData');
    }
}

// Restore image preview from localStorage if available
function restoreImagePreview() {
    const imagePreview = document.getElementById('imagePreview');
    const storedImageData = localStorage.getItem('bookCoverImageData');
    
    if (imagePreview && storedImageData) {
        imagePreview.src = storedImageData;
    }
}

// Set max date for publication date to today
function setMaxDate() {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    const maxDate = `${yyyy}-${mm}-${dd}`;
    document.getElementById('publicationDate').setAttribute('max', maxDate);
}

// Calculate discount based on original and selling price
function calculateDiscount() {
    const originalPrice = parseFloat(document.getElementById('originalPrice').value);
    const sellingPrice = parseFloat(document.getElementById('sellingPrice').value);
    
    if (originalPrice && sellingPrice && originalPrice > 0) {
        const discountPercent = ((originalPrice - sellingPrice) / originalPrice) * 100;
        document.getElementById('discountPercent').value = discountPercent.toFixed(2);
    }
}

// Calculate selling price based on original price and discount
function calculateSellingPrice() {
    const originalPrice = parseFloat(document.getElementById('originalPrice').value);
    const discountPercent = parseFloat(document.getElementById('discountPercent').value);
    
    if (originalPrice && discountPercent && originalPrice > 0) {
        const sellingPrice = originalPrice * (1 - (discountPercent / 100));
        document.getElementById('sellingPrice').value = sellingPrice.toFixed(2);
    }
}

// Count words in description and validate max 200 words
function checkWordCount(textarea) {
    const text = textarea.value;
    const wordCount = text.trim().split(/\s+/).length;
    document.getElementById('wordCount').textContent = wordCount;
    
    if (wordCount > 200) {
        textarea.setCustomValidity('Description must not exceed 200 words');
    } else {
        textarea.setCustomValidity('');
    }
}

// Initialize form validation and components
document.addEventListener('DOMContentLoaded', function() {
    setMaxDate();
    
    // Bootstrap form validation - support both add and edit forms
    const form = document.getElementById('addBookForm') || document.getElementById('editBookForm');
    if (form) {
        // Restore image preview if available
        restoreImagePreview();
        
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
                
                // Scroll to top where errors are displayed
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
            form.classList.add('was-validated');
        });
    }
    
    // Check description word count on load
    const description = document.getElementById('description');
    if (description && description.value) {
        checkWordCount(description);
    }
    
    // Fix for image upload - handle large images appropriately
    const imageInput = document.getElementById('coverImageFile');
    if (imageInput) {
        // Check if we have errors and restore the file input if needed
        const hasErrors = document.querySelector('.alert-danger:not([style*="display: none"])');
        if (hasErrors && localStorage.getItem('bookCoverImageData')) {
            // Display the preview restored from localStorage
            restoreImagePreview();
        }
        
        imageInput.addEventListener('change', function(event) {
            const file = event.target.files[0];
            if (file && file.size > 5 * 1024 * 1024) { // 5MB limit
                imageInput.setCustomValidity('Image size should be less than 5MB');
                document.getElementById('imageFormatError').textContent = 'Image size should be less than 5MB';
                document.getElementById('imageFormatError').style.display = 'block';
                // Clear stored image data
                localStorage.removeItem('bookCoverImageData');
            } else {
                imageInput.setCustomValidity('');
            }
        });
    }
}); 