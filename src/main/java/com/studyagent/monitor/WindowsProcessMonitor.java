package com.studyagent.monitor;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

public class WindowsProcessMonitor extends ProcessMonitor {



    public WindowsProcessMonitor() {

    }

    @Override
    public String getActiveWindowTitle() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) {
            return null;
        }
        char[] buffer = new char[1024];
        User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        return Native.toString(buffer);
    }

    @Override
    public String getActiveProcessName() {
        // 获取当前 HWND
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) {
            return null;
        }
        // 获取 PID
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

        // 获取 HANDLE
        HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid.getValue());
        if (hProcess == null) {
            return null;
        }
        // 根据路径提取文件名
        try {
            String fullPath = Kernel32Util.QueryFullProcessImageName(hProcess, 0);
            int index = fullPath.lastIndexOf("\\");
            return index >= 0 ? fullPath.substring(index + 1) : fullPath;
        } finally {
            Kernel32.INSTANCE.CloseHandle(hProcess);
        }
    }
}