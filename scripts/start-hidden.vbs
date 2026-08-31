' StudyAgent 隐藏窗口启动器 (Windows)
' 用 WScript 静默启动 start.bat，不弹出 cmd 窗口。
' 通过本文件(deploy/enable-autostart)替代直接运行 start.bat。
Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")

' 定位到本 vbs 所在目录
strPath = fso.GetParentFolderName(WScript.ScriptFullName)
strBat = strPath & "\start.bat"

If Not fso.FileExists(strBat) Then
    MsgBox "找不到 start.bat: " & strBat & vbCrLf & "请确认 start.bat 与 start-hidden.vbs 在同一目录。", 16, "StudyAgent"
    WScript.Quit 1
End If

' 隐藏窗口方式启动 start.bat
' 0 = 隐藏窗口
shell.Run """" & strBat & """", 0, False
