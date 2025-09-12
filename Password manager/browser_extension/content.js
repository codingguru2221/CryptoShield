// Content script for Pendrive Password Manager Extension

const SERVER_URL = 'http://127.0.0.1:5000';

// Listen for messages from popup or background script
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'fillPassword') {
        fillPasswordFields(request.password);
        sendResponse({ success: true });
    } else if (request.action === 'getFormFields') {
        const fields = getFormFields();
        sendResponse({ fields: fields });
    } else if (request.type === 'CONNECTION_LOST') {
        // Clear any cached data and remove UI elements immediately on disconnect
        try {
            if (window.pendrivePasswordData) {
                window.pendrivePasswordData = null;
            }
            document.querySelectorAll('.pendrive-fill-btn').forEach(btn => btn.remove());
            showNotification('Connection lost. Cleared page helpers.', 'error');
        } catch (e) {}
    } else if (request.type === 'CONNECTION_RESTORED') {
        // Re-add buttons on reconnect
        try {
            addFillButtons();
            showNotification('Connection restored.', 'success');
        } catch (e) {}
    }
});

// Auto-detect login forms and add fill buttons
function addFillButtons() {
    // Remove existing buttons
    document.querySelectorAll('.pendrive-fill-btn').forEach(btn => btn.remove());
    
    // Find login forms
    const forms = document.querySelectorAll('form');
    
    forms.forEach(form => {
        const hasPasswordField = form.querySelector('input[type="password"]');
        const hasUsernameField = form.querySelector('input[type="email"], input[name*="user"], input[name*="email"]');
        
        if (hasPasswordField && hasUsernameField) {
            // Create fill button
            const fillBtn = document.createElement('button');
            fillBtn.className = 'pendrive-fill-btn';
            fillBtn.innerHTML = '🔐 Fill from Pendrive';
            fillBtn.style.cssText = `
                position: absolute;
                background: #4CAF50;
                color: white;
                border: none;
                padding: 5px 10px;
                border-radius: 3px;
                font-size: 12px;
                cursor: pointer;
                z-index: 1000;
                box-shadow: 0 2px 5px rgba(0,0,0,0.2);
            `;
            
            // Position button near the form
            const rect = form.getBoundingClientRect();
            fillBtn.style.left = (rect.right + 10) + 'px';
            fillBtn.style.top = rect.top + 'px';
            
            // Add click handler
            fillBtn.addEventListener('click', async (e) => {
                e.preventDefault();
                await autoFillForm(form);
            });
            
            document.body.appendChild(fillBtn);
        }
    });
}

// Auto-fill form with saved password
async function autoFillForm(form) {
    try {
        // First check if server and Java app are available
        const statusResponse = await fetch(`${SERVER_URL}/api/status`);
        const statusData = await statusResponse.json();
        
        if (!statusData.server_running || !statusData.pendrive_detected || !statusData.java_running) {
            showNotification('Cannot access passwords: Server disconnected or pendrive removed', 'error');
            return;
        }
        
        const url = new URL(window.location.href);
        const domain = url.hostname;
        
        // Get passwords for this website
        const response = await fetch(`${SERVER_URL}/api/passwords/${domain}`);
        const data = await response.json();
        
        if (data.passwords && data.passwords.length > 0) {
            const password = data.passwords[0];
            fillPasswordFields(password);
            
            // Show success notification
            showNotification('Password filled successfully!', 'success');
        } else {
            showNotification('No saved passwords found for this website', 'info');
        }
    } catch (error) {
        console.error('Error auto-filling form:', error);
        showNotification('Error: Cannot connect to password server', 'error');
    }
}

// Get form fields information
function getFormFields() {
    const fields = [];
    
    // Find all input fields
    const inputs = document.querySelectorAll('input');
    
    inputs.forEach(input => {
        if (input.type === 'password' || input.type === 'email' || 
            input.name?.toLowerCase().includes('user') ||
            input.name?.toLowerCase().includes('email') ||
            input.id?.toLowerCase().includes('user') ||
            input.id?.toLowerCase().includes('email')) {
            
            fields.push({
                type: input.type,
                name: input.name,
                id: input.id,
                placeholder: input.placeholder,
                value: input.value
            });
        }
    });
    
    return fields;
}

// Show notification
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'success' ? '#4CAF50' : type === 'error' ? '#F44336' : '#2196F3'};
        color: white;
        padding: 10px 20px;
        border-radius: 5px;
        z-index: 10000;
        font-family: Arial, sans-serif;
        font-size: 14px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.3);
        max-width: 300px;
        word-wrap: break-word;
    `;
    notification.textContent = message;
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Initialize content script
function initialize() {
    // Add fill buttons to existing forms
    addFillButtons();
    
    // Watch for dynamically added forms
    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            if (mutation.type === 'childList') {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        if (node.tagName === 'FORM' || node.querySelector('form')) {
                            setTimeout(addFillButtons, 100);
                        }
                    }
                });
            }
        });
    });
    
    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
}

// Start when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize);
} else {
    initialize();
}
