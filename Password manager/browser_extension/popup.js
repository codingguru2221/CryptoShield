// Popup script for Pendrive Password Manager Extension

const SERVER_URL = 'http://127.0.0.1:5000';
let currentTab = null;
let passwords = [];
let isConnected = false;

// Initialize popup
let popupConnectionInterval = null;

document.addEventListener('DOMContentLoaded', async () => {
    await initializePopup();
    // Start fast polling while popup is open to detect disconnects immediately
    if (popupConnectionInterval) clearInterval(popupConnectionInterval);
    popupConnectionInterval = setInterval(async () => {
        await checkServerConnection();
    }, 2000);
    // Listen for connection status broadcasts from background
    chrome.runtime.onMessage.addListener(async (request) => {
        if (request.type === 'CONNECTION_LOST') {
            await clearAllPopupData();
            const statusDiv = document.getElementById('status');
            const statusText = document.getElementById('statusText');
            statusDiv.className = 'status disconnected';
            statusText.textContent = '❌ Server not running or pendrive not detected';
            document.getElementById('mainContent').classList.add('hidden');
            document.getElementById('loading').classList.remove('hidden');
        }
        if (request.type === 'CONNECTION_RESTORED') {
            // Refresh UI and data when reconnected
            await checkServerConnection();
            await loadPasswords();
        }
    });
});

// Clean up when popup closes
window.addEventListener('unload', () => {
    if (popupConnectionInterval) {
        clearInterval(popupConnectionInterval);
        popupConnectionInterval = null;
    }
});

async function initializePopup() {
    try {
        // Get current tab
        const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
        currentTab = tabs[0];
        
        // Check server connection
        await checkServerConnection();
        
        // Load passwords
        await loadPasswords();
        
        // Set up event listeners
        setupEventListeners();
        
        // Update current website display
        updateCurrentWebsite();
        
    } catch (error) {
        showError('Failed to initialize: ' + error.message);
    }
}

async function checkServerConnection() {
    try {
        const response = await fetch(`${SERVER_URL}/api/status`);
        const data = await response.json();
        
        const statusDiv = document.getElementById('status');
        const statusText = document.getElementById('statusText');
        
        const wasConnected = isConnected;
        isConnected = data.server_running && data.pendrive_detected && data.java_running;
        
        if (isConnected) {
            statusDiv.className = 'status connected';
            statusText.textContent = `✅ Connected - ${data.passwords_count} passwords available`;
            document.getElementById('mainContent').classList.remove('hidden');
            document.getElementById('loading').classList.add('hidden');
        } else {
            statusDiv.className = 'status disconnected';
            statusText.textContent = '❌ Java app not running, server or pendrive missing';
            document.getElementById('mainContent').classList.add('hidden');
            document.getElementById('loading').classList.remove('hidden');
            showError('Please ensure the Java application is running and pendrive is connected');
        }
        
        // If connection was lost, clear all data
        if (wasConnected && !isConnected) {
            await clearAllPopupData();
        }
    } catch (error) {
        const wasConnected = isConnected;
        isConnected = false;
        
        const statusDiv = document.getElementById('status');
        const statusText = document.getElementById('statusText');
        statusDiv.className = 'status disconnected';
        statusText.textContent = '❌ Cannot connect to server';
        document.getElementById('mainContent').classList.add('hidden');
        document.getElementById('loading').classList.remove('hidden');
        showError('Cannot connect to password server. Please ensure the Java application is running.');
        
        // If connection was lost, clear all data
        if (wasConnected && !isConnected) {
            await clearAllPopupData();
        }
    }
}

async function loadPasswords() {
    if (!isConnected) {
        passwords = [];
        displayPasswords();
        return;
    }
    
    try {
        const response = await fetch(`${SERVER_URL}/api/passwords`);
        const data = await response.json();
        passwords = data.passwords || [];
        displayPasswords();
    } catch (error) {
        console.error('Error loading passwords:', error);
        passwords = [];
        displayPasswords();
        showError('Failed to load passwords');
    }
}

function displayPasswords() {
    const passwordsList = document.getElementById('passwordsList');
    passwordsList.innerHTML = '';
    
    if (passwords.length === 0) {
        passwordsList.innerHTML = '<div style="text-align: center; opacity: 0.7; padding: 20px;">No passwords saved yet</div>';
        return;
    }
    
    passwords.forEach((password, index) => {
        const passwordDiv = document.createElement('div');
        passwordDiv.className = 'password-item';
        passwordDiv.innerHTML = `
            <div class="website">${password.website}</div>
            <div class="username">${password.username}</div>
        `;
        
        passwordDiv.addEventListener('click', () => {
            fillPassword(password);
        });
        
        passwordsList.appendChild(passwordDiv);
    });
}

async function fillPassword(password) {
    if (!isConnected) {
        showError('Cannot fill password: Server disconnected or pendrive removed');
        return;
    }
    
    try {
        // Inject content script to fill password
        await chrome.scripting.executeScript({
            target: { tabId: currentTab.id },
            function: fillPasswordFields,
            args: [password]
        });
        
        // Close popup
        window.close();
    } catch (error) {
        showError('Failed to fill password: ' + error.message);
    }
}

