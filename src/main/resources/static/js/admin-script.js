document.addEventListener('DOMContentLoaded', function () {
    // Admin Sidebar Toggle for mobile/tablet
    const sidebarToggle = document.getElementById('adminSidebarToggle');
    const adminSidebar = document.querySelector('.admin-sidebar');
    const adminMainContent = document.querySelector('.admin-main-content');

    if (sidebarToggle && adminSidebar && adminMainContent) {
        sidebarToggle.addEventListener('click', function () {
            adminSidebar.classList.toggle('active');
            adminMainContent.classList.toggle('sidebar-active'); // Optional: to push content
        });
    }

    // Update Topbar Page Title based on Sidebar Active Link
    function updateTopbarTitle() {
        const activeNavLink = document.querySelector('.admin-nav-menu .nav-link.active');
        const topbarTitleEl = document.querySelector('.admin-topbar .page-title-placeholder');
        let title = "Dashboard"; // Default title

        if (activeNavLink) {
            // Try to get text directly from the link, removing icon and badge
            let linkText = activeNavLink.innerHTML;
            const iconMatch = linkText.match(/<i class="[^"]+"><\/i>/);
            if (iconMatch) linkText = linkText.replace(iconMatch[0], "").trim();
            const badgeMatch = linkText.match(/<span class="badge[^"]+">.*?<\/span>/);
            if (badgeMatch) linkText = linkText.replace(badgeMatch[0], "").trim();
            const chevronMatch = linkText.match(/<i class="fas fa-chevron-down[^"]*"><\/i>/);
            if (chevronMatch) linkText = linkText.replace(chevronMatch[0], "").trim();


            // If it's a sub-link, try to get parent's text too for context if desired
            if(activeNavLink.classList.contains('sub-link')){
                const parentCollapse = activeNavLink.closest('.collapse');
                if(parentCollapse){
                    const parentLink = document.querySelector(`a[href="#${parentCollapse.id}"]`);
                    if(parentLink) {
                        let parentText = parentLink.innerHTML;
                        const parentIconMatch = parentText.match(/<i class="[^"]+"><\/i>/);
                        if (parentIconMatch) parentText = parentText.replace(parentIconMatch[0], "").trim();
                        const parentChevronMatch = parentText.match(/<i class="fas fa-chevron-down[^"]*"><\/i>/);
                        if (parentChevronMatch) parentText = parentText.replace(parentChevronMatch[0], "").trim();

                        title = parentText.trim() + " > " + linkText.trim();
                    } else {
                        title = linkText.trim();
                    }
                } else {
                    title = linkText.trim();
                }
            } else {
                title = linkText.trim();
            }
        }

        if (topbarTitleEl) {
            topbarTitleEl.textContent = title;
        }
    }

    updateTopbarTitle(); // Call on page load

    // Re-update title if a nav link is clicked (for SPA-like behavior if you add that later)
    const allNavLinks = document.querySelectorAll('.admin-nav-menu .nav-link');
    allNavLinks.forEach(link => {
        link.addEventListener('click', function() {
            // This is a bit simplistic for non-SPA. For full SPA, you'd update on route change.
            // For now, it will update based on the clicked link *before* page navigation.
            // Timeout to allow active class to potentially update if done by other JS
            setTimeout(updateTopbarTitle, 50);
        });
    });


    // "Select All" checkbox for product table
    const selectAllProductsCheckbox = document.getElementById('selectAllProducts');
    if (selectAllProductsCheckbox) {
        selectAllProductsCheckbox.addEventListener('change', function() {
            const productCheckboxes = document.querySelectorAll('.admin-table tbody .form-check-input');
            productCheckboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
        });
    }

});