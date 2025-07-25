// Inventory Management JavaScript

document.addEventListener('DOMContentLoaded', function() {
    initializeInventoryManagement();
});

function initializeInventoryManagement() {
    // Initialize event listeners
    initializeStockInputs();
    initializeCheckboxes();
    initializeBulkUpdate();
    initializeFilters();
    initializeRefresh();
}

// Stock input management
function initializeStockInputs() {
    const stockInputs = document.querySelectorAll('.stock-input');
    
    stockInputs.forEach(input => {
        const originalValue = input.dataset.original;
        
        input.addEventListener('input', function() {
            const bookId = this.dataset.bookId;
            const currentValue = this.value;
            const updateBtn = document.querySelector(`.update-stock-btn[data-book-id="${bookId}"]`);
            const resetBtn = document.querySelector(`.reset-stock-btn[data-book-id="${bookId}"]`);
            
            // Validate input
            if (currentValue < 0) {
                this.classList.add('error');
                this.classList.remove('changed');
                updateBtn.classList.remove('enabled');
                resetBtn.classList.remove('enabled');
                return;
            } else {
                this.classList.remove('error');
            }
            
            // Check if value changed
            if (currentValue !== originalValue) {
                this.classList.add('changed');
                updateBtn.classList.add('enabled');
                resetBtn.classList.add('enabled');
            } else {
                this.classList.remove('changed');
                updateBtn.classList.remove('enabled');
                resetBtn.classList.remove('enabled');
            }
        });
        
        // Handle Enter key
        input.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                const bookId = this.dataset.bookId;
                updateSingleStock(bookId);
            }
        });
    });
    
    // Update stock buttons
    document.querySelectorAll('.update-stock-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const bookId = this.dataset.bookId;
            updateSingleStock(bookId);
        });
    });
    
    // Reset stock buttons
    document.querySelectorAll('.reset-stock-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const bookId = this.dataset.bookId;
            const originalValue = this.dataset.original;
            const input = document.querySelector(`.stock-input[data-book-id="${bookId}"]`);
            
            input.value = originalValue;
            input.classList.remove('changed', 'error');
            this.classList.remove('enabled');
            document.querySelector(`.update-stock-btn[data-book-id="${bookId}"]`).classList.remove('enabled');
        });
    });
}

// Checkbox management
function initializeCheckboxes() {
    const headerCheckbox = document.getElementById('headerCheckbox');
    const selectAllCheckbox = document.getElementById('selectAll');
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    
    // Header checkbox functionality
    if (headerCheckbox) {
        headerCheckbox.addEventListener('change', function() {
            productCheckboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
            updateBulkUpdateButton();
        });
    }
    
    // Select all checkbox functionality
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            productCheckboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
            if (headerCheckbox) {
                headerCheckbox.checked = this.checked;
            }
            updateBulkUpdateButton();
        });
    }
    
    // Individual checkbox functionality
    productCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateHeaderCheckbox();
            updateBulkUpdateButton();
        });
    });
}

function updateHeaderCheckbox() {
    const headerCheckbox = document.getElementById('headerCheckbox');
    const selectAllCheckbox = document.getElementById('selectAll');
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    const checkedBoxes = document.querySelectorAll('.product-checkbox:checked');
    
    if (headerCheckbox) {
        headerCheckbox.checked = checkedBoxes.length === productCheckboxes.length;
        headerCheckbox.indeterminate = checkedBoxes.length > 0 && checkedBoxes.length < productCheckboxes.length;
    }
    
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = checkedBoxes.length === productCheckboxes.length;
        selectAllCheckbox.indeterminate = checkedBoxes.length > 0 && checkedBoxes.length < productCheckboxes.length;
    }
}

function updateBulkUpdateButton() {
    const bulkUpdateBtn = document.getElementById('bulkUpdateBtn');
    const checkedBoxes = document.querySelectorAll('.product-checkbox:checked');
    
    if (bulkUpdateBtn) {
        bulkUpdateBtn.disabled = checkedBoxes.length === 0;
        bulkUpdateBtn.innerHTML = checkedBoxes.length > 0 
            ? `<i class="fas fa-edit"></i> Bulk Update (${checkedBoxes.length})`
            : '<i class="fas fa-edit"></i> Bulk Update';
    }
}

// Bulk update functionality
function initializeBulkUpdate() {
    const bulkUpdateBtn = document.getElementById('bulkUpdateBtn');
    const bulkUpdateModal = new bootstrap.Modal(document.getElementById('bulkUpdateModal'));
    const confirmBtn = document.getElementById('confirmBulkUpdate');
    
    if (bulkUpdateBtn) {
        bulkUpdateBtn.addEventListener('click', function() {
            const checkedBoxes = document.querySelectorAll('.product-checkbox:checked');
            if (checkedBoxes.length === 0) {
                showAlert('Please select at least one product to update.', 'warning');
                return;
            }
            
            populateSelectedProducts();
            bulkUpdateModal.show();
        });
    }
    
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            performBulkUpdate();
        });
    }
}

