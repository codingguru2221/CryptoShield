@echo off
echo Testing Web Extension Security Features
echo ======================================
echo.
echo This script will help you test the security features of the web extension.
echo.
echo Instructions:
echo 1. Make sure the Java application is running and pendrive is connected
echo 2. Load the web extension in your browser
echo 3. Open the extension popup to verify it shows connected status
echo 4. Try to fill a password to ensure it works
echo 5. Remove the pendrive or terminate the Java application
echo 6. Check that the extension shows disconnected status
echo 7. Try to fill a password - it should show an error message
echo 8. Check that all password data is cleared from the extension
echo.
echo Expected Behavior:
echo - Extension badge should show "!" when disconnected
echo - Popup should show "Server not running or pendrive not detected"
echo - All password operations should be blocked
echo - All stored data should be cleared
echo - Notification should appear about connection loss
echo.
echo Press any key to continue...
pause >nul
echo.
echo Test completed. Check the extension behavior as described above.
echo.
pause
