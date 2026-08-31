@echo off
chcp 65001 >nul
REM 开启 StudyAgent 开机自启 (Windows)
REM 通过写入当前用户的注册表 Run 键实现登录自动启动。
REM 本脚本需要管理员权限(写到 HKLM) 或当前用户(HKCU, 无需管理员)。
setlocal enabledelayedexpansion

REM 切到脚本所在目录
cd /d "%~dp0"

set "LAUNCHER=%~dp0start-hidden.vbs"
if not exist "%LAUNCHER%" (
    echo 错误: 找不到 %LAUNCHER%
    echo start-hidden.vbs 必须与 enable-autostart.bat 放在同一目录。
    pause
    exit /b 1
)

REM 写入 HKCU Run 键（当前用户登录时启动，通过 vbs 隐藏窗口后台运行）
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" ^
    /v "StudyAgent" /t REG_SZ /d "wscript.exe \"%LAUNCHER%\"" /f

echo 已开启开机自启 ^(HKCU Run^)
echo 启动方式: wscript.exe "%LAUNCHER%" ^(隐藏窗口后台运行^)
echo 下次登录 Windows 时会静默自动启动 StudyAgent，不会弹出 cmd 窗口。
echo 下次登录 Windows 时会自动启动 StudyAgent。
echo 若要关闭，双击 disable-autostart.bat。
pause
