# SPEC-187：桌面 Shortcut 不生效根因与修复

状态：Done（操作者 2026-10-27 反馈已装 1.7.8 仍显示旧三项）
日期：2026-10-27
关联：SPEC-164/166（ShortCuts 四入口 + 圆角 24dp 主题底）

## 根因（已查明）

- 工作区 `ShortCuts.kt`（10-25 17:18 起）本就是操作者要求的四项 + Symbols 图标 + 圆角 24dp
  圆角矩形底（浅 #F6F1EA / 深 #121212），1.6.8-beta 起随包发布，intent 接线完整。
- **真凶**：`MainIntent.createLauncherIntent` 产出的 Intent **没有 action**；
  `buildNewTab`、`buildBookshelf` 只 putExtra 未补 action。`ShortcutManager` 要求 shortcut
  intent 必须带 action → `setDynamicShortcuts` 整批校验抛 `IllegalArgumentException` →
  被 `MainActivity` 的 `runCatching` 吞掉（仅 Log.e）。**每次发布都失败** → 桌面永远是
  旧三项（最后阅读/朗读/阅读列表），装机/刷新多少次都无效。
- `buildBookmarks`/`buildScan` 自己补了 action，但四个 shortcut 一次校验，一损俱损。

## 修复

- `buildNewTab` / `buildBookshelf` 的 intent 补 `action = Intent.ACTION_VIEW`（与其余两项一致）。

## 验收

- [ ] AC-01：装机后打开 App 一次，长按图标显示四项新 Shortcut（收藏/书签、新建标签页、扫一扫、阅读列表）。
- [ ] AC-02：图标为对应功能 Symbols + 圆角 24dp 底，浅/深色随系统。
