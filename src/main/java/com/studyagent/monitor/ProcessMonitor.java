package com.studyagent.monitor;

import com.studyagent.analyze.CategoryAnalyzer;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

public class ProcessMonitor {


    private final CategoryAnalyzer categoryAnalyzer;

    public ProcessMonitor(CategoryAnalyzer categoryAnalyzer) {
        this.categoryAnalyzer = categoryAnalyzer;
    }

    public String getActiveWindowTitle() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) {
            return null;
        }
        char[] buffer = new char[1024];
        User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        return Native.toString(buffer);
    }

    public String getActiveProcessName() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) {
            return null;
        }
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

        HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid.getValue());
        if (hProcess == null) {
            return null;
        }
        try {
            String fullPath = Kernel32Util.QueryFullProcessImageName(hProcess, 0);
            int index = fullPath.lastIndexOf("\\");
            return index >= 0 ? fullPath.substring(index + 1) : fullPath;
        } finally {
            Kernel32.INSTANCE.CloseHandle(hProcess);
        }
    }
}