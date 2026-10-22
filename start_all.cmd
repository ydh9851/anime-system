@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "ROOT=%CD%"
set "BACKEND=%ROOT%\backend"
set "FRONTEND=%ROOT%\frontend"
set "ADMIN_STATIC=%BACKEND%\src\main\resources\static\admin"

set "MODE=%~1"
if "%MODE%"=="" set "MODE=all"

echo ============================================================
echo         番析 AniScope  ·  动漫数据分析与推荐系统
echo         一键启动  (模式: %MODE%)
echo ============================================================
echo.
echo   首次启动需要编译，约需 30~90 秒，请勿关闭本窗口。
echo.

if /i "%MODE%"=="stop" goto :doStop
if /i "%MODE%"=="status" goto :doStatus

rem ================= 1. 中间件 =================
echo [1/7] 检查 MySQL 与 Redis...
call :isPortOpen 3306
if errorlevel 1 (
  echo   MySQL 未运行，正在启动服务 MySQL80 ...
  net start MySQL80 >nul 2>&1
  timeout /t 4 /nobreak >nul
  call :isPortOpen 3306
  if errorlevel 1 (
    echo.
    echo   [失败] MySQL 无法启动。请先在「服务」中手动启动 MySQL80 后重试。
    goto :failEnd
  )
)
echo   [OK] MySQL 已就绪 3306

call :isPortOpen 6379
if errorlevel 1 (
  echo   Redis 未运行，正在启动服务 Redis ...
  net start Redis >nul 2>&1
  timeout /t 2 /nobreak >nul
  call :isPortOpen 6379
  if errorlevel 1 (
    echo   [提示] Redis 未启动（可选组件，不影响主要功能）
  ) else (
    echo   [OK] Redis 已就绪 6379
  )
)
echo.

rem ================= 2. JDK =================
echo [2/7] 检查 JDK ...
call :setupJava
if errorlevel 1 (
  echo.
  echo   [失败] 未找到可用的 JDK 17，请安装 JDK 17 或设置 JAVA_HOME。
  goto :failEnd
)
echo.

rem ================= 3. Maven =================
echo [3/7] 检查 Maven ...
call :setupMaven
if errorlevel 1 (
  echo.
  echo   [失败] 未找到 Maven，请安装 Maven 或加入 PATH。
  goto :failEnd
)
echo.

rem ================= 4. 数据库 =================
echo [4/7] 检查数据库 ...
call :setupMysql
call :checkDb
echo.

rem ================= 5. 前端产物 =================
echo [5/7] 检查前端页面资源 ...
if not exist "%ADMIN_STATIC%\index.html" (
  echo   未找到前端构建产物，正在自动构建（需要几分钟）...
  call :buildFrontend
  if errorlevel 1 (
    echo.
    echo   [失败] 前端构建失败，请到 frontend 目录手动执行 npm install 与 npm run build。
    goto :failEnd
  )
)
echo   [OK] 前端页面资源就绪
echo.

rem ================= 6. 清理旧进程 =================
echo [6/7] 清理 8000 端口上的旧进程...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING') do (
  echo   正在停止 PID %%p
  taskkill /PID %%p /F >nul 2>&1
)
timeout /t 2 /nobreak >nul
echo.

rem ================= 7. 启动后端 =================
echo [7/7] 启动后端服务...
if exist "%ROOT%\_boot.log" del "%ROOT%\_boot.log"
echo   正在另开一个窗口运行 Spring Boot，日志写入 _boot.log
echo   （首次编译较慢，请耐心等待，窗口可最小化但不要关闭）
echo.
rem 注意：set 出来的变量不会传给新进程，JAVA_HOME 必须在这里再设一次
start "AnimeSystem 后端服务（请勿关闭）" cmd /k "set JAVA_HOME=%JAVA_HOME%&& set PATH=%JAVA_HOME%\bin;%PATH%&& cd /d %BACKEND%&& mvn -q -Dmaven.test.skip=true compile spring-boot:run 1> %ROOT%\_boot.log 2>&1"

echo   正在等待后端就绪（最长 180 秒）...
set "READY=0"
for /L %%i in (1,1,60) do (
  timeout /t 3 /nobreak >nul
  netstat -ano ^| findstr :8000 ^| findstr LISTENING >nul
  if not errorlevel 1 (
    set "READY=1"
    goto :ready
  )
)

echo.
echo   [失败] 等待超时，后端未能启动。日志末尾 30 行如下：
echo ------------------------------------------------------------
if exist "%ROOT%\_boot.log" (
  powershell -NoProfile -Command "Get-Content '%ROOT%\_boot.log' -Tail 30"
) else (
  echo   没有生成日志文件，后端窗口可能被关闭了。
)
echo ------------------------------------------------------------
echo   常见原因：
echo     1) 8000 端口被其他程序占用
echo     2) MySQL 未启动或数据库未初始化
echo     3) 后端窗口里出现了报错，请把那个窗口的内容发给我
goto :failEnd

