@echo off
echo === Pendrive Password Manager Setup ===
echo.

echo 1. Installing Python dependencies...
pip install -r requirements.txt
echo.

echo 2. Compiling Java application...
mvn clean compile
echo.

echo 3. Setup complete!
echo.
echo To run the application:
echo   - Insert your pendrive
echo   - Run: mvn exec:java -Dexec.mainClass="com.codex.passwordmanager.Main"
echo.
echo To install browser extension:
echo   - Open Chrome/Edge
echo   - Go to chrome://extensions/
echo   - Enable "Developer mode"
echo   - Click "Load unpacked"
echo   - Select the "browser_extension" folder
echo.
pause
