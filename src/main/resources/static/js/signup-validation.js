/**
 * Client-side validation for the buyer signup form
 * Provides immediate feedback to users before form submission
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
        
        // Form submission validation
        form.addEventListener('submit', function(event) {
            let isValid = true;
            
            // Reset all validation states
            resetValidationState();
            
            // Validate full name
            if (!fullNameInput.value.trim() || fullNameInput.value.length < 2) {
                setInvalid(fullNameInput, 'Name must be at least 2 characters');
                isValid = false;
            }
            
            // Validate email
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(emailInput.value.trim())) {
                setInvalid(emailInput, 'Please enter a valid email address');
                isValid = false;
            }
            
            // Validate password
            const passwordRegex = /^(?=.*[0-9])(?=.*[a-zA-Z]).*$/;
            if (passwordInput.value.length < 6 || !passwordRegex.test(passwordInput.value)) {
                setInvalid(passwordInput, 'Password must be at least 6 characters with letters and numbers');
                isValid = false;
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
            
            // Validate date of birth
            if (!dobInput.value) {
                setInvalid(dobInput, 'Date of birth is required');
                isValid = false;
            }
            
            // Prevent form submission if validation fails
            if (!isValid) {
                event.preventDefault();
            }
        });
        
        // Real-time validation for password match
        confirmPasswordInput.addEventListener('input', function() {
            if (passwordInput.value !== confirmPasswordInput.value) {
                setInvalid(confirmPasswordInput, 'Passwords do not match');
            } else {
                setValid(confirmPasswordInput);
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
    }
});
