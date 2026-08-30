import AppKit
import Foundation
import CoreGraphics

// 前台应用名 + 前台窗口标题 + 键盘鼠标空闲秒数，输出一行 JSON:
// {"name":"...","title":"...","idle":74.4}
// 标题与 idle 需要"屏幕录制"权限时不受影响；idle 用 CGEventSource 无需权限。
// idle 含义：距离上次键盘/鼠标输入经过的秒数。

@_silgen_name("CGEventSourceSecondsSinceLastEventType")
func cgEventSourceSeconds(_ stateID: Int32, _ eventType: UInt32) -> Double

struct Info: Codable {
    var name: String = ""
    var title: String = ""
    var idle: Double = 0
}

func currentFrontmost() -> Info {
    var info = Info()

    // 1. 前台应用名（NSWorkspace，可靠，无需权限）
    if let app = NSWorkspace.shared.frontmostApplication {
        info.name = app.localizedName ?? ""
    } else {
        info.name = ""
    }

    // 2. 按前台应用的 PID 从窗口列表里找它的前台窗口标题
    let pid = NSWorkspace.shared.frontmostApplication?.processIdentifier ?? -1

    if let list = CGWindowListCopyWindowInfo([.optionOnScreenOnly], kCGNullWindowID) as? [[String: Any]] {
        // 窗口列表是前到后的 z 序，取第一个匹配 pid、层级 0、有标题的窗口
        for w in list {
            let layer = w[kCGWindowLayer as String] as? Int ?? 0
            let ownerPid = w[kCGWindowOwnerPID as String] as? Int ?? -1
            let title = w[kCGWindowName as String] as? String
            if layer == 0 && ownerPid == pid, let title, !title.isEmpty {
                info.title = title
                break
            }
        }
    }

    // 3. 已空闲秒数（距离上次键盘/鼠标输入）
    //    kCGEventSourceStateCombinedSessionState = 0, kCGAnyInputEventType = 0xFFFFFFFF
    info.idle = cgEventSourceSeconds(0, 0xFFFFFFFF)

    return info
}

let info = currentFrontmost()
let data = try? JSONSerialization.data(withJSONObject: ["name": info.name, "title": info.title, "idle": info.idle])
if let data, let s = String(data: data, encoding: .utf8) {
    print(s)
}
