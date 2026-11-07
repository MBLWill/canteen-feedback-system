@echo off
chcp 65001 >nul
cd /d %~dp0canteen-frontend
if not exist node_modules (
  echo 首次运行，正在安装前端依赖，约 1-3 分钟...
  call npm install
  if errorlevel 1 goto fail
)
echo ============================================
echo  正在启动前端，端口 5173
echo  浏览器打开 http://localhost:5173
echo ============================================
call npm run dev
pause
exit /b

:fail
echo.
echo 依赖安装失败，请确认已安装 Node.js 18+ 且网络可用。
pause
