@echo off
chcp 65001 >nul 2>&1
setlocal
cd /d "%~dp0"
set "ROOT=%CD%"
set "BACKEND=%ROOT%/backend"
set "FRONTEND=%ROOT%/frontend"
set "JAVA_HOME=C:/developtools/jdk17"
if exist "%JAVA_HOME%/bin/java.exe" set "PATH=%JAVA_HOME%/bin;%PATH%"
if /i "%~1"=="prod" goto :prodOnly
echo.
echo   正在启动后端服务 (端口 8000) ...
echo.
start "AniScope 后端 - 请勿关闭" cmd /k "cd /d %BACKEND% && mvn -q -Dmaven.test.skip=true compile spring-boot:run"
if not exist "%FRONTEND%/node_modules" (
  echo   前端依赖未安装，正在安装（仅第一次需要）...
  pushd "%FRONTEND%"
  call npm install
  popd
)
echo   正在启动前端开发服务器 (端口 8002) ...
echo.
start "AniScope 前端 - 请勿关闭" cmd /k "cd /d %FRONTEND% && npm run dev"
echo ============================================================
echo.
echo     前端开发地址 :  http://localhost:8002
echo     后台管理地址 :  http://localhost:8000/admin
echo     登录账号     :  admin / 123456
echo.
echo   首次启动后端需要编译，约 30~90 秒，稍等一下再刷新页面。
echo   停止服务：把上面两个窗口直接关掉即可。
echo.
echo ============================================================
echo.
pause
exit /b 0
:prodOnly
echo.
echo   正在启动后端服务 (端口 8000，后端托管已构建好的前端) ...
echo.
start "AniScope 后端 - 请勿关闭" cmd /k "cd /d %BACKEND% && mvn -q -Dmaven.test.skip=true compile spring-boot:run"
echo ============================================================
echo.
echo     后台管理地址 :  http://localhost:8000/admin
echo     登录账号     :  admin / 123456
echo.
echo   首次启动需要编译，约 30~90 秒，稍等一下再刷新页面。
echo   停止服务：把上面那个窗口直接关掉即可。
echo.
echo ============================================================
echo.
pause
exit /b 0
