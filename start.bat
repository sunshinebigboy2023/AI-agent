@echo off
chcp 65001 > nul
setlocal

set ROOT_DIR=%~dp0
set BACKEND_DIR=%ROOT_DIR%ai-agent
set FRONTEND_DIR=%BACKEND_DIR%\office-ai-assistant-frontend
set LOCAL_CONFIG=%BACKEND_DIR%\src\main\resources\application-local.yml

echo ========================================
echo   Office AI Assistant - Start
echo ========================================
echo.

if not exist "%LOCAL_CONFIG%" (
  if "%DASHSCOPE_API_KEY%"=="" (
    echo [SETUP] DashScope API key is required for backend startup.
    echo [SETUP] It will be saved to:
    echo         %LOCAL_CONFIG%
    echo [SETUP] This file is ignored by Git.
    echo.
    set /p DASHSCOPE_API_KEY=Paste DashScope API key and press Enter: 
    echo spring:>"%LOCAL_CONFIG%"
    echo   ai:>>"%LOCAL_CONFIG%"
    echo     dashscope:>>"%LOCAL_CONFIG%"
    echo       api-key: %DASHSCOPE_API_KEY%>>"%LOCAL_CONFIG%"
  )
)

echo [1/2] Starting backend on http://localhost:8123/api ...
start "Office AI Backend" cmd /k "cd /d ""%BACKEND_DIR%"" && .\mvnw.cmd spring-boot:run"

echo Waiting for backend warm-up...
timeout /t 20 /nobreak > nul

echo [2/2] Starting frontend on http://localhost:3000/index.html ...
start "Office AI Frontend" cmd /k "cd /d ""%FRONTEND_DIR%"" && npm run dev"

echo.
echo ========================================
echo   Started
echo   Backend: http://localhost:8123/api
echo   Health:  http://localhost:8123/api/health
echo   Frontend: http://localhost:3000/index.html
echo   API docs: http://localhost:8123/api/swagger-ui.html
echo ========================================
pause
