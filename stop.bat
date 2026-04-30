@echo off
chcp 65001 > nul
setlocal

echo ========================================
echo   Office AI Assistant - Stop
echo ========================================
echo.

echo Stopping backend on port 8123...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8123 ^| findstr LISTENING') do taskkill /PID %%a /F

echo Stopping frontend on port 3000...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING') do taskkill /PID %%a /F

echo.
echo Done. This script only stops processes listening on ports 8123 and 3000.
pause
