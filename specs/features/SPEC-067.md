# SPEC-067：NTP 使用 App ColorScheme

| Spec ID | SPEC-067 |
| 状态 | Done |
| 最后更新 | 2026-10-08 |
| Plan | [`../plans/PLAN-067.md`] |

## 2. 确认

- 2026-10-08 复述后确认；无痕仍独立深色；壁纸层保留
- 红色：否

## 4. 范围

1. 浏览器主页（NTP，非无痕）背景用 `colorScheme.background`。
2. NTP 组件底（搜索胶囊、快捷书签圆底、阅读列表卡等）用 `surfaceContainerLow`（与「我的」选项行一致）。
3. 主/次文字用 `onBackground` / `onSurfaceVariant`（或等价）。

明确不做：改无痕配色；改会话页网页内容区。

## 8. 验收

- AC-01：浅色 NTP 背景与「我的」页背景一致。
- AC-02：NTP 搜索胶囊等组件底与「我的」选项行同色阶。
- AC-03：无痕 NTP 仍为深色无痕样式。