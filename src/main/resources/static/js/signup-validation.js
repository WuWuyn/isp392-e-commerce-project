/**
 * Client-side validation for the buyer signup form
 * Provides immediate feedback to users before form submission
 * Includes comprehensive validation for email, full name, and date of birth
 * With password strength meter functionality
 */
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('registrationForm');
    
    if (form) {
        // Get form elements
        const fullNameInput = document.getElementById('fullName');
        const emailInput = document.getElementById('email');
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirmPassword');
        const phoneInput = document.getElementById('phoneNumber');
        const dobInput = document.getElementById('dateOfBirth');
        
        // Add password strength meter
        if (passwordInput) {
            // Create password strength meter
            const strengthMeter = document.createElement('div');
            strengthMeter.className = 'password-strength mt-2';
            strengthMeter.innerHTML = `
                <div class="progress" style="height: 5px;">
                    <div class="progress-bar" role="progressbar" style="width: 0%;" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>
                </div>
                <small class="form-text text-muted mt-1">Password strength: <span class="strength-text">None</span></small>
            `;
            
            // Insert after the form text element if it exists
            const formText = passwordInput.nextElementSibling;
            if (formText && formText.classList.contains('form-text')) {
                passwordInput.parentNode.insertBefore(strengthMeter, formText.nextSibling);
            } else {
                passwordInput.parentNode.insertBefore(strengthMeter, passwordInput.nextSibling);
            }
            
            // Update strength meter on input
            passwordInput.addEventListener('input', function() {
                updatePasswordStrength(this.value);
                
                // Validate password requirements on input
                const passwordRegex = /^(?=.*[0-9])(?=.*[a-zA-Z]).*$/;
                if (this.value && (this.value.length < 6 || !passwordRegex.test(this.value))) {
                    setInvalid(this, 'Password must be at least 6 characters with letters and numbers');
                } else if (this.value) {
                    setValid(this);
                } else {
                    clearValidation(this);
                }
                
                // Check password match if confirm password has value
                if (confirmPasswordInput.value) {
                    if (this.value !== confirmPasswordInput.value) {
                        setInvalid(confirmPasswordInput, 'Passwords do not match');
                    } else {
                        setValid(confirmPasswordInput);
                    }
                }
            });
        }
        
        // Form submission validation
        form.addEventListener('submit', function(event) {
            let isValid = true;
            
            // Reset all validation states
            resetValidationState();
            
            // Validate full name - must have at least 2 words
            const fullNameValue = fullNameInput.value.trim();
            const fullNameWords = fullNameValue.split(/\s+/).filter(word => word.length > 0);
            
            if (!fullNameValue || fullNameWords.length < 2) {
                setInvalid(fullNameInput, 'Full name must contain at least 2 words');
                isValid = false;
            }
            
            // Validate email with comprehensive checks
            const emailValue = emailInput.value.trim();
            const isEmailValid = validateEmail(emailValue);
            
            if (!isEmailValid) {
                setInvalid(emailInput, 'Please enter a valid email address');
                isValid = false;
            }
            
            // Validate password
            const passwordRegex = /^(?=.*[0-9])(?=.*[a-zA-Z]).*$/;
            if (passwordInput.value.length < 6 || !passwordRegex.test(passwordInput.value)) {
                setInvalid(passwordInput, 'Password must be at least 6 characters with letters and numbers');
                isValid = false;
            } else {
                setValid(passwordInput);
            }
            
            // Validate password confirmation
            if (passwordInput.value !== confirmPasswordInput.value) {
                setInvalid(confirmPasswordInput, 'Passwords do not match');
                isValid = false;
            }
            
            // Validate phone number
            const phoneRegex = /^[0-9]{10,15}$/;
            if (!phoneRegex.test(phoneInput.value.trim())) {
                setInvalid(phoneInput, 'Phone number must be between 10-15 digits');
                isValid = false;
            }
            
            // Validate date of birth - cannot be future date or older than 200 years
            if (!dobInput.value) {
                setInvalid(dobInput, 'Date of birth is required');
                isValid = false;
            } else {
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
                }
            }
            
            // Prevent form submission if validation fails
            if (!isValid) {
                event.preventDefault();
            }
        });
        
        // Real-time validation for fields
        
        // Full name validation on blur
        fullNameInput.addEventListener('blur', function() {
            const fullNameValue = this.value.trim();
            if (fullNameValue) {
                const fullNameWords = fullNameValue.split(/\s+/).filter(word => word.length > 0);
                if (fullNameWords.length < 2) {
                    setInvalid(this, 'Full name must contain at least 2 words');
                } else {
                    setValid(this);
                }
            }
        });
        
        // Email validation on blur
        emailInput.addEventListener('blur', function() {
            const emailValue = this.value.trim();
            if (emailValue) {
                const isEmailValid = validateEmail(emailValue);
                if (!isEmailValid) {
                    setInvalid(this, 'Please enter a valid email address');
                } else {
                    setValid(this);
                }
            }
        });
        
        // Date of birth validation on change
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
        
        // Password match validation on input
        confirmPasswordInput.addEventListener('input', function() {
            if (passwordInput.value !== confirmPasswordInput.value) {
                setInvalid(confirmPasswordInput, 'Passwords do not match');
            } else {
                setValid(confirmPasswordInput);
            }
        });
        
        // Phone validation on blur
        phoneInput.addEventListener('blur', function() {
            const phoneValue = this.value.trim();
            if (phoneValue) {
                // Vietnamese phone number validation (10-11 digits, starts with 0)
                const vietnamesePhoneRegex = /^(0[3|5|7|8|9])+([0-9]{8})$/;
                if (!vietnamesePhoneRegex.test(phoneValue)) {
                    setInvalid(this, 'Please enter a valid Vietnamese phone number');
                } else {
                    setValid(this);
                }
            }
        });
        
        // Helper functions for validation
        function setInvalid(element, message) {
            element.classList.add('is-invalid');
            element.classList.remove('is-valid');
            
            // Find or create feedback element
            let feedback = element.nextElementSibling;
            if (!feedback || !feedback.classList.contains('invalid-feedback')) {
                feedback = document.createElement('div');
                feedback.className = 'invalid-feedback';
                element.parentNode.insertBefore(feedback, element.nextSibling);
            }
            feedback.textContent = message;
        }
        
        function setValid(element) {
            element.classList.remove('is-invalid');
            element.classList.add('is-valid');
        }
        
        function resetValidationState() {
            const inputs = form.querySelectorAll('input, select, textarea');
            inputs.forEach(input => {
                input.classList.remove('is-invalid');
                input.classList.remove('is-valid');
            });
        }
        
        /**
         * Updates the password strength meter based on password complexity
         * @param {string} password - The password to evaluate
         */
        function updatePasswordStrength(password) {
            const progressBar = document.querySelector('.password-strength .progress-bar');
            const strengthText = document.querySelector('.password-strength .strength-text');
            
            if (!progressBar || !strengthText) return;
            
            // Default values
            let strength = 0;
            let status = 'None';
            let color = 'secondary';
            
            if (password) {
                // Calculate strength
                strength = 0;
                
                // Length check
                if (password.length >= 6) strength += 20; // Basic requirement
                if (password.length >= 8) strength += 10; // Better
                
                // Contains lowercase letters
                if (password.match(/[a-z]+/)) strength += 10;
                
                // Contains uppercase letters
                if (password.match(/[A-Z]+/)) strength += 15;
                
                // Contains numbers
                if (password.match(/[0-9]+/)) strength += 15;
                
                // Contains special characters
                if (password.match(/[^A-Za-z0-9]+/)) strength += 15;
                
                // Length bonus
                if (password.length >= 10) strength += 15;
                
                // Determine status text and color
                if (strength >= 75) {
                    status = 'Strong';
                    color = 'success';
                } else if (strength >= 50) {
                    status = 'Medium';
                    color = 'warning';
                } else if (strength > 0) {
                    status = 'Weak';
                    color = 'danger';
                }
            }
            
            // Update UI
            progressBar.style.width = strength + '%';
            progressBar.setAttribute('aria-valuenow', strength);
            progressBar.className = 'progress-bar bg-' + color;
            strengthText.textContent = status;
            strengthText.className = 'strength-text text-' + color;
        }
        
        /**
         * Helper function to clear validation state
         * @param {HTMLElement} input - Input element to clear validation for
         */
        function clearValidation(input) {
            input.classList.remove('is-invalid', 'is-valid');
            
            // Hide feedback if exists
            const feedback = input.nextElementSibling;
            if (feedback && feedback.classList.contains('invalid-feedback')) {
                feedback.style.display = 'none';
            }
        }
    }
});
