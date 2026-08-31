@echo off
chcp 65001 >nul
REM 开启 StudyAgent 开机自启 (Windows)
REM 通过写入当前用户的注册表 Run 键实现登录自动启动。
REM 本脚本需要管理员权限(写到 HKLM) 或当前用户(HKCU, 无需管理员)。
setlocal enabledelayedexpansion

REM 切到脚本所在目录
cd /d "%~dp0"

set "START_BAT=%~dp0start.bat"
if not exist "%START_BAT%" (
    echo 错误: 找不到 %START_BAT%
    echo start.bat 必须与 enable-autostart.bat 放在同一目录。
    pause
    exit /b 1
)

REM 写入 HKCU Run 键（当前用户登录时启动）
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" ^
    /v "StudyAgent" /t REG_SZ /d "\"%START_BAT%\"" /f

echo 已开启开机自启 ^(HKCU Run^)
echo 启动脚本: %START_BAT%
echo 下次登录 Windows 时会自动启动 StudyAgent。
echo 若要关闭，双击 disable-autostart.bat。
pause
