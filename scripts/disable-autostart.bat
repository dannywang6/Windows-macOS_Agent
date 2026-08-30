@echo off
REM 关闭 StudyAgent 开机自启 (Windows)
REM 删除注册表 Run 键中的 StudyAgent 项。
setlocal

REM 删除 HKCU Run 键中的 StudyAgent
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "StudyAgent" /f >nul 2>nul

echo 已关闭开机自启。
echo 若原本未开启，则无影响。
pause
