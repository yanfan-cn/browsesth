# -*- coding: utf-8 -*-
from pathlib import Path

p = Path(r"D:\cursor\browsesth\docs\会话状态交接.md")
text = p.read_text(encoding="utf-8")

if "## SPEC-118 (2026-10-13)" in text:
    text = text.split("## SPEC-118 (2026-10-13)")[0].rstrip() + "\n"

start = text.index("> 更新日期：")
end = text.index("| **SPEC-111** |")
new_top = """> 更新日期：2026-10-13（**SPEC-112～118 已 commit+push；最后包 1.0.7-beta；下一档 1.0.8 / code 149；真机待装**）
> 用途：跨对话继承。**新会话开始前请先读本文 + `specs/CONSTITUTION.md` + 相关 SPEC/PLAN/LOG。**

## 0. 本轮结论（当前焦点）

**版本（SPEC-108）：** `MAJOR.MINOR.PATCH-beta`；assemble 本包用当前值，结束后 PATCH+1、versionCode+1；APK 名 `巡页`+versionName。

真机优先验：**118**（书签顶栏间距/启动色/关于 Logo）→ **117** → **116** → **115** → **114** → **113**。

本对话主线：**SPEC-112～118** 已落地并提交。**P1/P2 M3 Expressive 未做。** 过渡动画仅研究（安全表限制 slide，未改代码）。

### 0.A 本对话已落地

| Spec | 要点 | 包 |
| --- | --- | --- |
| **SPEC-118** | 书签顶栏 Row+等距 4dp（修分段挡按钮）；splash `#F6F1EA`；关于 Logo 奶油底板+Fit | **1.0.7-beta** 已组装（待装机；下一档 **1.0.8**） |
| **SPEC-117** | 已读=`durChapterTime>0`；默认打开已读+强制显示；网页阅读去 notShelf；快捷书签=百度/B站/热榜/IT之家/澎湃（强制迁移）；百度本地图标 | 并入 1.0.7 |
| **SPEC-116** | 收藏/书签顶栏=标签页式胶囊分段；两 Tab 均可排序/新建文件夹；阅读书签独立文件夹（prefs）；README 公开库文案 | 并入 1.0.7（中途曾打 1.0.6） |
| **SPEC-115** | 软件更新→飞盘；开发人员→`yanfan-cn/browsesth`（已 Public）；重制 launcher/mipmap/foreground；launcher0–6/w 同巡页层 | 并入 1.0.7 |
| **SPEC-114** | 「我的」删文件管理；下载→软件更新→广告→关于；广告仍 Toast「即将推出」 | 并入 1.0.7 |
| **SPEC-113** | 胶囊不自动收缩；音色入口；阅读不自动翻译；右上角更多；收藏|书签分离；加入列表+book | 并入 1.0.7 |
| **SPEC-112** | 阅读顶底栏默认；朗读流光/胶囊设置/语速；TTS PackageManager；平板 MainRouteRequestBus | 并入 1.0.7 |
| """
text = text[:start] + new_top + text[end:]

a = text.index("### 0.2 APK")
b = text.index("### 0.4 协作与构建硬约束")
new_apk = """### 0.2 APK

| 项 | 值 |
| --- | --- |
| 最后成功组装 | **1.0.7-beta**（打完后 props 下一档 **1.0.8** / code **149**） |
| 真机上一已装 | **1.0.1-beta**（108）；**1.0.2～1.0.7** 组装后均未装机（待 adb） |
| 路径 | `upstream/legado-with-MD3/app/build/outputs/apk/app/debug/巡页1.0.7-beta.apk` |
| Git | **112～118** 本轮 commit+push（见 log）；**111** @ `119d481` |
| 崩溃 log | `_tabs_click_log*` / `_crash*` 勿 commit |

### 0.3 真机待验 / 回归（1.0.7-beta）

1. **118（优先）**：书签顶栏分段不挡返回/排序；操作键间距≥4dp；启动页 `#F6F1EA`；关于 Logo 黑字奶油底。
2. **117**：默认打开已读；快捷书签五站+百度本地图标；网页书不 notShelf。
3. **116**：收藏/书签胶囊分段；两 Tab 排序/新建夹；阅读书签文件夹。
4. **115/114**：飞盘更新；开发人员公开库；我的列表项序。
5. **113/112**：胶囊/更多/收藏分流；顶底栏默认；语速。
6. 回归：网页↔NTP / 标签 slide / agefans http 回退（106）。

"""
text = text[:a] + new_apk + text[b:]

