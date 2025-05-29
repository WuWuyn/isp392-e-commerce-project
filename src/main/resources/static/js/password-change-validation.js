document.addEventListener('DOMContentLoaded', function() {
    const passwordForm = document.querySelector('form[action*="update-password"]');
    const currentPasswordInput = document.getElementById('currentPassword');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    
    // Check for success or error messages in URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        const errorType = urlParams.get('error');
        let errorMessage = 'Error changing password. Please try again.';
        
        if (errorType === 'current_password') {
            errorMessage = 'The current password you entered is incorrect.';
            setInvalid(currentPasswordInput, errorMessage);
            currentPasswordInput.focus();
        } else if (errorType === 'same_password') {
            errorMessage = 'New password cannot be the same as the current password.';
            setInvalid(newPasswordInput, errorMessage);
            newPasswordInput.focus();
        }
        
        // Display custom error notification if it doesn't already exist
        if (!document.querySelector('.custom-notification.error')) {
            showNotification(errorMessage, 'error');
        }
    } else if (urlParams.has('success')) {
        // Show success notification
        showNotification('Password changed successfully!', 'success');
    }
    
    if (passwordForm) {
        // Form submission validation
        passwordForm.addEventListener('submit', function(event) {
            let isValid = true;
            
            // Reset validation states
            clearValidationStates();
            // Remove any existing notifications
            removeAllNotifications();
            
            // Validate current password is not empty
            if (!currentPasswordInput.value.trim()) {
                setInvalid(currentPasswordInput, 'Please enter your current password');
                isValid = false;
            } else {
                setValid(currentPasswordInput);
            }
            
            // Validate new password meets requirements
            const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/;
            if (!passwordRegex.test(newPasswordInput.value)) {
                setInvalid(newPasswordInput, 'Password must be at least 6 characters long and include at least one letter and one number');
                isValid = false;
            } else if (newPasswordInput.value === currentPasswordInput.value) {
                setInvalid(newPasswordInput, 'New password cannot be the same as the current password');
                showNotification('New password cannot be the same as the current password', 'error', 'same-password');
                isValid = false;
            } else {
                setValid(newPasswordInput);
            }
            
            // Validate confirm password matches new password
            if (newPasswordInput.value !== confirmPasswordInput.value) {
                setInvalid(confirmPasswordInput, 'Passwords do not match');
                showNotification('New password and confirmation do not match', 'error', 'password-match');
                isValid = false;
            } else if (confirmPasswordInput.value) {
                setValid(confirmPasswordInput);
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
            } else {
                // Add a loading state to the form when submitting
                const submitBtn = passwordForm.querySelector('button[type="submit"]');
                if (submitBtn) {
                    const originalText = submitBtn.innerHTML;
                    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...';
                    submitBtn.disabled = true;
                    
                    // Re-enable the button after 10 seconds (failsafe in case of network issues)
                    setTimeout(() => {
                        submitBtn.innerHTML = originalText;
                        submitBtn.disabled = false;
                    }, 10000);
                }
            }
        });
        
        // Real-time validation
        
        // New password validation on input
        if (newPasswordInput) {
            newPasswordInput.addEventListener('input', function() {
                // Clear validation
                clearValidation(this);
                
                // If confirm password is filled, validate match
                if (confirmPasswordInput.value) {
                    if (this.value !== confirmPasswordInput.value) {
                        setInvalid(confirmPasswordInput, 'Passwords do not match');
                    } else {
                        setValid(confirmPasswordInput);
                    }
                }
                
                // Check if new password matches current password
                if (this.value && this.value === currentPasswordInput.value) {
                    setInvalid(this, 'New password cannot be the same as the current password');
                }
            });
            
            // Validate password requirements on blur
            newPasswordInput.addEventListener('blur', function() {
                if (this.value) {
                    const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/;
                    if (!passwordRegex.test(this.value)) {
                        setInvalid(this, 'Password must be at least 6 characters long and include at least one letter and one number');
                    } else if (this.value === currentPasswordInput.value) {
                        setInvalid(this, 'New password cannot be the same as the current password');
                        showNotification('New password cannot be the same as the current password', 'error', 'same-password');
                    } else {
                        setValid(this);
                    }
                }
            });
        }
        
        // Confirm password validation on input
        if (confirmPasswordInput) {
            confirmPasswordInput.addEventListener('input', function() {
                if (this.value) {
                    if (this.value !== newPasswordInput.value) {
                        setInvalid(this, 'Passwords do not match');
                    } else {
                        setValid(this);
                        // Remove any existing password match error notifications
                        const matchNotifications = document.querySelectorAll('.custom-notification.password-match');
                        matchNotifications.forEach(note => note.remove());
                    }
                } else {
                    clearValidation(this);
                }
            });
            
            // Add additional validation on blur for more visible feedback
            confirmPasswordInput.addEventListener('blur', function() {
                if (this.value && this.value !== newPasswordInput.value) {
                    setInvalid(this, 'Passwords do not match');
                    showNotification('New password and confirmation do not match', 'error', 'password-match');
                }
            });
        }
        
        // Add password strength meter (optional enhancement)
        if (newPasswordInput) {
            // Create password strength meter
            const strengthMeter = document.createElement('div');
            strengthMeter.className = 'password-strength mt-2';
            strengthMeter.innerHTML = `
                <div class="progress" style="height: 5px;">
                    <div class="progress-bar" role="progressbar" style="width: 0%;" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>
                </div>
                <small class="form-text text-muted mt-1">Password strength: <span class="strength-text">None</span></small>
            `;
            
            // Insert after the form-text div
            const formTextDiv = newPasswordInput.nextElementSibling;
            if (formTextDiv && formTextDiv.classList.contains('form-text')) {
                formTextDiv.parentNode.insertBefore(strengthMeter, formTextDiv.nextSibling);
            } else {
                newPasswordInput.parentNode.insertBefore(strengthMeter, newPasswordInput.nextSibling);
            }
            
            // Update strength meter on input
            newPasswordInput.addEventListener('input', function() {
                updatePasswordStrength(this.value);
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
    
    /**
     * Clear validation states for all inputs
     */
    function clearValidationStates() {
        if (currentPasswordInput) clearValidation(currentPasswordInput);
        if (newPasswordInput) clearValidation(newPasswordInput);
        if (confirmPasswordInput) clearValidation(confirmPasswordInput);
    }
    
    /**
     * Shows a notification to the user
     * @param {string} message - The message to display
     * @param {string} type - The type of notification ('success', 'error', 'warning')
     * @param {string} [customClass] - Optional custom class for the notification
     */
    function showNotification(message, type, customClass = '') {
        // Remove any existing notifications of the same class if provided
        if (customClass) {
            const existingNotifications = document.querySelectorAll(`.custom-notification.${customClass}`);
            existingNotifications.forEach(note => note.remove());
        }
        
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `custom-notification ${type} ${customClass}`;
        
        // Set icon based on type
        let icon = '';
        if (type === 'success') {
            icon = '<i class="fas fa-check-circle"></i>';
        } else if (type === 'error') {
            icon = '<i class="fas fa-exclamation-circle"></i>';
        } else if (type === 'warning') {
            icon = '<i class="fas fa-exclamation-triangle"></i>';
        }
        
        // Set content
        notification.innerHTML = `
            <div class="notification-content">
                ${icon} ${message}
            </div>
            <button type="button" class="notification-close">×</button>
        `;
        
        // Add styles
        notification.style.position = 'fixed';
        notification.style.top = '20px';
        notification.style.right = '20px';
        notification.style.zIndex = '1050';
        notification.style.minWidth = '300px';
        notification.style.padding = '15px';
        notification.style.borderRadius = '4px';
        notification.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
        notification.style.display = 'flex';
        notification.style.justifyContent = 'space-between';
        notification.style.alignItems = 'center';
        notification.style.animation = 'slide-in-right 0.3s ease-out forwards';
        
        // Set color based on type
        if (type === 'success') {
            notification.style.backgroundColor = '#d4edda';
            notification.style.color = '#155724';
            notification.style.border = '1px solid #c3e6cb';
        } else if (type === 'error') {
            notification.style.backgroundColor = '#f8d7da';
            notification.style.color = '#721c24';
            notification.style.border = '1px solid #f5c6cb';
        } else if (type === 'warning') {
            notification.style.backgroundColor = '#fff3cd';
            notification.style.color = '#856404';
            notification.style.border = '1px solid #ffeeba';
        }
        
        // Add to body
        document.body.appendChild(notification);
        
        // Add close button functionality
        const closeBtn = notification.querySelector('.notification-close');
        if (closeBtn) {
            closeBtn.style.background = 'none';
            closeBtn.style.border = 'none';
            closeBtn.style.color = 'inherit';
            closeBtn.style.fontSize = '1.25rem';
            closeBtn.style.cursor = 'pointer';
            closeBtn.style.padding = '0 0 0 10px';
            
            closeBtn.addEventListener('click', function() {
                notification.style.animation = 'slide-out-right 0.3s ease-out forwards';
                setTimeout(() => {
                    notification.remove();
                }, 300);
            });
        }
        
        // Auto dismiss after 5 seconds
        setTimeout(() => {
            if (document.body.contains(notification)) {
                notification.style.animation = 'slide-out-right 0.3s ease-out forwards';
                setTimeout(() => {
                    if (document.body.contains(notification)) {
                        notification.remove();
                    }
                }, 300);
            }
        }, 5000);
    }
    
    /**
     * Removes all notification elements
     */
    function removeAllNotifications() {
        const notifications = document.querySelectorAll('.custom-notification');
        notifications.forEach(note => note.remove());
    }
    
    /**
     * Update password strength meter
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
            if (password.length >= 8) strength += 25;
            
            // Contains lowercase letters
            if (password.match(/[a-z]+/)) strength += 15;
            
            // Contains uppercase letters
            if (password.match(/[A-Z]+/)) strength += 15;
            
            // Contains numbers
            if (password.match(/[0-9]+/)) strength += 15;
            
            // Contains special characters
            if (password.match(/[^A-Za-z0-9]+/)) strength += 15;
            
            // Length bonus
            if (password.length >= 12) strength += 15;
            
            // Determine status text and color
            if (strength >= 75) {
                status = 'Strong';
                color = 'success';
            } else if (strength >= 50) {
                status = 'Medium';
                color = 'warning';
            } else {
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
});