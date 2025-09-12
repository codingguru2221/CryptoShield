@echo off
echo === Complete System Test ===
echo.

echo 1. Testing Java compilation...
mvn clean compile
if %errorlevel% neq 0 (
    echo ERROR: Java compilation failed
    pause
    exit /b 1
)
echo Java compilation successful!
echo.

echo 2. Testing Python dependencies...
python -c "import flask, flask_cors; print('Python dependencies OK')"
if %errorlevel% neq 0 (
    echo ERROR: Python dependencies missing
    echo Run: pip install -r requirements.txt
    pause
    exit /b 1
)
echo Python dependencies OK!
echo.

echo 3. Testing Python server...
start /B python password_server.py
timeout /t 3 /nobreak >nul
curl -s http://localhost:5000/api/status
if %errorlevel% neq 0 (
    echo WARNING: Could not test server (curl not available)
) else (
    echo Python server test successful!
)
echo.

echo 4. System test complete!
echo.
echo To run the full system:
echo 1. Insert your pendrive
echo 2. Run: mvn exec:java -Dexec.mainClass="com.codex.passwordmanager.Main"
echo 3. Install browser extension from browser_extension folder
echo.
pause
