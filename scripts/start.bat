@echo off
chcp 65001 >nul
REM StudyAgent 启动脚本 (Windows)
REM 双击本文件即可启动 Agent。
REM 自动切换到本脚本所在目录，查找 java，运行 study-agent.jar。
setlocal

REM 切到脚本所在目录
cd /d "%~dp0"

set "JAR=study-agent.jar"
if not exist "%JAR%" (
    echo 错误: 找不到 %JAR%
    echo 请先把打包好的 study-agent.jar 放到本目录。
    pause
    exit /b 1
)

REM 查找 java：优先 JAVA_HOME，其次 PATH，最后探测常见 JDK 目录
set "JAVA_CMD="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
)
if not defined JAVA_CMD (
    where java >nul 2>nul && set "JAVA_CMD=java"
)
if not defined JAVA_CMD (
    if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "JAVA_CMD=C:\Program Files\Java\jdk-17\bin\java.exe"
    if exist "C:\Program Files\Java\jdk-17.0.18\bin\java.exe" set "JAVA_CMD=C:\Program Files\Java\jdk-17.0.18\bin\java.exe"
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17*\bin\java.exe" set "JAVA_CMD=C:\Program Files\Eclipse Adoptium\jdk-17*\bin\java.exe"
)
if not defined JAVA_CMD (
    echo 错误: 找不到 java.exe。请安装 JDK 17 并设置 JAVA_HOME，或把 java 加入 PATH。
    pause
    exit /b 1
)

echo 启动 StudyAgent ... 按 Ctrl+C 停止
echo Web UI: http://localhost:18080
"%JAVA_CMD%" -Dfile.encoding=UTF-8 -jar "%JAR%"

pause