async function autoFillPassword() {
    if (!currentTab) return;
    
    if (!isConnected) {
        showError('Cannot auto-fill: Server disconnected or pendrive removed');
        return;
    }
    
    try {
        // Get current website domain
        const url = new URL(currentTab.url);
        const domain = url.hostname;
        
        // Find matching passwords
        const matchingPasswords = passwords.filter(p => 
            domain.includes(p.website.toLowerCase()) || 
            p.website.toLowerCase().includes(domain)
        );
        
        if (matchingPasswords.length === 0) {
            showError('No saved passwords for this website');
            return;
        }
        
        if (matchingPasswords.length === 1) {
            await fillPassword(matchingPasswords[0]);
        } else {
            // Show selection dialog
            showPasswordSelection(matchingPasswords);
        }
    } catch (error) {
        showError('Failed to auto-fill: ' + error.message);
    }
}

function showPasswordSelection(matchingPasswords) {
    const passwordsList = document.getElementById('passwordsList');
    passwordsList.innerHTML = '<h4>Select password to fill:</h4>';
    
    matchingPasswords.forEach(password => {
        const passwordDiv = document.createElement('div');
        passwordDiv.className = 'password-item';
        passwordDiv.innerHTML = `
            <div class="website">${password.website}</div>
            <div class="username">${password.username}</div>
        `;
        
        passwordDiv.addEventListener('click', () => {
            fillPassword(password);
        });
        
        passwordsList.appendChild(passwordDiv);
    });
}

async function saveNewPassword() {
    if (!isConnected) {
        showError('Cannot save password: Server disconnected or pendrive removed');
        return;
    }
    
    const website = document.getElementById('newWebsite').value.trim();
    const username = document.getElementById('newUsername').value.trim();
    const password = document.getElementById('newPassword').value.trim();
    
    if (!website || !username || !password) {
        showError('Please fill in all fields');
        return;
    }
    
    try {
        const response = await fetch(`${SERVER_URL}/api/save`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                website: website,
                username: username,
                password: password
            })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            // Clear form
            document.getElementById('newWebsite').value = '';
            document.getElementById('newUsername').value = '';
            document.getElementById('newPassword').value = '';
            
            // Reload passwords
            await loadPasswords();
            
            // Show success message
            showSuccess('Password saved successfully!');
        } else {
            showError(data.error || 'Failed to save password');
        }
    } catch (error) {
        showError('Failed to save password: ' + error.message);
    }
}

function updateCurrentWebsite() {
    if (currentTab) {
        try {
            const url = new URL(currentTab.url);
            document.getElementById('currentWebsite').textContent = url.hostname;
        } catch (error) {
            document.getElementById('currentWebsite').textContent = 'Invalid URL';
        }
    }
}

function setupEventListeners() {
    document.getElementById('fillPassword').addEventListener('click', autoFillPassword);
    document.getElementById('savePassword').addEventListener('click', saveNewPassword);
}

function showError(message) {
    const errorDiv = document.getElementById('error');
    const errorText = document.getElementById('errorText');
    errorText.textContent = message;
    errorDiv.classList.remove('hidden');
    
    // Hide error after 5 seconds
    setTimeout(() => {
        errorDiv.classList.add('hidden');
    }, 5000);
}

function showSuccess(message) {
    // Create temporary success message
    const successDiv = document.createElement('div');
    successDiv.className = 'error';
    successDiv.style.background = 'rgba(76, 175, 80, 0.3)';
    successDiv.textContent = message;
    
    document.body.insertBefore(successDiv, document.body.firstChild);
    
    // Remove after 3 seconds
    setTimeout(() => {
        successDiv.remove();
    }, 3000);
}

// Clear all popup data when connection is lost
async function clearAllPopupData() {
    passwords = [];
    displayPasswords();
    
    // Clear any form data
    const websiteField = document.getElementById('newWebsite');
    const usernameField = document.getElementById('newUsername');
    const passwordField = document.getElementById('newPassword');
    
    if (websiteField) websiteField.value = '';
    if (usernameField) usernameField.value = '';
    if (passwordField) passwordField.value = '';
    
    console.log('Popup data cleared due to connection loss');
}

// Function to be injected into web pages
function fillPasswordFields(password) {
    // Find username/email fields
    const usernameSelectors = [
        'input[type="email"]',
        'input[name*="user"]',
        'input[name*="email"]',
        'input[id*="user"]',
        'input[id*="email"]',
        'input[placeholder*="user"]',
        'input[placeholder*="email"]'
    ];
    
    // Find password fields
    const passwordSelectors = [
        'input[type="password"]',
        'input[name*="pass"]',
        'input[id*="pass"]'
    ];
    
    let usernameField = null;
    let passwordField = null;
    
    // Find username field
    for (const selector of usernameSelectors) {
        usernameField = document.querySelector(selector);
        if (usernameField) break;
    }
    
    // Find password field
    for (const selector of passwordSelectors) {
        passwordField = document.querySelector(selector);
        if (passwordField) break;
    }
    
    // Fill fields
    if (usernameField) {
        usernameField.value = password.username;
        usernameField.dispatchEvent(new Event('input', { bubbles: true }));
        usernameField.dispatchEvent(new Event('change', { bubbles: true }));
    }
    
    if (passwordField) {
        passwordField.value = password.password;
        passwordField.dispatchEvent(new Event('input', { bubbles: true }));
        passwordField.dispatchEvent(new Event('change', { bubbles: true }));
    }
    
    // Show notification
    if (usernameField || passwordField) {
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #4CAF50;
            color: white;
            padding: 10px 20px;
            border-radius: 5px;
            z-index: 10000;
            font-family: Arial, sans-serif;
            font-size: 14px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.3);
        `;
        notification.textContent = 'Password filled successfully!';
        document.body.appendChild(notification);
        
        setTimeout(() => {
            notification.remove();
        }, 3000);
    }
}