i = text.index("| **112** | 阅读顶底栏默认")
j = text.index("\n", text.index("| **113** |", i))
old_evol = text[i:j]
new_evol = (
    "| **112～113** | 阅读顶底栏/朗读；胶囊/更多/收藏|书签分离 | 并入 **1.0.7-beta** |\n"
    "| **114～115** | 我的列表；飞盘更新；公开库；巡页图标全量 | 并入 **1.0.7-beta** |\n"
    "| **116～117** | 书签顶栏/文件夹；已读语义；快捷书签迁移 | 并入 **1.0.7-beta** |\n"
    "| **118** | 书签顶栏等距；splash `#F6F1EA`；关于 Logo 底板 | **1.0.7-beta** |"
)
text = text[:i] + new_evol + text[j:]

c = text.index("## 3. 未决 / 下一会话")
new_tail = """## 3. 未决 / 下一会话

1. **优先**：装 **巡页1.0.7-beta.apk**，真机验 **118→117→116→115→114→113**（见 §0.3）。
2. 若要恢复标签进出动画或手机底「关全部」，保持标签页无液态玻璃采样、慎用 Nav fade；平板设置 L1 / **平板工具箱侧栏**勿再开玻璃或 fade。
3. 标签快照在 GPU 绘制下可能仍是占位。
4. Edge 式页内实时译以后开 Spec；网页译 UI 由 `WEB_PAGE_TRANSLATE_UI_ENABLED` 恢复。
5. NTP「AI 模式」仍是 toast 占位；「我的」广告仍 Toast「即将推出」。
6. 平板「我的」入口未做。
7. 下一 **新** Spec 号从 **119** 起。
8. 工作区仍有未跟踪临时文件（`_crash*`、`tmp-*`、`tools/`、`spikes/` 等），**勿误 commit**。
9. 新 Kotlin/md 必须 UTF-8；折叠壳 / 侧栏 / 标签页：**禁止** fade/scale + 液态玻璃同屏。

## 4. 新会话开工检查清单

1. 读本文 + `CONSTITUTION.md`。动书签/关于/启动色读 **118**；已读/快捷书签读 **117**；收藏顶栏读 **116**；阅读/朗读读 **112/113**。
2. 可装机产物是 **巡页1.0.7-beta.apk**；下一 assemble 为 **1.0.8-beta**（props 已 bump）。
3. 构建设 `GRADLE_USER_HOME=D:\\cursor\\.gradle-home`。
4. 新文件 UTF-8。勿改宪章；勿 commit/push unless asked。下一 Spec **119**。
5. 打开网页勿再调 Cookie/Eyeo JNI。
"""
text = text[:c] + new_tail

text = text.replace(
    "`specs/features/SPEC-087.md`～`113` | SDD 切片（**113** 最新） |",
    "`specs/features/SPEC-087.md`～`118` | SDD 切片（**118** 最新） |",
)
text = text.replace(
    "`specs/plans/PLAN-112.md` / `PLAN-113.md` | 112/113 工程合同 |",
    "`specs/plans/PLAN-112.md`～`PLAN-118.md` | 112～118 工程合同 |",
)
text = text.replace(
    "`specs/logs/LOG-112.md` / `LOG-113.md` | 112/113 实施记录 |",
    "`specs/logs/LOG-112.md`～`LOG-118.md` | 112～118 实施记录 |",
)

p.write_text(text, encoding="utf-8", newline="\n")
print("OK", p.stat().st_size)
raw = p.read_bytes()
assert b"\x00" not in raw[:200]
assert "1.0.7-beta" in text
assert "SPEC-118" in text
print("verify ok")