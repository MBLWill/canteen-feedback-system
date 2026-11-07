@echo off
chcp 65001 >nul
cd /d %~dp0canteen-frontend
if not exist node_modules (
  echo 正在安装前端依赖...
  call npm install
)
echo 正在构建前端产物...
call npm run build
echo.
echo 构建完成，产物输出在 canteen-frontend\dist
pause
