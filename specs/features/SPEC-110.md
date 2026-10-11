# SPEC-110：主页 NTP 顶栏缩高 + 平板去掉多余顶栏占位

| Spec ID | SPEC-110 |
| 状态 | Done |
| 基线 | SPEC-109 |
| 依据 | 确认 1C 2A 3A 4A |
| Plan | [`../plans/PLAN-110.md`] |

## 做

1. **手机 NTP**：仅缩顶栏行高（`NtpTopBarHeight`，54→44dp）；`NtpContentShiftDown=85dp` 不变。
2. **平板 NTP**：去掉内容区多余的 `statusBarsPadding` 与 `NtpContentShiftDown`；壳层标签条+功能栏保留；内容紧贴功能栏下（仅留顶栏控件行高以免压住「自定义」）。
3. **范围**：仅主页 NTP；会话顶栏 / 平板功能栏高度不改。
4. **构建**：`assembleAppDebug` 并 adb 安装。

## 验收

- AC-01：手机主页顶栏行更矮，正文起始位置与改前同（85dp）
- AC-02：平板主页不再出现「状态栏+85dp」叠空白；Logo/搜索更靠上
- AC-03：平板壳层顶栏仍在；网页会话顶栏高度未变
- AC-04：包可安装
