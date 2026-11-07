@echo off
chcp 65001 >nul
cd /d %~dp0canteen-backend
echo ============================================
echo  正在启动后端 SpringBoot，端口 8080
echo  首次启动需要下载 Maven 依赖，请耐心等待
echo  启动成功后访问 http://localhost:8080/api
echo ============================================
call mvn spring-boot:run
pause
