const AGENT_URL = "http://localhost:9876/tab";

function reportActiveTab() {
    chrome.tabs.query({ active: true, lastFocusedWindow: true }, (tabs) => {
        const tab = tabs[0];
        if (!tab || !tab.url) {
            return;
        }
        fetch(AGENT_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ url: tab.url, title: tab.title })
        }).catch(() => {});
    });
}

chrome.tabs.onActivated.addListener(reportActiveTab);
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (tab.active) {
        reportActiveTab();
    }
});
chrome.windows.onFocusChanged.addListener((windowId) => {
    if (windowId !== chrome.windows.WINDOW_ID_NONE) {
        reportActiveTab();
    }
});