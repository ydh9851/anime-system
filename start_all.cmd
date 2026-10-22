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
echo         番析 AniScope  --  动漫数据分析与推荐系统
echo         一键启动   (模式: %MODE%)
echo ============================================================
echo.
echo   首次启动需要编译，约需 30~90 秒，请勿关闭本窗口。
echo.

if /i "%MODE%"=="stop" goto :doStop
if /i "%MODE%"=="status" goto :doStatus

echo [1/7] 检查 MySQL 与 Redis ...
call :portIsOpen 3306
if errorlevel 1 goto :startMysql
goto :mysqlReady

:startMysql
echo   MySQL 未运行，正在启动 Windows 服务 MySQL80 ...
net start MySQL80 >nul 2>&1
call :sleep 4
call :portIsOpen 3306
if errorlevel 1 goto :mysqlFail
:mysqlReady
echo   [OK] MySQL 已就绪 3306
call :portIsOpen 6379
if errorlevel 1 goto :startRedis
goto :redisReady

:startRedis
echo   Redis 未运行，正在启动 Windows 服务 Redis ...
net start Redis >nul 2>&1
call :sleep 2
call :portIsOpen 6379
if errorlevel 1 goto :redisFail
:redisReady
echo   [OK] Redis 已就绪 6379
goto :step2

:mysqlFail
echo   [FAIL] MySQL 启动失败，请手动启动 MySQL 服务后重试。
goto :failEnd

:redisFail
echo   [提示] Redis 未启动（可选组件，不影响核心功能）
goto :step2

:step2
echo.
echo [2/7] 检查 JDK ...
call :setupJava
if errorlevel 1 goto :failJava
echo.
echo [3/7] 检查 Maven ...
call :setupMaven
if errorlevel 1 goto :failMaven
echo.
echo [4/7] 检查数据库 ...
call :setupMysqlClient
call :checkDb
echo.
echo [5/7] 检查前端页面资源 ...
if exist "%ADMIN_STATIC%\index.html" goto :frontReady
echo   未找到前端构建产物，正在自动构建（需要几分钟）...
call :buildFrontend
if errorlevel 1 goto :failFrontend
:frontReady
echo   [OK] 前端页面资源就绪
echo.
echo [6/7] 清理本项目的旧进程 ...
call :killJava
call :killPort 8000
call :sleep 2
echo   [OK] 清理完成
echo.
echo [7/7] 启动后端服务 ...
if exist "%ROOT%\_boot.log" del "%ROOT%\_boot.log"
echo   正在另开窗口运行 Spring Boot（标题：AnimeSystem 后端服务）
echo   日志写入 _boot.log，窗口可最小化但不要关闭。
echo.
start "AnimeSystem 后端服务（请勿关闭）" cmd /k "set JAVA_HOME=%JAVA_HOME%&& set PATH=%JAVA_HOME%\bin;%PATH%&& cd /d %BACKEND%&& mvn -q -Dmaven.test.skip=true compile spring-boot:run 1> %ROOT%\_boot.log 2>&1"
echo   正在等待后端就绪（最长 180 秒）...
call :waitPort 8000 60
if errorlevel 1 goto :waitFail
echo   [OK] 后端已就绪
goto :done

:waitFail
echo.
echo   [FAIL] 等待超时，后端未能启动。日志末尾 30 行：
echo ------------------------------------------------------------
call :tailLog
echo ------------------------------------------------------------
echo   常见原因：
echo     - 8000 端口被其他程序占用
echo     - MySQL 未启动或数据库未初始化
echo     - 后端窗口里出现了报错，请把那个窗口的内容发给我
goto :failEnd

:done
echo.
echo ============================================================
echo   启动成功
echo.
echo     后台管理页面 : http://localhost:8000/admin
echo     登录账号     : admin / 123456
echo     后端日志     : %ROOT%\_boot.log
echo.
echo   正在打开浏览器 ...
echo ============================================================
start "" "http://localhost:8000/admin"
echo.
echo   提示：后端在另一个窗口运行，需要停止时执行 start_all.cmd stop
echo.
pause
exit /b 0

