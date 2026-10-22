@echo off
chcp 936 >nul 2>&1
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "ROOT=%CD%"

rem ================= 参数解析 =================
set "MODE=%~1"
if "%MODE%"=="" set "MODE=all"

echo ============================================================
echo   番析 AniScope  一键启动
echo   project : %ROOT%
echo   mode    : %MODE%   (all / dev / stop / status)
echo ============================================================
echo.

if /i "%MODE%"=="stop" goto :doStop
if /i "%MODE%"=="status" goto :doStatus

rem ================= 1. 中间件 =================
echo [1/6] 检查中间件...
call :isPortOpen 3306 MySQL
if errorlevel 1 (
  echo   [WARN] MySQL 未运行，尝试启动服务 MySQL80 ...
  net start MySQL80 >nul 2>&1
  timeout /t 3 /nobreak >nul
  call :isPortOpen 3306 MySQL
  if errorlevel 1 (
    echo   [FAIL] MySQL 启动失败，请手动启动 MySQL 服务后重试。
    pause
    exit /b 1
  )
)
echo   [OK] MySQL 已就绪 3306

call :isPortOpen 6379 Redis
if errorlevel 1 (
  echo   [WARN] Redis 未运行，尝试启动服务 Redis ...
  net start Redis >nul 2>&1
  timeout /t 2 /nobreak >nul
  call :isPortOpen 6379 Redis
  if errorlevel 1 (
    echo   [WARN] Redis 未启动（可选组件，不影响核心功能）
  ) else (
    echo   [OK] Redis 已就绪 6379
  )
)
echo.

rem ================= 2. 数据库 =================
echo [2/6] 检查数据库...
call :checkDb
echo.

rem ================= 3. 后端环境 =================
echo [3/6] 检查后端运行环境...
call :checkJava
if errorlevel 1 goto :failJava
call :checkMaven
if errorlevel 1 goto :failMaven
echo.

rem ================= 4. 清理旧进程 =================
echo [4/6] 清理 8000 端口上的旧进程...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING') do (
  echo   停止 PID %%p
  taskkill /PID %%p /F >nul 2>&1
)
timeout /t 2 /nobreak >nul
echo.

rem ================= 5. 启动后端 =================
echo [5/6] 启动后端（Spring Boot，日志: _boot.log）...
if exist "%ROOT%\_boot.log" del "%ROOT%\_boot.log"
start "anime-backend" /MIN cmd /c "cd /d %ROOT%\backend && mvn -q -Dmaven.test.skip=true compile spring-boot:run > %ROOT%\_boot.log 2>&1"
echo   后端启动中，首次编译约需 30~90 秒...
echo.

rem ================= 6. 等待就绪 =================
echo [6/6] 等待服务就绪...
for /L %%i in (1,1,60) do (
  timeout /t 3 /nobreak >nul
  netstat -ano ^| findstr :8000 ^| findstr LISTENING >nul
  if not errorlevel 1 (
    echo   [OK] 后端已就绪
    goto :ready
  )
)
echo   [FAIL] 后端启动超时，日志末尾 40 行：
powershell -NoProfile -Command "Get-Content '%ROOT%\_boot.log' -Tail 40"
pause
exit /b 1

:ready
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8000/admin/index.html' -UseBasicParsing -TimeoutSec 10; Write-Host ('   [OK] 页面 HTTP ' + $r.StatusCode) } catch { Write-Host '   [FAIL] 页面不可访问' }"
echo.
if /i "%MODE%"=="dev" goto :startFrontend
goto :openBrowser

:startFrontend
echo [附加] 启动前端开发服务器（Vite, 端口 8002）...
if not exist "%ROOT%\frontend\node_modules" (
  echo   首次运行，正在安装前端依赖（需要几分钟）...
  pushd "%ROOT%\frontend"
  call npm install
  popd
)
start "anime-frontend" /MIN cmd /c "cd /d %ROOT%\frontend && npm run dev > %ROOT%\_frontend.log 2>&1"
echo   前端开发地址: http://localhost:8002
echo   （生产模式请访问后端托管的 http://localhost:8000/admin）
echo.

