@echo off
setlocal

REM Get the directory where this batch file is located
set "SCRIPT_DIR=%~dp0"

REM Change to the script directory
cd /d "%SCRIPT_DIR%"

REM Check for admin privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: ONB-Classic requires Administrator privileges to bind to low-level ports ^(67, 69, 4011^).
    echo.
    echo Please right-click this script and select "Run as administrator"
    echo.
    goto :pause_exit
)

java -version >nul 2>&1
if %errorLevel% equ 0 (
    echo ONB-Classic is running with Administrator privileges.
    echo Binding to low-level ports ^(67, 69, 4011^)...
    echo.
    java -jar "onb-classic-all.jar" %*
) else (
    echo ERROR: Java is not installed or not in PATH.
    echo Please install Java first.
)

:pause_exit
echo.
echo Press any key to close this window...
pause >nul
