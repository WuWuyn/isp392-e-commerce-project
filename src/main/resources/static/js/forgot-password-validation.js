/**
 * Client-side validation for forgot-password.html
 */
document.addEventListener('DOMContentLoaded', function() {
    const forgotPasswordForm = document.querySelector('form[method="post"]');
    const emailInput = document.getElementById('email');

    // Check URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        showNotification(urlParams.get('error'), 'error');
        setInvalid(emailInput, 'Please check your email');
        emailInput.focus();
    }

    if (forgotPasswordForm) {
        forgotPasswordForm.addEventListener('submit', function(event) {
            event.preventDefault();
            clearValidation(emailInput);

            // Validate email
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailInput.value.trim()) {
                setInvalid(emailInput, 'Email is required');
                emailInput.focus();
                return;
            } else if (!emailRegex.test(emailInput.value)) {
                setInvalid(emailInput, 'Please enter a valid email address');
                emailInput.focus();
                return;
            }

            // Send OTP
            const submitBtn = forgotPasswordForm.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Sending...';
            submitBtn.disabled = true;

            fetch('/api/send-otp', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: emailInput.value })
            })
            .then(response => response.json())
            .then(data => {
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
                if (data.success) {
                    showNotification('OTP sent to your email!', 'success');
                    window.location.href = `/otp?email=${encodeURIComponent(emailInput.value)}`;
                } else {
                    showNotification(data.error || 'Failed to send OTP.', 'error');
                    setInvalid(emailInput, data.error || 'Invalid email address');
                }
            })
            .catch(error => {
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
                showNotification('Error sending OTP.', 'error');
                console.error('Error:', error);
            });
        });

        // Real-time email validation
        emailInput.addEventListener('input', function() {
            clearValidation(this);
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!this.value.trim()) {
                setInvalid(this, 'Email is required');
            } else if (!emailRegex.test(this.value)) {
                setInvalid(this, 'Please enter a valid email address');
            } else {
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
        const feedback = input.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.textContent = message;
            feedback.style.display = 'block';
        } else {
            const newFeedback = document.createElement('div');
            newFeedback.className = 'invalid-feedback';
            newFeedback.textContent = message;
            input.parentNode.appendChild(newFeedback);
        }
    }

    /**
     * Set valid state for an input
     * @param {HTMLElement} input - Input element
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