@echo off
rem ============================================================================
rem  movie-system - backend one-click start (Windows)
rem  Usage : double-click this file in project root (movie-system).
rem  Steps : 1 stop old backend -> 2 start backend in background
rem          -> 3 wait port 8000 -> 4 open browser
rem ============================================================================
setlocal
cd /d "%~dp0"
set "ROOT=%CD%"

echo ============================================================
echo  movie-system backend start
echo  project dir: %ROOT%
echo ============================================================

echo.
echo [1/4] Stopping old backend (port 8000) if any ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING') do (
  echo   killing PID %%p
  taskkill /PID %%p /F >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo.
echo [2/4] Starting backend in background (log: _boot.log) ...
start "movie-backend" /MIN cmd /c "cd /d %ROOT%\backend && mvn -q -Dmaven.test.skip=true compile spring-boot:run > %ROOT%\_boot.log 2>&1"

echo.
echo [3/4] Waiting for port 8000 (max ~180s) ...
set "UP=0"
for /L %%i in (1,1,60) do (
  netstat -ano | findstr :8000 | findstr LISTENING >nul
  if not errorlevel 1 (
    set "UP=1"
    echo   Backend is UP.
    goto :open
  )
  timeout /t 3 /nobreak >nul
)

echo   [FAIL] Backend did not start. Last 30 lines of _boot.log:
powershell -NoProfile -Command "Get-Content '%ROOT%\_boot.log' -Tail 30"
pause
exit /b 1

:open
echo.
echo [4/4] Opening browser ...
start "" http://localhost:8000/admin

echo.
echo ------------------------------------------------------------
echo  Done. Login with admin / 123456
echo  You can close this window; backend keeps running.
echo  Log file: _boot.log
echo ------------------------------------------------------------
pause
endlocal
