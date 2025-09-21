@echo off
echo Choose verification script:
echo 1. PowerShell version (recommended for Windows)
echo 2. Bash version (requires WSL/Git Bash)
echo.
set /p choice=Enter your choice (1 or 2): 

if "%choice%"=="1" (
    powershell -ExecutionPolicy Bypass -File "%~dp0verify-deployment.ps1"
) else if "%choice%"=="2" (
    bash "%~dp0verify-deployment.sh"
) else (
    echo Invalid choice. Running PowerShell version by default.
    powershell -ExecutionPolicy Bypass -File "%~dp0verify-deployment.ps1"
)

pause
