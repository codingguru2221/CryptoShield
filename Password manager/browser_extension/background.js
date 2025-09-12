// Background script for Pendrive Password Manager Extension

const SERVER_URL = 'http://127.0.0.1:5000';
let isConnected = false;
let connectionCheckInterval = null;

// Extension installation
chrome.runtime.onInstalled.addListener(() => {
    console.log('Pendrive Password Manager Extension installed');
    
    // Set up context menu
    chrome.contextMenus.create({
        id: 'fillPassword',
        title: 'Fill Password from Pendrive',
        contexts: ['editable']
    });
    
    // Start connection monitoring
    startConnectionMonitoring();
    scheduleAlarm();
});

// Start monitoring on browser startup as well
chrome.runtime.onStartup.addListener(() => {
    startConnectionMonitoring();
    scheduleAlarm();
});

// Context menu click handler
chrome.contextMenus.onClicked.addListener(async (info, tab) => {
    if (info.menuItemId === 'fillPassword') {
        if (!isConnected) {
            chrome.notifications.create({
                type: 'basic',
                iconUrl: 'icons/icon48.png',
                title: 'Pendrive Password Manager',
                message: 'Cannot access passwords: Server disconnected or pendrive removed'
            });
            return;
        }
        await handleContextMenuFill(tab);
    }
});

// Handle context menu password fill
async function handleContextMenuFill(tab) {
    try {
        // Get current website domain
        const url = new URL(tab.url);
        const domain = url.hostname;
        
        // Get passwords for this website
        const response = await fetch(`${SERVER_URL}/api/passwords/${domain}`);
        const data = await response.json();
        
        if (data.passwords && data.passwords.length > 0) {
            // Inject content script to fill password
            await chrome.scripting.executeScript({
                target: { tabId: tab.id },
                function: fillPasswordFields,
                args: [data.passwords[0]]
            });
        } else {
            // Show notification
            chrome.notifications.create({
                type: 'basic',
                iconUrl: 'icons/icon48.png',
                title: 'Pendrive Password Manager',
                message: 'No saved passwords found for this website'
            });
        }
    } catch (error) {
        console.error('Error filling password:', error);
        chrome.notifications.create({
            type: 'basic',
            iconUrl: 'icons/icon48.png',
            title: 'Pendrive Password Manager',
            message: 'Error: ' + error.message
        });
    }
}

// Handle keyboard shortcuts
chrome.commands.onCommand.addListener(async (command) => {
    if (command === 'fill-password') {
        if (!isConnected) {
            chrome.notifications.create({
                type: 'basic',
                iconUrl: 'icons/icon48.png',
                title: 'Pendrive Password Manager',
                message: 'Cannot access passwords: Server disconnected or pendrive removed'
            });
            return;
        }
        const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tabs[0]) {
            await handleContextMenuFill(tabs[0]);
        }
    }
});

// Start connection monitoring
function startConnectionMonitoring() {
    if (connectionCheckInterval) {
        clearInterval(connectionCheckInterval);
    }
    
    connectionCheckInterval = setInterval(async () => {
        await checkServerConnection();
    }, 2000); // Check every 2 seconds for faster detection
}

// Fallback periodic wake-up using alarms (MV3 service worker may be suspended)
function scheduleAlarm() {
    if (chrome.alarms) {
        chrome.alarms.create('connectionCheck', { periodInMinutes: 1 });
    }
}

if (chrome.alarms) {
    chrome.alarms.onAlarm.addListener((alarm) => {
        if (alarm.name === 'connectionCheck') {
            checkServerConnection();
        }
    });
}

// Check server connection
async function checkServerConnection() {
    try {
        const response = await fetch(`${SERVER_URL}/api/status`);
        const data = await response.json();
        
        const wasConnected = isConnected;
        isConnected = data.server_running && data.pendrive_detected && data.java_running;
        
        // Update badge
        if (isConnected) {
            chrome.action.setBadgeText({ text: data.passwords_count.toString() });
            chrome.action.setBadgeBackgroundColor({ color: '#4CAF50' });
        } else {
            chrome.action.setBadgeText({ text: '!' });
            chrome.action.setBadgeBackgroundColor({ color: '#F44336' });
        }
        
        // If connection was lost, clear all data and notify
        if (wasConnected && !isConnected) {
            await clearAllData();
            notifyConnectionLost();
            // Tell all extension contexts (popup/content) to clear immediately
            chrome.runtime.sendMessage({ type: 'CONNECTION_LOST' }).catch(() => {});
        }
        
        // If connection restored, notify contexts to refresh
        if (!wasConnected && isConnected) {
            chrome.runtime.sendMessage({ type: 'CONNECTION_RESTORED', passwordsCount: data.passwords_count }).catch(() => {});
        }
        
    } catch (error) {
        const wasConnected = isConnected;
        isConnected = false;
        chrome.action.setBadgeText({ text: '!' });
        chrome.action.setBadgeBackgroundColor({ color: '#F44336' });
        
        // If connection was lost, clear all data and notify
        if (wasConnected && !isConnected) {
            await clearAllData();
            notifyConnectionLost();
            chrome.runtime.sendMessage({ type: 'CONNECTION_LOST' }).catch(() => {});
        }
    }
}

// Clear all stored data
async function clearAllData() {
    try {
        // Clear extension storage
        await chrome.storage.local.clear();
        
        // Clear session storage for all tabs
        const tabs = await chrome.tabs.query({});
        for (const tab of tabs) {
            try {
                await chrome.scripting.executeScript({
                    target: { tabId: tab.id },
                    function: () => {
                        // Clear any cached data in the page
                        if (window.pendrivePasswordData) {
                            window.pendrivePasswordData = null;
                        }
                        // Clear any fill buttons
                        document.querySelectorAll('.pendrive-fill-btn').forEach(btn => btn.remove());
                    }
                });
            } catch (error) {
                // Ignore errors for tabs that can't be accessed
            }
        }
        
        console.log('All data cleared due to connection loss');
    } catch (error) {
        console.error('Error clearing data:', error);
    }
}

// Notify about connection loss
function notifyConnectionLost() {
    chrome.notifications.create({
        type: 'basic',
        iconUrl: 'icons/icon48.png',
        title: 'Pendrive Password Manager',
        message: 'Connection lost! All password data has been cleared for security.'
    });
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
