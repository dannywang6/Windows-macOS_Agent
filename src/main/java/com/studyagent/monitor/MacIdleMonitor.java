package com.studyagent.monitor;

public class MacIdleMonitor extends IdleMonitor {

    @Override
    public long getLastInputIdleMillis() {
        MacNativeHelper.Snapshot snap = MacNativeHelper.getSnapshot();
        if (snap == null) {
            return 0;
        }
        return (long) (snap.idleSeconds * 1000);
    }
}
