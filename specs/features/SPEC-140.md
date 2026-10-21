# SPEC-140 手机 NTP 顶栏下空白收紧（内容下移 90→66dp）

| Spec ID | SPEC-140 |
|---|---|
| 状态 | Ready |
| 创建 | 2026-10-19 |
| Plan | [`../plans/PLAN-140.md`] |
| Log | [`../logs/LOG-140.md`] |
| 前置 | SPEC-129（四键行 54dp、内容下移 90dp）、SPEC-133（safeDrawing 按旋转避让） |

## 1. 问题与确认（操作者 2026-10-19）

手机端浏览器主页（NTP）顶栏区域空白过多。实测（vivo V2157A，480dpi，状态栏 32dp）：

- 顶栏行（Tune/历史/书签/标签数）：32 → 86dp（`NtpTopBarHeight=54dp` 本身已达标）
- 内容列顶：状态栏 32dp + `NtpContentShiftDown=90dp` = **122dp**
- 顶栏行底与内容间空隙 = **36dp** —— 即操作者指的「过多空白」

操作者选定（选项 B）：**顶栏行保持 54dp，缩小顶栏行与内容间空隙**；其余 NTP 组件相对位置不变（内容列整体上移）。

## 2. 方案

- `BrowserUiTokens.NtpContentShiftDown`：**90dp → 66dp**（= 顶栏行 54dp + 12dp 间距）；内容顶 122 → 98dp。
- 仅改该 token：横屏（`phoneLandscape` 本就不加 shift）与平板（不用该 token）不受影响；Logo/搜索/快捷/资讯相对关系不变。

## 3. 验收标准

- **AC-01**：手机竖屏 NTP：资讯卡片（或搜索胶囊，按外观）距顶栏行底约 12dp；无多余空白。
- **AC-02**：横屏、平板 NTP、无痕模式布局无回退。
