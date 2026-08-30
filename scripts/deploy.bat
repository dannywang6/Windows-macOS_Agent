@echo off
REM StudyAgent 发布/部署脚本 (Windows)
REM 把项目 target 下最新的 study-agent.jar 复制到本脚本所在目录(运行目录)。
REM 用法: 先在项目里 mvn clean package 打好 jar，再双击本脚本。
setlocal

REM 本脚本所在目录
set "RUN_DIR=%~dp0"
REM 项目 target 目录（scripts 的上一级）
set "PROJECT_DIR=%~dp0.."
set "JAR_SRC=%PROJECT_DIR%\target\study-agent.jar"

if not exist "%JAR_SRC%" (
    echo 错误: 找不到 %JAR_SRC%
    echo 请先在项目里执行打包: mvn clean package
    pause
    exit /b 1
)

copy /y "%JAR_SRC%" "%RUN_DIR%study-agent.jar" >nul

echo 发布完成。
echo 已复制: %JAR_SRC%
echo   到: %RUN_DIR%study-agent.jar
echo.
echo 下一步:
echo   1. 运行 Double-click  %RUN_DIR%start.bat
echo   2. 自启 Double-click  %RUN_DIR%enable-autostart.bat
pause
