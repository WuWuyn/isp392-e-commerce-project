document.addEventListener('DOMContentLoaded', function () {
    // Search Icon Toggle for Desktop and Mobile
    const searchIconToggle = document.getElementById('searchIconToggle');
    const searchFormDesktop = document.getElementById('searchFormDesktop');
    const searchFormMobileContainer = document.getElementById('searchFormMobileContainer');
    const mainNavLinks = document.querySelector('.main-navbar .navbar-nav.mx-auto');

    if (searchIconToggle) {
        searchIconToggle.addEventListener('click', function (e) {
            e.preventDefault();

            // Desktop search toggle
            if (window.innerWidth >= 992) { // lg breakpoint
                if (searchFormDesktop) {
                    searchFormDesktop.classList.toggle('d-none');
                    searchFormDesktop.classList.toggle('d-lg-flex');
                    if (searchFormDesktop.classList.contains('d-lg-flex')) {
                        searchFormDesktop.querySelector('input').focus();
                        // Optionally hide central nav links to make space
                        if (mainNavLinks) mainNavLinks.classList.add('d-none');
                    } else {
                        if (mainNavLinks) mainNavLinks.classList.remove('d-none');
                    }
                }
            } else { // Mobile/Tablet search toggle
                if (searchFormMobileContainer) {
                    if (searchFormMobileContainer.style.display === 'none' || searchFormMobileContainer.style.display === '') {
                        searchFormMobileContainer.style.display = 'block';
                        searchFormMobileContainer.querySelector('input').focus();
                    } else {
                        searchFormMobileContainer.style.display = 'none';
                    }
                }
            }
        });
    }

    // Close search if clicked outside (simple version for desktop)
    document.addEventListener('click', function(event) {
        if (window.innerWidth >= 992 && searchFormDesktop && searchFormDesktop.classList.contains('d-lg-flex')) {
            const isClickInsideSearchForm = searchFormDesktop.contains(event.target);
            const isClickOnSearchIcon = searchIconToggle.contains(event.target);

            if (!isClickInsideSearchForm && !isClickOnSearchIcon) {
                searchFormDesktop.classList.add('d-none');
                searchFormDesktop.classList.remove('d-lg-flex');
                if (mainNavLinks) mainNavLinks.classList.remove('d-none');
            }
        }
    });

});