function populateSelectedProducts() {
    const selectedProductsList = document.getElementById('selectedProductsList');
    const checkedBoxes = document.querySelectorAll('.product-checkbox:checked');
    
    selectedProductsList.innerHTML = '';
    
    checkedBoxes.forEach(checkbox => {
        const bookId = checkbox.value;
        const row = checkbox.closest('tr');
        const title = row.querySelector('.product-info h6').textContent;
        const currentStock = row.querySelector('.stock-input').value;
        
        const productItem = document.createElement('div');
        productItem.className = 'selected-product-item';
        productItem.innerHTML = `
            <div class="selected-product-info">
                <strong>${title}</strong>
                <br>
                <small class="text-muted">Current Stock: ${currentStock}</small>
            </div>
            <input type="number" class="form-control selected-product-stock" 
                   value="${currentStock}" min="0" data-book-id="${bookId}">
        `;
        
        selectedProductsList.appendChild(productItem);
    });
}

function performBulkUpdate() {
    const globalReason = document.getElementById('globalReason').value;
    const globalNotes = document.getElementById('globalNotes').value;
    const selectedProducts = document.querySelectorAll('.selected-product-stock');
    
    const inventoryUpdates = [];
    
    selectedProducts.forEach(input => {
        const bookId = parseInt(input.dataset.bookId);
        const stockQuantity = parseInt(input.value);
        
        if (!isNaN(bookId) && !isNaN(stockQuantity) && stockQuantity >= 0) {
            inventoryUpdates.push({
                bookId: bookId,
                stockQuantity: stockQuantity,
                reason: globalReason,
                notes: globalNotes
            });
        }
    });
    
    if (inventoryUpdates.length === 0) {
        showAlert('No valid updates to perform.', 'warning');
        return;
    }
    
    const bulkUpdateData = {
        inventoryUpdates: inventoryUpdates,
        globalReason: globalReason,
        globalNotes: globalNotes
    };
    
    // Show loading state
    const confirmBtn = document.getElementById('confirmBulkUpdate');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Updating...';
    confirmBtn.disabled = true;
    
    fetch('/seller/inventory/bulk-update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify(bulkUpdateData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showAlert(data.message, 'success');
            // Close modal and refresh page
            bootstrap.Modal.getInstance(document.getElementById('bulkUpdateModal')).hide();
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            showAlert(data.message, 'danger');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('An error occurred while updating stock.', 'danger');
    })
    .finally(() => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
    });
}

// Single stock update
function updateSingleStock(bookId) {
    const input = document.querySelector(`.stock-input[data-book-id="${bookId}"]`);
    const stockQuantity = parseInt(input.value);
    
    if (isNaN(stockQuantity) || stockQuantity < 0) {
        showAlert('Please enter a valid stock quantity.', 'warning');
        return;
    }
    
    const updateBtn = document.querySelector(`.update-stock-btn[data-book-id="${bookId}"]`);
    const originalText = updateBtn.innerHTML;
    updateBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    updateBtn.disabled = true;
    
    const formData = new FormData();
    formData.append('bookId', bookId);
    formData.append('stockQuantity', stockQuantity);
    formData.append('reason', 'MANUAL_UPDATE');
    formData.append('notes', 'Updated via inventory management');
    
    fetch('/seller/inventory/update-stock', {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showAlert(data.message, 'success');
            // Update the original value
            input.dataset.original = stockQuantity;
            input.classList.remove('changed');
            updateBtn.classList.remove('enabled');
            document.querySelector(`.reset-stock-btn[data-book-id="${bookId}"]`).classList.remove('enabled');
            
            // Animate the input
            input.classList.add('animate');
            setTimeout(() => {
                input.classList.remove('animate');
            }, 300);
        } else {
            showAlert(data.message, 'danger');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('An error occurred while updating stock.', 'danger');
    })
    .finally(() => {
        updateBtn.innerHTML = originalText;
        updateBtn.disabled = false;
    });
}

// Filter and sort functionality
function initializeFilters() {
    // These functions are called by the HTML onchange events
}

function applyFilter() {
    const stockFilter = document.getElementById('stockFilter').value;
    const currentUrl = new URL(window.location);
    currentUrl.searchParams.set('stockFilter', stockFilter);
    currentUrl.searchParams.set('page', '0'); // Reset to first page
    window.location.href = currentUrl.toString();
}

function applySort() {
    const sortField = document.getElementById('sortField').value;
    const currentUrl = new URL(window.location);
    currentUrl.searchParams.set('sortField', sortField);
    currentUrl.searchParams.set('page', '0'); // Reset to first page
    window.location.href = currentUrl.toString();
}

function toggleSortDirection() {
    const currentUrl = new URL(window.location);
    const currentDir = currentUrl.searchParams.get('sortDir') || 'asc';
    const newDir = currentDir === 'asc' ? 'desc' : 'asc';
    currentUrl.searchParams.set('sortDir', newDir);
    currentUrl.searchParams.set('page', '0'); // Reset to first page
    window.location.href = currentUrl.toString();
}

// Refresh functionality
function initializeRefresh() {
    const refreshBtn = document.getElementById('refreshBtn');
    
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            const icon = this.querySelector('i');
            icon.classList.add('fa-spin');
            
            setTimeout(() => {
                window.location.reload();
            }, 500);
        });
    }
}

// Utility functions
function showAlert(message, type) {
    // Remove existing alerts
    const existingAlerts = document.querySelectorAll('.alert');
    existingAlerts.forEach(alert => alert.remove());
    
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    // Insert at the top of main content
    const mainContent = document.querySelector('main');
    mainContent.insertBefore(alertDiv, mainContent.firstChild);
    
    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 5000);
}
