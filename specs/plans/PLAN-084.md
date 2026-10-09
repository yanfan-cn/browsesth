# PLAN-084

1. `BrowserNtpPrefs`：`newsFeedEnabled`（默认 true）；`newsSourceThePaper/BbcZh/Sspai`（默认 true）。
2. `BrowserNtpNews`：拉取三源 RSS/公开列表，解析标题+链接+时间，合并排序去重。
3. `BrowserNtpScreen`：总开关开时在快捷区下方展示列表；点击 `onOpenUrl`。
4. `NtpCustomizePanel`：总开关 + 三源子开关（总关时子项禁用或隐藏）。
5. 失败降级：空列表或轻提示，不崩溃。
6. 单元测试：prefs 默认值；RSS 解析样例。