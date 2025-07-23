/**
 * seller.js
 * Core functionality for the seller dashboard
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Initialize popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function(popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    // Handle sidebar toggle on mobile
    const sidebarToggle = document.querySelector('.sidebar-toggle');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function() {
            document.querySelector('.seller-sidebar').classList.toggle('show');
        });
    }

    // Active link highlighting for current page
    const currentPageUrl = window.location.pathname;
    const sidebarLinks = document.querySelectorAll('.seller-sidebar .nav-link');
    sidebarLinks.forEach(link => {
        const linkUrl = link.getAttribute('href');
        if (currentPageUrl === linkUrl || 
            (linkUrl !== '/seller' && currentPageUrl.startsWith(linkUrl))) {
            link.classList.add('active');
            
            // If it's in a dropdown, expand the dropdown
            const dropdownParent = link.closest('.nav-item-dropdown');
            if (dropdownParent) {
                dropdownParent.querySelector('.dropdown-toggle').classList.add('active');
                dropdownParent.querySelector('.dropdown-menu').classList.add('show');
            }
        }
    });

    // Handling dropdown toggles in sidebar
    const dropdownToggles = document.querySelectorAll('.seller-sidebar .dropdown-toggle');
    dropdownToggles.forEach(toggle => {
        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            const dropdownMenu = this.nextElementSibling;
            dropdownMenu.classList.toggle('show');
            this.querySelector('.dropdown-arrow').classList.toggle('rotate');
        });
    });

    // Mobile version of the sidebar
    const mobileMenuToggle = document.querySelector('.mobile-menu-toggle');
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', function() {
            document.querySelector('.seller-sidebar').classList.toggle('mobile-show');
            document.querySelector('.sidebar-overlay').classList.toggle('show');
        });

        const sidebarOverlay = document.querySelector('.sidebar-overlay');
        if (sidebarOverlay) {
            sidebarOverlay.addEventListener('click', function() {
                document.querySelector('.seller-sidebar').classList.remove('mobile-show');
                this.classList.remove('show');
            });
        }
    }

    // Close alert messages
    const closeButtons = document.querySelectorAll('.alert .btn-close');
    closeButtons.forEach(button => {
        button.addEventListener('click', function() {
            this.parentElement.classList.add('fade');
            setTimeout(() => {
                this.parentElement.style.display = 'none';
            }, 150);
        });
    });

    // Table row highlighting on hover
    const tableRows = document.querySelectorAll('.table tbody tr');
    tableRows.forEach(row => {
        row.addEventListener('mouseover', function() {
            this.classList.add('row-hover');
        });
        row.addEventListener('mouseout', function() {
            this.classList.remove('row-hover');
        });
    });

    // Format currency values in table cells with currency-value class
    const currencyValues = document.querySelectorAll('.currency-value');
    currencyValues.forEach(value => {
        const amount = parseFloat(value.textContent.replace(/[^\d.-]/g, ''));
        if (!isNaN(amount)) {
            value.textContent = new Intl.NumberFormat('vi-VN', {
                style: 'decimal',
                maximumFractionDigits: 0
            }).format(amount) + ' đ';
        }
    });

    // Format dates in table cells with date-value class
    const dateValues = document.querySelectorAll('.date-value');
    dateValues.forEach(value => {
        const dateStr = value.textContent.trim();
        if (dateStr && dateStr !== 'N/A') {
            const date = new Date(dateStr);
            if (!isNaN(date.getTime())) {
                value.textContent = new Intl.DateTimeFormat('en-US', { 
                    year: 'numeric',
                    month: 'short', 
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                }).format(date);
            }
        }
    });

    // Handle confirmation dialogs
    const confirmButtons = document.querySelectorAll('[data-confirm]');
    confirmButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            const message = this.getAttribute('data-confirm');
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });
}); 