:doStop
echo 正在停止本项目相关进程 ...
call :killJava
call :killPort 8000
call :killPort 8002
echo [OK] 已停止
pause
exit /b 0

:doStatus
echo [端口占用情况]
netstat -ano | findstr ":8000" | findstr "LISTENING"
netstat -ano | findstr ":8002" | findstr "LISTENING"
netstat -ano | findstr ":3306" | findstr "LISTENING"
netstat -ano | findstr ":6379" | findstr "LISTENING"
echo.
echo [数据表行数]
call :showCount t_user
call :showCount t_video_info
call :showCount t_video_play
call :showCount t_dataset
call :showCount t_train_task
pause
exit /b 0

rem ==================== 子过程 ====================

rem 判断端口是否被占用（管道放在子过程内，避免 for 块内转义层级问题）
:portIsOpen
netstat -ano | findstr ":%~1" | findstr "LISTENING" >nul
exit /b %errorlevel%

rem 等待若干秒。用 ping 而不是 timeout，timeout 在非交互终端会直接报错退出
:sleep
ping -n %~1 127.0.0.1 >nul
exit /b 0

rem 等待端口就绪：参数 1=端口  2=最大尝试次数（每次约 3 秒）
:waitPort
set /a tries+=1
ping -n 3 127.0.0.1 >nul
netstat -ano | findstr ":%~1" | findstr "LISTENING" >nul
if not errorlevel 1 exit /b 0
if %tries% lss %~2 goto waitPort
exit /b 1

rem 结束占用指定端口的进程
:killPort
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%~1" ^| findstr "LISTENING"') do (
  echo   停止占用 %~1 的 PID %%p
  taskkill /PID %%p /F >nul 2>&1
)
exit /b 0

rem 结束本项目残留的后端 java 进程（避免两个 mvn 并发编译损坏 class 文件）
:killJava
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*anime-system*backend*' } | ForEach-Object { Write-Host ('   停止残留后端 PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force }" 2>nul
exit /b 0

rem 打印日志末尾 30 行
:tailLog
if not exist "%ROOT%\_boot.log" (
  echo   没有生成日志文件，后端窗口可能被关闭了。
  exit /b 0
)
powershell -NoProfile -Command "Get-Content '%ROOT%\_boot.log' -Tail 30"
exit /b 0

:setupJava
set "JAVA_HOME="
if exist "C:\developtools\jdk17\bin\java.exe" set "JAVA_HOME=C:\developtools\jdk17"
if not "!JAVA_HOME!"=="" goto :javaFound
where java >nul 2>&1
if errorlevel 1 (
  echo   [FAIL] 未找到 JDK
  exit /b 1
)
echo   [WARN] PATH 中有 java，但未定位到 JDK 17，后端将使用系统 JAVA_HOME
exit /b 0
:javaFound
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo   [OK] JDK : %JAVA_HOME%
exit /b 0

:setupMaven
where mvn >nul 2>&1
if not errorlevel 1 (
  echo   [OK] Maven 可用
  exit /b 0
)
if exist "C:\developtools\apache-maven-3.9.9\bin\mvn.cmd" (
  set "PATH=C:\developtools\apache-maven-3.9.9\bin;%PATH%"
  echo   [OK] Maven : C:\developtools\apache-maven-3.9.9
  exit /b 0
)
echo   [FAIL] 未找到 Maven
exit /b 1

:setupMysqlClient
where mysql >nul 2>&1
if not errorlevel 1 exit /b 0
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin;%PATH%"
exit /b 0

:checkDb
if not exist "%ROOT%\.venv\Scripts\python.exe" echo   [提示] 未找到 Python 虚拟环境，双击 setup_venv.cmd 可重建
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

:failJava
echo   [FAIL] JDK 检查未通过，已中止。
goto :failEnd

:failMaven
echo   [FAIL] Maven 检查未通过，已中止。
goto :failEnd

:failFrontend
echo   [FAIL] 前端构建失败，请到 frontend 目录手动执行 npm install 与 npm run build。
goto :failEnd

:failEnd
echo.
echo 按任意键关闭本窗口 ...
pause >nul
exit /b 1
