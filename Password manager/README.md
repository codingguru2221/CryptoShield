# Pendrive-Based Password Manager

A comprehensive password management system that stores passwords on a USB pendrive and provides auto-fill functionality through a browser extension.

## 🚀 Features

- **Pendrive Detection**: Automatically detects USB pendrives
- **Secure Storage**: Passwords stored locally on pendrive with Base64 encoding
- **Java GUI**: Desktop application for managing passwords
- **Python Server**: Local web server for browser extension communication
- **Browser Extension**: Auto-fill passwords on websites
- **Cross-Platform**: Works on Windows, macOS, and Linux

## 📋 System Requirements

- Java 23 or higher
- Python 3.7 or higher
- Flask and Flask-CORS Python packages
- Chrome, Edge, or Firefox browser
- USB pendrive

## 🛠️ Installation

### 1. Setup Dependencies

Run the setup script:
```bash
setup.bat
```

Or manually:
```bash
# Install Python dependencies
pip install -r requirements.txt

# Compile Java application
mvn clean compile
```

### 2. Install Browser Extension

1. Open Chrome/Edge browser
2. Go to `chrome://extensions/`
3. Enable "Developer mode" (toggle in top right)
4. Click "Load unpacked"
5. Select the `browser_extension` folder
6. The extension should now appear in your extensions list

## 🎯 Usage

### Starting the Application

1. **Insert your USB pendrive**
2. **Run the Java application**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.codex.passwordmanager.Main"
   ```

### Workflow

1. **Pendrive Detection**: The application automatically detects your pendrive
2. **Java GUI**: Use the desktop application to:
   - Save new passwords
   - View saved passwords
   - Edit/delete passwords
3. **Python Server**: Automatically starts on `http://localhost:5000`
4. **Browser Extension**: 
   - Click the extension icon to view saved passwords
   - Use "Auto-Fill Password" button on login pages
   - Add new passwords directly from the extension

### Using the Browser Extension

1. **View Passwords**: Click the extension icon to see all saved passwords
2. **Auto-Fill**: Click "Auto-Fill Password" button on any login page
3. **Add Passwords**: Use the extension popup to add new passwords
4. **Context Menu**: Right-click on password fields and select "Fill Password from Pendrive"

## 🔧 Technical Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   USB Pendrive  │    │  Java Desktop   │    │  Python Server  │
│                 │    │   Application   │    │  (Flask API)    │
│  passwords.txt  │◄───┤                 ├───►│  localhost:5000 │
│  (Base64 data)  │    │  - Save/Edit    │    │  - REST API     │
└─────────────────┘    │  - View/Delete  │    │  - CORS enabled │
                       └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │ Browser Extension│
                                              │                 │
                                              │ - Auto-fill     │
                                              │ - Password UI   │
                                              │ - Context menu  │
                                              └─────────────────┘
```

## 📁 Project Structure

```
Password manager/
├── src/main/java/com/codex/passwordmanager/
│   ├── Main.java                 # Main application entry point
│   ├── PendriveDetector.java     # Pendrive detection logic
│   └── PasswordManagerUI.java    # Desktop GUI
├── browser_extension/
│   ├── manifest.json             # Extension manifest
│   ├── popup.html               # Extension popup UI
│   ├── popup.js                 # Popup functionality
│   ├── background.js            # Background script
│   ├── content.js               # Content script for web pages
│   └── icons/                   # Extension icons
├── password_server.py           # Python Flask server
├── requirements.txt             # Python dependencies
├── setup.bat                   # Setup script
└── README.md                   # This file
```

## 🔒 Security Features

- **Local Storage**: All passwords stored locally on pendrive
- **No Cloud**: No data sent to external servers
- **Base64 Encoding**: Basic encoding for password storage
- **CORS Protection**: Server only accepts requests from localhost
- **Extension Permissions**: Minimal required permissions

## 🐛 Troubleshooting

### Pendrive Not Detected

1. Run the test script: `test_pendrive.bat`
2. Check console output for detailed detection information
3. Ensure pendrive is properly connected and has a drive letter
4. Try different USB ports

### Python Server Issues

1. Ensure Python is installed: `python --version`
2. Install dependencies: `pip install -r requirements.txt`
3. Check if port 5000 is available
4. Check console output for error messages

### Browser Extension Issues

1. Ensure extension is properly loaded in browser
2. Check browser console for errors
3. Verify server is running on localhost:5000
4. Check extension permissions

### Common Issues

- **"Cannot connect to server"**: Ensure Java application is running
- **"No pendrive detected"**: Check pendrive connection and run test scripts
- **"Extension not working"**: Reload extension and check permissions

## 🔄 Development

### Adding New Features

1. **Java GUI**: Modify `PasswordManagerUI.java`
2. **Server API**: Add endpoints to `password_server.py`
3. **Extension**: Update `popup.js`, `background.js`, or `content.js`

### Testing

1. **Pendrive Detection**: Use `test_pendrive_detection.java`
2. **Server API**: Test endpoints with curl or Postman
3. **Extension**: Use browser developer tools

## 📝 API Endpoints

The Python server provides these REST endpoints:

- `GET /api/status` - Server and pendrive status
- `GET /api/passwords` - Get all passwords
- `GET /api/passwords/<website>` - Get passwords for specific website
- `POST /api/save` - Save new password
- `POST /api/search` - Search passwords

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is open source and available under the MIT License.

## 🆘 Support

If you encounter issues:

1. Check the troubleshooting section
2. Run the test scripts
3. Check console output for error messages
4. Ensure all dependencies are properly installed

---

**Note**: This is a local password manager. Always keep your pendrive secure and make regular backups of your password data.
