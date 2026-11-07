@echo off
chcp 65001 >nul
cd /d %~dp0
echo ============================================
echo  初始化数据库 canteen_feedback（一键全量）
echo  需要本机已安装并启动 MySQL 8.0+
echo ============================================
set PWD=1234
set /p PWD=请输入 MySQL root 密码（直接回车使用默认 1234）:

mysql -uroot -p%PWD% --default-character-set=utf8mb4 < db\full-setup.sql
if errorlevel 1 goto fail

echo.
echo 数据库初始化完成：13 张表、12 个档口、52 道菜品（含配图）、762 条评价、24 条客服知识库。
echo 若 MySQL 密码不是 1234，记得同步修改
echo canteen-backend\src\main\resources\application.yml 中的 spring.datasource.password
pause
exit /b

:fail
echo.
echo 初始化失败，请确认：
echo   1. MySQL 服务已启动
echo   2. root 密码正确
echo   3. mysql 命令已加入系统 PATH
pause
