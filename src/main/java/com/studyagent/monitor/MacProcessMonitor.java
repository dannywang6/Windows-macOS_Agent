package com.studyagent.monitor;

public class MacProcessMonitor extends ProcessMonitor {

    @Override
    public String getActiveWindowTitle() {
        MacNativeHelper.Snapshot snap = MacNativeHelper.getSnapshot();
        return snap == null ? null : snap.title;
    }

    @Override
    public String getActiveProcessName() {
        MacNativeHelper.Snapshot snap = MacNativeHelper.getSnapshot();
        return snap == null ? null : snap.name;
    }
}
