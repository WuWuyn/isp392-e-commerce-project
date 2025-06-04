/**
 * Password validation and strength checker
 * Validates that password is at least 6 characters long and contains at least one letter and one number
 * Also provides visual feedback on password strength
 */

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('resetPasswordForm');
    const newPassword = document.getElementById('newPassword');
    const confirmPassword = document.getElementById('confirmPassword');
    const togglePassword = document.getElementById('togglePassword');
    const toggleConfirmPassword = document.getElementById('toggleConfirmPassword');
    const toggleIcon = document.getElementById('toggleIcon');
    const toggleConfirmIcon = document.getElementById('toggleConfirmIcon');
    const passwordStrengthContainer = document.getElementById('passwordStrengthContainer');
    const passwordStrength = document.getElementById('passwordStrength');
    const strengthLevel = document.getElementById('strengthLevel');
    const submitButton = document.getElementById('submitButton');
    
    // Toggle password visibility
    if (togglePassword) {
        togglePassword.addEventListener('click', function() {
            const type = newPassword.getAttribute('type') === 'password' ? 'text' : 'password';
            newPassword.setAttribute('type', type);
            toggleIcon.classList.toggle('fa-eye');
            toggleIcon.classList.toggle('fa-eye-slash');
        });
    }
    
    if (toggleConfirmPassword) {
        toggleConfirmPassword.addEventListener('click', function() {
            const type = confirmPassword.getAttribute('type') === 'password' ? 'text' : 'password';
            confirmPassword.setAttribute('type', type);
            toggleConfirmIcon.classList.toggle('fa-eye');
            toggleConfirmIcon.classList.toggle('fa-eye-slash');
        });
    }
    
    // Password validation on input
    if (newPassword) {
        newPassword.addEventListener('input', function() {
            validatePassword();
            validatePasswordMatch();
        });
    }
    
    // Confirm password validation on input
    if (confirmPassword) {
        confirmPassword.addEventListener('input', validatePasswordMatch);
    }
    
    // Form submission
    if (form) {
        form.addEventListener('submit', function(event) {
            if (!validateForm()) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                // Disable button to prevent double submission
                if (submitButton) {
                    submitButton.disabled = true;
                    submitButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...';
                }
            }
            
            form.classList.add('was-validated');
        });
    }
    
    /**
     * Validate the entire form
     * @returns {boolean} true if form is valid, false otherwise
     */
    function validateForm() {
        const isPasswordValid = validatePassword();
        const isMatchValid = validatePasswordMatch();
        return isPasswordValid && isMatchValid;
    }
    
    /**
     * Validate password meets requirements
     * @returns {boolean} true if password is valid, false otherwise
     */
    function validatePassword() {
        if (!newPassword) return false;
        
        const password = newPassword.value;
        const isValid = password.length >= 6 && /[a-zA-Z]/.test(password) && /[0-9]/.test(password);
        
        // Update UI
        if (password.length > 0) {
            updatePasswordStrength(password);
            passwordStrengthContainer.style.display = 'block';
            
            if (isValid) {
                newPassword.classList.remove('is-invalid');
                newPassword.classList.add('is-valid');
            } else {
                newPassword.classList.remove('is-valid');
                newPassword.classList.add('is-invalid');
                document.getElementById('newPasswordFeedback').textContent = 
                    'Password must be at least 6 characters long and include at least one number and one letter.';
            }
        } else {
            passwordStrengthContainer.style.display = 'none';
            newPassword.classList.remove('is-valid', 'is-invalid');
        }
        
        return isValid;
    }
    
    /**
     * Validate that passwords match
     * @returns {boolean} true if passwords match, false otherwise
     */
    function validatePasswordMatch() {
        if (!confirmPassword || !newPassword) return false;
        
        // If confirm password is empty, don't show error yet
        if (confirmPassword.value.length === 0) {
            confirmPassword.classList.remove('is-valid', 'is-invalid');
            return false;
        }
        
        const isMatch = confirmPassword.value === newPassword.value;
        
        // Update UI
        if (isMatch) {
            confirmPassword.classList.remove('is-invalid');
            confirmPassword.classList.add('is-valid');
        } else {
            confirmPassword.classList.remove('is-valid');
            confirmPassword.classList.add('is-invalid');
            document.getElementById('confirmPasswordFeedback').textContent = 'Passwords do not match';
        }
        
        return isMatch;
    }
    
    /**
     * Update password strength meter
     * @param {string} password The password to evaluate
     */
    function updatePasswordStrength(password) {
        if (!passwordStrength || !strengthLevel) return;
        
        // Calculate strength (0-100)
        let strength = 0;
        
        // Length (up to 50%)
        const length = Math.min(password.length, 12); // Cap at 12 for scoring
        strength += (length / 12) * 50;
        
        // Complexity (up to 50%)
        let complexity = 0;
        if (/[a-z]/.test(password)) complexity += 10; // Lowercase
        if (/[A-Z]/.test(password)) complexity += 10; // Uppercase
        if (/[0-9]/.test(password)) complexity += 10; // Numbers
        if (/[^a-zA-Z0-9]/.test(password)) complexity += 20; // Special chars
        
        strength += (complexity / 50) * 50; // Scale to 50%
        
        // Update progress bar
        passwordStrength.style.width = Math.round(strength) + '%';
        
        // Update strength text and color
        let strengthText = 'Weak';
        let strengthClass = 'danger';
        
        if (strength > 70) {
            strengthText = 'Strong';
            strengthClass = 'success';
        } else if (strength > 40) {
            strengthText = 'Medium';
            strengthClass = 'warning';
        }
        
        passwordStrength.className = `progress-bar bg-${strengthClass}`;
        strengthLevel.textContent = strengthText;
        strengthLevel.className = `text-${strengthClass}`;
    }
});
