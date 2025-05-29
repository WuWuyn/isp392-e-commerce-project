/**
 * Login form validation
 */
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.querySelector('form[action*="process_login"]');
    const emailInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');

    if (loginForm) {
        loginForm.addEventListener('submit', function(event) {
            let isValid = true;

            // Email validation
            if (emailInput) {
                const isEmailValid = validateEmail(emailInput.value);
                if (!isEmailValid) {
                    setInvalid(emailInput, 'Please enter a valid email address');
                    isValid = false;
                } else {
                    setValid(emailInput);
                }
            }

            // Password validation - simple check for empty
            if (passwordInput) {
                if (!passwordInput.value.trim()) {
                    setInvalid(passwordInput, 'Password is required');
                    isValid = false;
                } else {
                    setValid(passwordInput);
                }
            }

            if (!isValid) {
                event.preventDefault();
                // Focus on the first invalid field
                const firstInvalidField = document.querySelector('.is-invalid');
                if (firstInvalidField) {
                    firstInvalidField.focus();
                }
            }
        });

        // Live validation on blur for email
        if (emailInput) {
            emailInput.addEventListener('blur', function() {
                if (this.value.trim()) {
                    const isEmailValid = validateEmail(this.value);
                    if (!isEmailValid) {
                        setInvalid(this, 'Please enter a valid email address');
                    } else {
                        setValid(this);
                    }
                }
            });

            // Clear validation on input
            emailInput.addEventListener('input', function() {
                this.classList.remove('is-invalid', 'is-valid');
                const feedback = this.nextElementSibling;
                if (feedback && feedback.classList.contains('invalid-feedback')) {
                    feedback.style.display = 'none';
                }
            });
        }

        // Clear validation on input for password
        if (passwordInput) {
            passwordInput.addEventListener('input', function() {
                this.classList.remove('is-invalid', 'is-valid');
                const feedback = this.nextElementSibling;
                if (feedback && feedback.classList.contains('invalid-feedback')) {
                    feedback.style.display = 'none';
                }
            });
        }
    }

    /**
     * Comprehensive email validation function
     * @param {string} email - The email to validate
     * @returns {boolean} - Whether the email is valid
     */
    function validateEmail(email) {
        if (!email) return false;
        
        // Remove leading/trailing whitespace
        email = email.trim();
        
        // Check if empty after trimming
        if (email === '') return false;
        
        // Check for multiple @ symbols
        if ((email.match(/@/g) || []).length !== 1) return false;
        
        // Check total length (RFC says 254 chars max)
        if (email.length > 254) return false;
        
        // Comprehensive regex for email validation
        // This regex aims to follow RFC specifications while still being practical
        const emailRegex = /^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])$/i;
        
        // Final validation
        return emailRegex.test(email);
    }

    /**
     * Set invalid state for an input
     * @param {HTMLElement} input - The input to set as invalid
     * @param {string} message - The error message to display
     */
    function setInvalid(input, message) {
        input.classList.add('is-invalid');
        input.classList.remove('is-valid');
        const feedback = input.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.textContent = message;
            feedback.style.display = 'block';
        }
    }

    /**
     * Set valid state for an input
     * @param {HTMLElement} input - The input to set as valid
     */
    function setValid(input) {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
        const feedback = input.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.style.display = 'none';
        }
    }
});
