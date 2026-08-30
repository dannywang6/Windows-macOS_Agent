package com.studyagent.monitor;

public abstract class IdleMonitor {

    public abstract long getLastInputIdleMillis();

    public static IdleMonitor create() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WindowsIdleMonitor();
        }
        if (os.contains("mac")) {
            return new MacIdleMonitor();
        }
        throw new UnsupportedOperationException("暂不支持的系统: " + os);
    }
}
