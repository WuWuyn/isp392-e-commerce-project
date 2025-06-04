/**
 * Client-side validation for otp.html
 */
document.addEventListener('DOMContentLoaded', function() {
    const otpForm = document.getElementById('otpForm');
    const otpInputs = document.querySelectorAll('.otp-input');
    const fullOtpInput = document.getElementById('fullOtp');
    const resendOtpLink = document.getElementById('resendOtpLink');
    const resendTimerDisplay = document.getElementById('resendTimer');
    let resendCooldown = 30;

    // Check URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const email = urlParams.get('email');
    if (urlParams.has('error')) {
        showNotification(urlParams.get('error'), 'error');
        otpInputs[0].focus();
    }

    // OTP input handling
    otpInputs.forEach((input, index) => {
        input.addEventListener('input', () => {
            if (input.value.length === 1 && index < otpInputs.length - 1) {
                otpInputs[index + 1].focus();
            }
            validateOtpInputs();
        });

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Backspace' && input.value === '' && index > 0) {
                otpInputs[index - 1].focus();
            }
        });

        input.addEventListener('keypress', (e) => {
            if (!/[0-9]/.test(e.key)) {
                e.preventDefault();
            }
        });
    });

    // Form submission
    if (otpForm) {
        otpForm.addEventListener('submit', function(event) {
            event.preventDefault();
            let otpValue = '';
            otpInputs.forEach(input => otpValue += input.value);
            fullOtpInput.value = otpValue;

            if (otpValue.length !== 6) {
                showNotification('Please enter a valid 6-digit OTP', 'error');
                otpInputs[0].focus();
                return;
            }

            const submitBtn = otpForm.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Verifying...';
            submitBtn.disabled = true;

            fetch('/api/verify-otp', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email, otp: otpValue })
            })
            .then(response => response.json())
            .then(data => {
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
                if (data.success) {
                    showNotification('OTP verified successfully!', 'success');
                    window.location.href = `/new-password?email=${encodeURIComponent(email)}&token=${data.token}`;
                } else {
                    showNotification(data.error || 'Invalid or expired OTP', 'error');
                    otpInputs.forEach(input => input.classList.add('is-invalid'));
                    otpInputs[0].focus();
                }
            })
            .catch(error => {
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
                showNotification('Error verifying OTP.', 'error');
                console.error('Error:', error);
            });
        });
    }

    // Resend OTP
    if (resendOtpLink) {
        resendOtpLink.addEventListener('click', function(e) {
            e.preventDefault();
            fetch('/api/send-otp', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showNotification('OTP resent successfully!', 'success');
                    startResendTimer();
                } else {
                    showNotification(data.error || 'Failed to resend OTP.', 'error');
                }
            })
            .catch(error => {
                showNotification('Error resending OTP.', 'error');
                console.error('Error:', error);
            });
        });
        startResendTimer();
    }

    /**
     * Start resend OTP timer
     */
    function startResendTimer() {
        resendOtpLink.style.pointerEvents = 'none';
        resendOtpLink.style.opacity = '0.6';
        resendTimerDisplay.style.display = 'inline';
        let timer = resendCooldown;
        const interval = setInterval(() => {
            timer--;
            resendTimerDisplay.textContent = `(0:${timer < 10 ? '0' : ''}${timer})`;
            if (timer <= 0) {
                clearInterval(interval);
                resendOtpLink.style.pointerEvents = 'auto';
                resendOtpLink.style.opacity = '1';
                resendTimerDisplay.style.display = 'none';
            }
        }, 1000);
    }

    /**
     * Validate OTP inputs
     */
    function validateOtpInputs() {
        let isValid = true;
        otpInputs.forEach(input => {
            if (!input.value.match(/[0-9]/)) {
                isValid = false;
            }
        });
        otpInputs.forEach(input => {
            if (isValid) {
                input.classList.remove('is-invalid');
            } else {
                input.classList.add('is-invalid');
            }
        });
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
        document.querySelector('.otp-container').prepend(notification);
        setTimeout(() => notification.remove(), 5000);
    }
});