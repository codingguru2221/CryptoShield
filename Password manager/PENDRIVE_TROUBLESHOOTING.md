# Pendrive Detection Troubleshooting Guide

## Problem
The pendrive is not being detected by the Password Manager application.

## Solutions

### 1. Run the Test Scripts
First, run these test scripts to diagnose the issue:

```bash
# Test the batch file
test_pendrive.bat

# Test the Java detection
javac test_pendrive_detection.java
java test_pendrive_detection
```

### 2. Check Common Issues

#### A. Pendrive Not Properly Connected
- Ensure the pendrive is fully inserted into the USB port
- Try a different USB port
- Check if the pendrive appears in Windows File Explorer
- Verify the pendrive has a drive letter assigned (e.g., E:, F:, G:)

#### B. Pendrive Not Recognized by Windows
- Open Device Manager and check for any USB device errors
- Try the pendrive on another computer to verify it works
- Check if Windows shows a notification when inserting the pendrive

#### C. Drive Type Issues
Some pendrives may not be detected as "removable" drives. The application now uses multiple detection methods:
- WMIC command: `wmic logicaldisk where drivetype=2 get deviceid`
- FileSystemView detection
- Direct drive enumeration

### 3. Manual Testing

#### Test WMIC Command
Open Command Prompt and run:
```cmd
wmic logicaldisk where drivetype=2 get deviceid
```

This should show your pendrive's drive letter if it's detected as a removable drive.

#### Test All Drives
```cmd
wmic logicaldisk get deviceid,drivetype,volumename
```

Look for drives with `drivetype=2` (removable drives).

### 4. Enhanced Detection Features

The updated application now includes:
- **Comprehensive debugging output** - Shows exactly what drives are found
- **Multiple detection methods** - Uses 3 different approaches to find pendrives
- **Better error messages** - Provides specific guidance when no pendrive is found
- **Fallback detection** - If one method fails, others are tried

### 5. Run the Updated Application

1. Compile the project:
```bash
mvn clean compile
```

2. Run the application:
```bash
mvn exec:java -Dexec.mainClass="com.codex.passwordmanager.Main"
```

3. Check the console output for detailed detection information

### 6. Alternative Solutions

If the pendrive still isn't detected:

#### Option A: Manual Drive Selection
Modify the application to allow manual drive selection instead of automatic detection.

#### Option B: Use Any Drive
Change the application to work with any available drive, not just removable ones.

#### Option C: Network Drive Support
Add support for network drives or cloud storage.

### 7. Debug Information

When you run the application, it will now show:
- WMIC command execution details
- All detected drives and their properties
- Error messages from failed commands
- Step-by-step detection process

This information will help identify exactly why the pendrive isn't being detected.

### 8. Contact Information

If the issue persists after trying these solutions, please provide:
- The console output from running the application
- The output from `test_pendrive.bat`
- Your Windows version
- The pendrive make/model
- Whether the pendrive appears in Windows File Explorer
