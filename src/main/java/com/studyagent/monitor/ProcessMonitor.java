package com.studyagent.monitor;

public abstract class ProcessMonitor {

    public abstract String getActiveWindowTitle();

    public abstract String getActiveProcessName();

    public static ProcessMonitor create() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WindowsProcessMonitor();
        }
        if (os.contains("mac")) {
            return new MacProcessMonitor();
        }
        throw new UnsupportedOperationException("暂不支持的系统: " + os);
    }
}