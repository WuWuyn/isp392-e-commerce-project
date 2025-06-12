/**
 * Address location API functionality for Vietnamese provinces, districts, and wards
 * Using a proxy endpoint to avoid CORS issues
 */
const locationApiHost = "/api/location/";
let fetchAttempts = 0;
const maxAttempts = 3;
const retryDelay = 1000; // 1 second delay between retries

/**
 * Fetch provinces data from API with retry logic
 */
function fetchProvinces() {
    fetchAttempts++;
    
    return axios.get(locationApiHost + 'provinces?depth=1')
        .then((response) => {
            fetchAttempts = 0; // Reset attempt counter on success
            renderLocationData(response.data, "province");
        })
        .catch(error => {
            console.error("Error fetching provinces:", error);
            
            // Show error message to user
            const provinceSelect = document.getElementById('province');
            if (provinceSelect) {
                provinceSelect.innerHTML = '<option value="">Error loading data</option>';
                
                // Retry logic
                if (fetchAttempts < maxAttempts) {
                    console.log(`Retrying provinces fetch... Attempt ${fetchAttempts} of ${maxAttempts}`);
                    setTimeout(fetchProvinces, retryDelay);
                } else {
                    showLocationApiError();
                }
            }
        });
}

/**
 * Fetch districts data for selected province with retry logic
 * @param {string} provinceCode - The code of the selected province
 */
function fetchDistricts(provinceCode) {
    const districtSelect = document.getElementById('district');
    if (!districtSelect) return;
    
    districtSelect.innerHTML = '<option value="">Loading districts...</option>';
    
    return axios.get(locationApiHost + "provinces/" + provinceCode + "?depth=2")
        .then((response) => {
            renderLocationData(response.data.districts, "district");
        })
        .catch(error => {
            console.error("Error fetching districts:", error);
            districtSelect.innerHTML = '<option value="">Error loading districts</option>';
            
            // Show an alert for district loading error
            districtSelect.classList.add('is-invalid');
            const feedbackElement = document.createElement('div');
            feedbackElement.className = 'invalid-feedback';
            feedbackElement.textContent = 'Failed to load districts. Please try again.';
            districtSelect.parentNode.appendChild(feedbackElement);
        });
}

/**
 * Fetch wards data for selected district with retry logic
 * @param {string} districtCode - The code of the selected district
 */
function fetchWards(districtCode) {
    const wardSelect = document.getElementById('ward');
    if (!wardSelect) return;
    
    wardSelect.innerHTML = '<option value="">Loading wards...</option>';
    
    return axios.get(locationApiHost + "districts/" + districtCode + "?depth=2")
        .then((response) => {
            renderLocationData(response.data.wards, "ward");
        })
        .catch(error => {
            console.error("Error fetching wards:", error);
            wardSelect.innerHTML = '<option value="">Error loading wards</option>';
            
            // Show an alert for ward loading error
            wardSelect.classList.add('is-invalid');
            const feedbackElement = document.createElement('div');
            feedbackElement.className = 'invalid-feedback';
            feedbackElement.textContent = 'Failed to load wards. Please try again.';
            wardSelect.parentNode.appendChild(feedbackElement);
        });
}

/**
 * Show error message when location API fails
 */
function showLocationApiError() {
    const provinceContainer = document.getElementById('province').parentElement;
    if (provinceContainer) {
        const alertElement = document.createElement('div');
        alertElement.className = 'alert alert-warning mt-2';
        alertElement.innerHTML = '<i class="fas fa-exclamation-triangle"></i> ' + 
            'Failed to load location data. Please refresh the page or try again later.';
        provinceContainer.appendChild(alertElement);
    }
}

/**
 * Render location data to select dropdown
 * @param {Array} data - The location data array
 * @param {string} selectId - The ID of the select element to populate
 */
function renderLocationData(data, selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    
    // Keep the first option (placeholder)
    let firstOption = '';
    if (select.options.length > 0) {
        firstOption = select.options[0].outerHTML;
    }
    
    // Check if data is valid array
    if (!Array.isArray(data) || data.length === 0) {
        select.innerHTML = `<option value="">No data available</option>`;
        return;
    }
    
    // Create options for each location item
    let optionsHtml = firstOption;
    data.forEach(item => {
        if (item && item.code && item.name) {
            optionsHtml += `<option value="${item.code}" data-name="${item.name}">${item.name}</option>`;
        }
    });
    
    select.innerHTML = optionsHtml;
    
    // Remove any validation error styling
    select.classList.remove('is-invalid');
    
    // Trigger change event to update validation state
    const event = new Event('change');
    select.dispatchEvent(event);
}

/**
 * Initialize location selectors with event listeners
 */
function initLocationSelectors() {
    const provinceSelect = document.getElementById('province');
    const districtSelect = document.getElementById('district');
    const wardSelect = document.getElementById('ward');
    
    // Check if elements exist
    if (!provinceSelect) {
        console.error("Province select element not found");
        return;
    }
    
    // Show loading state
    provinceSelect.innerHTML = '<option value="">Loading provinces...</option>';
    
    // Fetch provinces on page load
    fetchProvinces();
    
    // Set up event listeners for cascading selection
    if (provinceSelect) {
        provinceSelect.addEventListener('change', function() {
            // Reset dependent dropdowns
            if (districtSelect) {
                districtSelect.innerHTML = '<option value="">Select district</option>';
                districtSelect.disabled = this.value === '';
                
                if (wardSelect) {
                    wardSelect.innerHTML = '<option value="">Select ward</option>';
                    wardSelect.disabled = true;
                }
            }
            
            // Fetch districts for selected province
            if (this.value) {
                fetchDistricts(this.value);
                
                // Update hidden inputs with text value
                const provinceNameInput = document.getElementById('provinceName');
                if (provinceNameInput) {
                    const selectedOption = this.options[this.selectedIndex];
                    provinceNameInput.value = selectedOption.text;
                }
            }
        });
    }
    
    if (districtSelect) {
        districtSelect.addEventListener('change', function() {
            // Reset ward dropdown
            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Select ward</option>';
                wardSelect.disabled = this.value === '';
                
                // Make sure ward is marked as required
                if (wardSelect.hasAttribute('required')) {
                    wardSelect.setAttribute('required', 'required');
                }
            }
            
            // Fetch wards for selected district
            if (this.value) {
                fetchWards(this.value);
                
                // Update hidden inputs with text value
                const districtNameInput = document.getElementById('districtName');
                if (districtNameInput) {
                    const selectedOption = this.options[this.selectedIndex];
                    districtNameInput.value = selectedOption.text;
                }
            }
        });
    }
    
    if (wardSelect) {
        wardSelect.addEventListener('change', function() {
            // Update hidden inputs with text value
            if (this.value) {
                const wardNameInput = document.getElementById('wardName');
                if (wardNameInput) {
                    const selectedOption = this.options[this.selectedIndex];
                    wardNameInput.value = selectedOption.text;
                }
            }
        });
    }
}

// Initialize the location selectors when the DOM is fully loaded
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(initLocationSelectors, 300); // Add a small delay to ensure all elements are properly loaded
});
