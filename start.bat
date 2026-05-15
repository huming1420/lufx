@echo off
chcp 65001 >nul
title Lufax Dashboard

echo ========================================
echo    Lufax Dashboard
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python not found
    pause
    exit /b 1
)
python --version
echo.

echo [2/3] Dependencies...
pip install fastapi uvicorn python-dotenv -q 2>nul
echo OK
echo.

echo [3/3] Starting server...
echo.
echo ========================================
echo    http://localhost:8000/lufax_dashboard3.html
echo    http://localhost:8000/ai_insights.html
echo    http://localhost:8000/docs
echo.
echo    Ctrl+C to stop
echo ========================================
echo.

start "" http://localhost:8000/lufax_dashboard3.html

python -m uvicorn backend.server:app --host 0.0.0.0 --port 8000

pause