:openBrowser
echo ============================================================
echo   启动完成
echo   后台管理 : http://localhost:8000/admin
echo   登录账号 : admin / 123456
echo   日志文件 : _boot.log
echo ============================================================
start "" http://localhost:8000/admin
echo.
pause
exit /b 0

rem ================= 停止 =================
:doStop
echo [停止] 关闭本项目相关进程...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING') do (
  echo   停止后端 PID %%p
  taskkill /PID %%p /F >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8002 ^| findstr LISTENING') do (
  echo   停止前端 PID %%p
  taskkill /PID %%p /F >nul 2>&1
)
echo [OK] 已停止
pause
exit /b 0

rem ================= 状态查询 =================
:doStatus
echo [状态] 端口占用情况：
netstat -ano | findstr ":8000 :8002 :3306 :6379" | findstr LISTENING
echo.
echo [状态] 数据表行数：
call :showCount t_user
call :showCount t_video_info
call :showCount t_video_play
call :showCount t_dataset
call :showCount t_train_task
pause
exit /b 0

rem ================= 子过程 =================
:isPortOpen
netstat -ano | findstr ":%~1" | findstr LISTENING >nul
exit /b %errorlevel%

:checkJava
if exist "C:\developtools\jdk17\bin\java.exe" (
  set "JAVA_HOME=C:\developtools\jdk17"
  set "PATH=%JAVA_HOME%\bin;%PATH%"
  echo   [OK] JDK 17 : C:\developtools\jdk17
  exit /b 0
)
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" (
    echo   [OK] JDK : %JAVA_HOME%
    exit /b 0
  )
)
where java >nul 2>&1
if errorlevel 1 (
  echo   [FAIL] 未找到 JDK。请安装 JDK 17 或设置 JAVA_HOME。
  exit /b 1
)
echo   [WARN] 找到的是系统默认 JDK，请确认版本为 17
exit /b 0

:checkMaven
where mvn >nul 2>&1
if errorlevel 1 (
  if exist "C:\developtools\apache-maven-3.9.9\bin\mvn.cmd" (
    set "PATH=C:\developtools\apache-maven-3.9.9\bin;%PATH%"
    echo   [OK] Maven : C:\developtools\apache-maven-3.9.9
    exit /b 0
  )
  echo   [FAIL] 未找到 Maven，请安装 Maven 或加入 PATH。
  exit /b 1
)
echo   [OK] Maven 可用
exit /b 0

:checkDb
if not exist "%ROOT%\.venv\Scripts\python.exe" (
  echo   [WARN] 未找到 Python 虚拟环境，双击 setup_venv.cmd 可重建
)
where mysql >nul 2>&1
if errorlevel 1 (
  if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin;%PATH%"
  ) else (
    echo   [WARN] 未找到 mysql 客户端，跳过数据库检查
    exit /b 0
  )
)
mysql -uroot -p123456 -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='anime_mangage_db' AND table_name='t_user';" 2>nul | findstr 1 >nul
if errorlevel 1 (
  echo   [WARN] 数据库 anime_mangage_db 未初始化，请先执行：
  echo          mysql -uroot -p123456 ^< sql\anime_mangage_db.sql
  echo          mysql -uroot -p123456 ^< sql\dataset_model_schema.sql
  exit /b 0
)
echo   [OK] 数据库 anime_mangage_db 已就绪
exit /b 0

:showCount
set "TBL=%~1"
for /f "tokens=*" %%c in ('mysql -uroot -p123456 -N anime_mangage_db -e "SELECT COUNT(*) FROM %TBL%;" 2^>nul') do (
  echo    %TBL% = %%c 行
)
exit /b 0

:failJava
echo   [FAIL] JDK 检查未通过，已中止。
pause
exit /b 1

:failMaven
echo   [FAIL] Maven 检查未通过，已中止。
pause
exit /b 1
