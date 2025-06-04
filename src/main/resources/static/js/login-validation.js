/**
 * Login form validation
 */
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.querySelector('form[action*="process_login"]') || document.getElementById('loginForm');
    const emailInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const rememberMeInput = document.getElementById('rememberMe');

    // Check URL parameters for messages
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        // Don't show duplicate notification, the template already shows the error message
        // Just mark the fields as invalid for visual feedback
        setInvalid(emailInput, 'Please check your email');
        setInvalid(passwordInput, 'Please check your password');
        emailInput.focus();
    } else if (urlParams.has('logout')) {
        // Don't need to show duplicate logout notification, already in template
    } else if (urlParams.has('success')) {
        showNotification(urlParams.get('success'), 'success');
    }

    // Google Sign-In callback
    window.handleGoogleSignIn = function(googleUser) {
        const idToken = googleUser.credential;
        fetch('/api/google-signin', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ idToken: idToken })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('Signed in with Google successfully!', 'success');
                window.location.href = '/home';
            } else {
                showNotification(data.error || 'Google Sign-In failed.', 'error');
            }
        })
        .catch(error => {
            showNotification('Error during Google Sign-In.', 'error');
            console.error('Google Sign-In error:', error);
        });
    };

    if (loginForm) {
        loginForm.addEventListener('submit', function(event) {
            let isValid = true;
            clearValidationStates();

            // Validate email
            if (emailInput) {
                const isEmailValid = validateEmail(emailInput.value);
                if (!isEmailValid) {
                    setInvalid(emailInput, 'Please enter a valid email address');
                    isValid = false;
                } else {
                    setValid(emailInput);
                }
            }

            // Validate password
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
                const firstInvalid = document.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
            } else {
                const submitBtn = loginForm.querySelector('button[type="submit"]');
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Logging in...';
                submitBtn.disabled = true;
            }
        });

        // Real-time email validation
        if (emailInput) {
            emailInput.addEventListener('input', function() {
                clearValidation(this);
                if (this.value.trim()) {
                    const isEmailValid = validateEmail(this.value);
                    if (!isEmailValid) {
                        setInvalid(this, 'Please enter a valid email address');
                    } else {
                        setValid(this);
                    }
                } else {
                    setInvalid(this, 'Email is required');
                }
            });

            // Also keep the blur validation for better UX
            emailInput.addEventListener('blur', function() {
                if (this.value.trim()) {
                    const isEmailValid = validateEmail(this.value);
                    if (!isEmailValid) {
                        setInvalid(this, 'Please enter a valid email address');
                    } else {
                        setValid(this);
                    }
                } else {
                    setInvalid(this, 'Email is required');
                }
            });
        }

        // Real-time password validation
        if (passwordInput) {
            passwordInput.addEventListener('input', function() {
                clearValidation(this);
                if (!this.value.trim()) {
                    setInvalid(this, 'Password is required');
                } else {
                    setValid(this);
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

    /**
     * Clear validation state
     * @param {HTMLElement} input - Input element
     */
    function clearValidation(input) {
        input.classList.remove('is-invalid', 'is-valid');
        const feedback = input.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.style.display = 'none';
        }
    }

    /**
     * Clear all validation states
     */
    function clearValidationStates() {
        const inputs = [emailInput, passwordInput, rememberMeInput];
        inputs.forEach(input => input && clearValidation(input));
    }

    /**
     * Show notification
     * @param {string} message - Message to display
     * @param {string} type - 'success' or 'error'
     */
    function showNotification(message, type) {
        const notification = document.createElement('div');
        notification.className = `alert alert-${type === 'error' ? 'danger' : 'success'} text-center mb-3`;
        notification.innerHTML = `<i class="fas fa-${type === 'success' ? 'check' : 'exclamation'}-circle me-2"></i> ${message}`;
        document.querySelector('.login-container').prepend(notification);
        setTimeout(() => notification.remove(), 5000);
    }
});