@echo off
echo === Pendrive Detection Test ===
echo.

echo 1. Testing WMIC availability:
wmic --version
echo.

echo 2. Listing all logical disks:
wmic logicaldisk get deviceid,drivetype,volumename,size
echo.

echo 3. Filtering for removable drives (drivetype=2):
wmic logicaldisk where drivetype=2 get deviceid,volumename
echo.

echo 4. Alternative PowerShell command:
powershell -Command "Get-WmiObject -Class Win32_LogicalDisk | Where-Object {$_.DriveType -eq 2} | Select-Object DeviceID, VolumeName, Size"
echo.

echo 5. Checking drive letters:
for %%d in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    if exist %%d:\ (
        echo Drive %%d: exists
    )
)
echo.

echo === Test Complete ===
pause