:ready
echo.
echo   [OK] 后端已就绪，正在做页面检查...
timeout /t 2 /nobreak >nul
echo.

if /i "%MODE%"=="dev" goto :startFrontend

echo ============================================================
echo   启动成功
echo.
echo     后台管理页面 : http://localhost:8000/admin
echo     登录账号     : admin / 123456
echo     后端日志     : %ROOT%\_boot.log
echo.
echo   正在打开浏览器...
echo ============================================================
start "" "http://localhost:8000/admin"
echo.
echo   提示：后端在另一个窗口运行，需要停止时执行 start_all.cmd stop
echo.
pause
exit /b 0

:startFrontend
echo [附加] 启动前端开发服务器（Vite，端口 8002）...
if not exist "%FRONTEND%\node_modules" (
  echo   首次运行，正在安装前端依赖（需要几分钟）...
  pushd "%FRONTEND%"
  call npm install
  popd
)
start "AnimeSystem 前端 dev（Vite 8002）" cmd /k "cd /d %FRONTEND%&& npm run dev"
echo   前端开发地址: http://localhost:8002
echo   （生产模式请访问 http://localhost:8000/admin）
echo.

echo ============================================================
echo   启动成功
echo     生产模式 : http://localhost:8000/admin
echo     开发模式 : http://localhost:8002
echo     登录账号 : admin / 123456
echo ============================================================
start "" "http://localhost:8000/admin"
echo.
pause
exit /b 0

rem ================= 停止 =================
:doStop
echo 正在停止本项目相关进程...
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
echo [端口占用情况]
netstat -ano | findstr ":8000 :8002 :3306 :6379" | findstr LISTENING
echo.
echo [数据表行数]
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

rem JDK 17：优先用 C:\developtools\jdk17，其次系统 JAVA_HOME，最后 PATH
:setupJava
set "JAVA_HOME="
if exist "C:\developtools\jdk17\bin\java.exe" (
  set "JAVA_HOME=C:\developtools\jdk17"
)
if not "!JAVA_HOME!"=="" (
  set "PATH=!JAVA_HOME!\bin;!PATH!"
  echo   [OK] JDK : !JAVA_HOME!
  exit /b 0
)
where java >nul 2>&1
if errorlevel 1 (
  echo   [FAIL] 未找到 JDK
  exit /b 1
)
echo   [WARN] PATH 中有 java，但未能定位 JDK 17，后端将使用系统 JAVA_HOME
exit /b 0

:setupMaven
where mvn >nul 2>&1
if not errorlevel 1 (
  echo   [OK] Maven 可用
  exit /b 0
)
if exist "C:\developtools\apache-maven-3.9.9\bin\mvn.cmd" (
  set "PATH=C:\developtools\apache-maven-3.9.9\bin;!PATH!"
  echo   [OK] Maven : C:\developtools\apache-maven-3.9.9
  exit /b 0
)
echo   [FAIL] 未找到 Maven
exit /b 1

:setupMysql
where mysql >nul 2>&1
if not errorlevel 1 exit /b 0
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
  set "PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin;!PATH!"
  exit /b 0
)
exit /b 0

:checkDb
if not exist "%ROOT%\.venv\Scripts\python.exe" (
  echo   [提示] 未找到 Python 虚拟环境，双击 setup_venv.cmd 可重建
)
mysql -uroot -p123456 -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='anime_mangage_db' AND table_name='t_user';" 2>nul | findstr 1 >nul
if errorlevel 1 (
  echo   [提示] 数据库 anime_mangage_db 尚未初始化，请执行：
  echo          mysql -uroot -p123456 ^< sql\anime_mangage_db.sql
  echo          mysql -uroot -p123456 ^< sql\dataset_model_schema.sql
  exit /b 0
)
echo   [OK] 数据库 anime_mangage_db 已就绪
exit /b 0

:buildFrontend
pushd "%FRONTEND%"
if not exist "node_modules" (
  call npm install
  if errorlevel 1 (
    popd
    exit /b 1
  )
)
call npm run build
set "RC=%errorlevel%"
if exist "admin" (
  if not exist "%ADMIN_STATIC%" mkdir "%ADMIN_STATIC%"
  xcopy /E /I /Y /Q "admin\*" "%ADMIN_STATIC%\" >nul
)
popd
exit /b %RC%

:showCount
set "TBL=%~1"
for /f "tokens=*" %%c in ('mysql -uroot -p123456 -N anime_mangage_db -e "SELECT COUNT(*) FROM %TBL%;" 2^>nul') do (
  echo    %TBL% = %%c 行
)
exit /b 0

:failEnd
echo.
echo 按任意键关闭本窗口...
pause >nul
exit /b 1
