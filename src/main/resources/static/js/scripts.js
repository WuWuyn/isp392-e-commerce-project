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





    //----------------------------------------------------------------------------------------------------------
    //-----------------------------------------------Product List Page------------------------------------------
    // Add this inside your DOMContentLoaded event listener in script.js

    // Price Range Slider Display Update
    const priceRangeSlider = document.getElementById('priceRange');
    const minPriceDisplay = document.getElementById('minPriceDisplay');
    const maxPriceDisplay = document.getElementById('maxPriceDisplay'); // Assuming you might want to show current selected value, not just fixed max

    if (priceRangeSlider && minPriceDisplay && maxPriceDisplay) {
        // Initialize display
        minPriceDisplay.textContent = priceRangeSlider.value; // Or a separate min value if it's a dual slider
        // For a single HTML range, it usually represents one value.
        // If it's meant to be a min-value selector:
        // maxPriceDisplay.textContent remains the fixed max.

        priceRangeSlider.addEventListener('input', function() {
            minPriceDisplay.textContent = this.value;
            // If you adapt this to a dual-handle slider (requires a library or more complex HTML/JS),
            // you'd update both min and max displays.
        });
    }

    // Interactive book card hover (already handled by CSS, but JS could enhance)
    // The CSS solution for hover is generally preferred for simplicity if it meets needs.
    // If complex animations or actions are needed, JS would be used here.







    //-------------------------------------------------------------------------------
    //-------------------------------Product Detail----------------------------------
    // Add this inside your DOMContentLoaded event listener in script.js

    // Quantity Selector for Product Detail
    const quantityInput = document.getElementById('quantityInput');
    if (quantityInput) {
        const quantityButtons = document.querySelectorAll('.quantity-btn');
        quantityButtons.forEach(button => {
            button.addEventListener('click', function() {
                let currentValue = parseInt(quantityInput.value);
                const action = this.dataset.action;

                if (action === 'increase') {
                    quantityInput.value = currentValue + 1;
                } else if (action === 'decrease' && currentValue > 1) {
                    quantityInput.value = currentValue - 1;
                }
            });
        });
        // Prevent non-numeric input if needed, though type="number" helps
        quantityInput.addEventListener('change', function() {
            if (parseInt(this.value) < 1 || isNaN(parseInt(this.value))) {
                this.value = 1;
            }
        });
    }

    // Interactive Star Rating for "Add a Review" form
    // The CSS handles the visual selection, JS might be used for value setting if needed
    // or for more complex interactions. The current CSS setup is common for this.
    // If you need to get the value in JS:
    const reviewStarRadios = document.querySelectorAll('.add-review-stars input[type="radio"]');
    reviewStarRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                // console.log('Selected rating:', this.value);
                // You can use this.value for form submission
            }
        });
    });

    // Placeholder for review filtering and pagination logic
    // In a real application, these would likely trigger backend calls or page reloads
    // with new query parameters. For a static demo:
    const reviewSortSelect = document.querySelector('.review-filters select[aria-label="Sort reviews"]');
    if (reviewSortSelect) {
        reviewSortSelect.addEventListener('change', function() {
            console.log('Sort reviews by:', this.value);
            // Add logic to re-order or re-fetch reviews
        });
    }
    const reviewStarFilterSelect = document.querySelector('.review-filters select[aria-label="Filter by stars"]');
    if (reviewStarFilterSelect) {
        reviewStarFilterSelect.addEventListener('change', function() {
            console.log('Filter reviews by stars:', this.value);
            // Add logic to filter or re-fetch reviews
        });
    }

    // Image Zoom Modal - ensure image src is set if dynamic
    const imageZoomModal = document.getElementById('imageZoomModal')
    if (imageZoomModal) {
        imageZoomModal.addEventListener('show.bs.modal', function (event) {
            // If you have multiple product images and thumbnails,
            // you might want to update the modal's image source dynamically here.
            // For this example, it uses the same static image as the main one.
            // const button = event.relatedTarget // Button that triggered the modal
            // const mainImageSrc = document.getElementById('mainProductImage').src;
            // const modalImage = imageZoomModal.querySelector('.modal-body img');
            // modalImage.src = mainImageSrc;
        });
    }


    //--------------------------------------------------------------------------------------
    //----------------------------------Blog Page-------------------------------------------
    // Add this inside your DOMContentLoaded event listener in script.js

    // Blog Page: Search and Filter Placeholders
    const blogSearchForm = document.querySelector('.blog-search-form');
    if (blogSearchForm) {
        blogSearchForm.addEventListener('submit', function(event) {
            event.preventDefault(); // Prevent actual form submission for demo
            const searchTerm = this.querySelector('input[type="search"]').value;
            console.log('Searching blog for:', searchTerm);
            // Add AJAX call or page reload logic here
            // alert('Search functionality would be implemented here for: ' + searchTerm);
        });
    }

    const blogFilterSelect = document.querySelector('.blog-filters-search select[aria-label="Filter posts"]');
    if (blogFilterSelect) {
        blogFilterSelect.addEventListener('change', function() {
            const filterValue = this.value;
            console.log('Filtering blog posts by:', filterValue);
            // Add AJAX call or page reload logic here
            // alert('Filter functionality would be implemented here for: ' + filterValue);
        });
    }

});