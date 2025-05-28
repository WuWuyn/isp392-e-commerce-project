/**
 * Validation for buyer account edit form
 * Includes validation for full name, phone number, and date of birth
 */
document.addEventListener('DOMContentLoaded', function() {
    const editForm = document.querySelector('form[action*="update-info"]');
    const fullNameInput = document.getElementById('editFullName');
    const phoneInput = document.getElementById('editPhone');
    const dobInput = document.getElementById('editDateOfBirth');
    
    if (editForm) {
        // Add success message if operation was successful (URL param)
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('success')) {
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-success mb-4';
            alertDiv.role = 'alert';
            alertDiv.innerHTML = '<i class="fas fa-check-circle me-2"></i> Your profile information has been successfully updated.';
            
            // Insert alert before the form
            editForm.parentNode.insertBefore(alertDiv, editForm);
            
            // Auto-dismiss after 5 seconds
            setTimeout(() => {
                alertDiv.classList.add('fade');
                setTimeout(() => alertDiv.remove(), 500);
            }, 5000);
        }
        
        // Form submission validation
        editForm.addEventListener('submit', function(event) {
            let isValid = true;
            
            // Validate full name - must be at least one word, no special characters only
            const fullNameValue = fullNameInput.value.trim();
            const nameRegex = /^[A-Za-zÀ-ỹ\s]+$/; // Allow letters and spaces (including Vietnamese characters)
            const containsOnlySpecialChars = /^[^A-Za-zÀ-ỹ0-9]+$/; // Check if contains only special characters
            
            if (!fullNameValue || fullNameValue.length < 2 || !nameRegex.test(fullNameValue) || containsOnlySpecialChars.test(fullNameValue)) {
                setInvalid(fullNameInput, 'Full name must contain at least 2 characters and cannot contain only special characters');
                isValid = false;
            } else {
                setValid(fullNameInput);
            }
            
            // Validate phone number (Vietnamese format)
            if (phoneInput.value.trim()) {
                const vietnamesePhoneRegex = /^(0[3|5|7|8|9])+([0-9]{8})$/;
                if (!vietnamesePhoneRegex.test(phoneInput.value.trim())) {
                    setInvalid(phoneInput, 'Please enter a valid Vietnamese phone number');
                    isValid = false;
                } else {
                    setValid(phoneInput);
                }
            }
            
            // Validate date of birth - cannot be future date or older than 200 years
            if (dobInput.value) {
                const dobDate = new Date(dobInput.value);
                const today = new Date();
                const twoHundredYearsAgo = new Date();
                twoHundredYearsAgo.setFullYear(today.getFullYear() - 200);
                
                // Check for future date
                if (dobDate > today) {
                    setInvalid(dobInput, 'Date of birth cannot be in the future');
                    isValid = false;
                }
                // Check for date older than 200 years
                else if (dobDate < twoHundredYearsAgo) {
                    setInvalid(dobInput, 'Date of birth cannot be more than 200 years ago');
                    isValid = false;
                } else {
                    setValid(dobInput);
                }
            }
            
            // Prevent form submission if validation fails
            if (!isValid) {
                event.preventDefault();
                // Focus on the first invalid field
                const firstInvalid = document.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }
        });
        
        // Real-time validation
        
        // Full name validation on blur
        if (fullNameInput) {
            fullNameInput.addEventListener('blur', function() {
                const fullNameValue = this.value.trim();
                const nameRegex = /^[A-Za-zÀ-ỹ\s]+$/; // Allow letters and spaces (including Vietnamese characters)
                const containsOnlySpecialChars = /^[^A-Za-zÀ-ỹ0-9]+$/; // Check if contains only special characters
                
                if (fullNameValue) {
                    if (fullNameValue.length < 2 || !nameRegex.test(fullNameValue) || containsOnlySpecialChars.test(fullNameValue)) {
                        setInvalid(this, 'Full name must contain at least 2 characters and cannot contain only special characters');
                    } else {
                        setValid(this);
                    }
                }
            });
            
            // Clear validation on input
            fullNameInput.addEventListener('input', function() {
                clearValidation(this);
            });
        }
        
        // Phone validation on blur
        if (phoneInput) {
            phoneInput.addEventListener('blur', function() {
                const phoneValue = this.value.trim();
                if (phoneValue) {
                    const vietnamesePhoneRegex = /^(0[3|5|7|8|9])+([0-9]{8})$/;
                    if (!vietnamesePhoneRegex.test(phoneValue)) {
                        setInvalid(this, 'Please enter a valid Vietnamese phone number');
                    } else {
                        setValid(this);
                    }
                }
            });
            
            // Clear validation on input
            phoneInput.addEventListener('input', function() {
                clearValidation(this);
            });
        }
        
        // Date of birth validation on change
        if (dobInput) {
            dobInput.addEventListener('change', function() {
                if (this.value) {
                    const dobDate = new Date(this.value);
                    const today = new Date();
                    const twoHundredYearsAgo = new Date();
                    twoHundredYearsAgo.setFullYear(today.getFullYear() - 200);
                    
                    if (dobDate > today) {
                        setInvalid(this, 'Date of birth cannot be in the future');
                    } else if (dobDate < twoHundredYearsAgo) {
                        setInvalid(this, 'Date of birth cannot be more than 200 years ago');
                    } else {
                        setValid(this);
                    }
                }
            });
        }
    }
    
    /**
     * Set invalid state for an input
     * @param {HTMLElement} input - The input to set as invalid
     * @param {string} message - The error message to display
     */
    function setInvalid(input, message) {
        input.classList.add('is-invalid');
        input.classList.remove('is-valid');
        
        // Find or create feedback element
        let feedback = input.nextElementSibling;
        if (!feedback || !feedback.classList.contains('invalid-feedback')) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            input.parentNode.insertBefore(feedback, input.nextSibling);
        }
        
        feedback.textContent = message;
        feedback.style.display = 'block';
    }
    
    /**
     * Set valid state for an input
     * @param {HTMLElement} input - The input to set as valid
     */
    function setValid(input) {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
        
        // Hide feedback if exists
        const feedback = input.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.style.display = 'none';
        }
    }
    
    /**
     * Clear validation state for an input
     * @param {HTMLElement} input - The input to clear validation
     */
    function clearValidation(input) {
        input.classList.remove('is-invalid', 'is-valid');
        
        // Hide feedback if exists
        const feedback = input.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.style.display = 'none';
        }
    }
});
