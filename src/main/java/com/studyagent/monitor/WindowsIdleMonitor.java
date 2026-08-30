package com.studyagent.monitor;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser.LASTINPUTINFO;

public class WindowsIdleMonitor extends IdleMonitor {

    @Override
    public long getLastInputIdleMillis() {
        LASTINPUTINFO info = new LASTINPUTINFO();
        info.cbSize = info.size();
        if (!User32.INSTANCE.GetLastInputInfo(info)) {
            return 0;
        }
        long lastInput = Integer.toUnsignedLong(info.dwTime);
        long tick = Kernel32.INSTANCE.GetTickCount64();
        return Math.max(0, tick - lastInput);
    }
}
