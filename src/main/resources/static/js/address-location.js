/**
 * Address location API functionality for Vietnamese provinces, districts, and wards
 * Using a proxy endpoint to avoid CORS issues
 */
const locationApiHost = "/api/location/";

/**
 * Fetch provinces data from API
 */
function fetchProvinces() {
    return axios.get(locationApiHost + 'provinces?depth=1')
        .then((response) => {
            renderLocationData(response.data, "province");
        })
        .catch(error => {
            console.error("Error fetching provinces:", error);
        });
}

/**
 * Fetch districts data for selected province
 * @param {string} provinceCode - The code of the selected province
 */
function fetchDistricts(provinceCode) {
    return axios.get(locationApiHost + "provinces/" + provinceCode + "?depth=2")
        .then((response) => {
            renderLocationData(response.data.districts, "district");
        })
        .catch(error => {
            console.error("Error fetching districts:", error);
        });
}

/**
 * Fetch wards data for selected district
 * @param {string} districtCode - The code of the selected district
 */
function fetchWards(districtCode) {
    return axios.get(locationApiHost + "districts/" + districtCode + "?depth=2")
        .then((response) => {
            renderLocationData(response.data.wards, "ward");
        })
        .catch(error => {
            console.error("Error fetching wards:", error);
        });
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
    
    // Create options for each location item
    let optionsHtml = firstOption;
    data.forEach(item => {
        optionsHtml += `<option value="${item.code}" data-name="${item.name}">${item.name}</option>`;
    });
    
    select.innerHTML = optionsHtml;
    
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
    initLocationSelectors();
});
