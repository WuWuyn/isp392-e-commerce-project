/**
 * Client-side validation for new-password.html
 **/
document.addEventListener('DOMContentLoaded', function() {
    const newPasswordForm = document.getElementById('newPasswordForm');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const toggleButtons = document.querySelectorAll('.toggle-password-visibility');

    // Check URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const email = urlParams.get('email');
    const token = urlParams.get('token');
    if (urlParams.has('error')) {
        showNotification(urlParams.get('error'), 'error');
        newPasswordInput.focus();
    }

    // Toggle password visibility
    toggleButtons.forEach(button => {
        button.addEventListener('click', function() {
            const input = this.previousElementSibling;
            const icon = this.querySelector('i');
            if (input.type === 'password') {
                input.type = 'text';
                icon.classList.replace('fa-eye', 'fa-eye-slash');
            } else {
                input.type = 'password';
                icon.classList.replace('fa-eye-slash', 'fa-eye');
            }
        });
    });

    // Form submission
    if (newPasswordForm) {
        newPasswordForm.addEventListener('submit', function(event) {
            event.preventDefault();
            clearValidationStates();

            // Validate new password
            const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/;
            if (!newPasswordInput.value.trim()) {
                setInvalid(newPasswordInput, 'New password is required');
                newPasswordInput.focus();
                return;
            } else if (!passwordRegex.test(newPasswordInput.value)) {
                setInvalid(newPasswordInput, 'Password must be at least 6 characters with one letter and one number');
                newPasswordInput.focus();
                return;
            } else {
                setValid(newPasswordInput);
            }

            // Validate confirm password
            if (newPasswordInput.value !== confirmPasswordInput.value) {
                setInvalid(confirmPasswordInput, 'Passwords do not match');
                confirmPasswordInput.focus();
                return;
            } else if (confirmPasswordInput.value) {
                setValid(confirmPasswordInput);
            }

            // Submit new password
            const submitBtn = newPasswordForm.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Saving...';
            submitBtn.disabled = true;

            fetch('/api/reset-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email, token: token, newPassword: newPasswordInput.value })
            })
            .then(response => response.json())
            .then(data => {
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
                if (data.success) {
                    showNotification('Password reset successfully!', 'success');
                    window.location.href = '/login?success=Password reset successfully';
                } else {
                    showNotification(data.error || 'Error resetting password.', 'error');
                    setInvalid(newPasswordInput, data.error || 'Invalid password');
                }
            })
            .catch(error => {
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
                showNotification('Error resetting password.', 'error');
                console.error('Error:', error);
            });
        });

        // Real-time new password validation
        newPasswordInput.addEventListener('input', function() {
            clearValidation(this);
            const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/;
            if (!this.value.trim()) {
                setInvalid(this, 'New password is required');
            } else if (!passwordRegex.test(this.value)) {
                setInvalid(this, 'Password must be at least 6 characters with one letter and one number');
            } else {
                setValid(this);
            }
        });

        // Real-time confirm password validation
        confirmPasswordInput.addEventListener('input', function() {
            clearValidation(this);
            if (this.value && this.value !== newPasswordInput.value) {
                setInvalid(this, 'Passwords do not match');
            } else if (this.value) {
                setValid(this);
            }
        });
    }

    /**
     * Set invalid state for an input
     * @param {HTMLElement} input - Input element
     * @param {string} message - Error message
     */
    function setInvalid(input, message) {
        input.classList.add('is-invalid');
        input.classList.remove('is-valid');
        const feedback = document.getElementById(`${input.id}Feedback`);
        if (feedback) {
            feedback.textContent = message;
            feedback.style.display = 'block';
        }
    }

    /**
     * Set valid state for an input
     * @param {HTMLElement} input - Input element
     */
    function setValid(input) {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
        const feedback = document.getElementById(`${input.id}Feedback`);
        if (feedback) {
            feedback.style.display = 'none';
        }
    }

    /**
     * Clear validation state
     * @param {HTMLElement} input - Input element
     */
    function clearValidation(input) {
        input.classList.remove('is-invalid', 'is-valid');
        const feedback = document.getElementById(`${input.id}Feedback`);
        if (feedback) {
            feedback.style.display = 'none';
        }
    }

    /**
     * Clear all validation states
     */
    function clearValidationStates() {
        [newPasswordInput, confirmPasswordInput].forEach(clearValidation);
    }

    /**
     * Show notification
     * @param {string} message - Message to display
     * @param {string} type - 'success' or 'error'
     */
    function showNotification(message, type) {
        const notification = document.createElement('div');
        notification.className = `alert alert-${type} text-center mb-3`;
        notification.innerHTML = `<i class="fas fa-${type === 'success' ? 'check' : 'exclamation'}-circle me-2"></i> ${message}`;
        document.querySelector('.auth-container').prepend(notification);
        setTimeout(() => notification.remove(), 5000);
    }
});
