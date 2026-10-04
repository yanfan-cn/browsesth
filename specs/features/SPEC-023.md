# SPEC-023：BrowserTheme 全页生效 + 地址栏 URL + Safari 底栏留白

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-023 |
| 状态 | Done（待真机确认） |
| 优先级 | P0 |
| 创建日期 | 2026-10-04 |

## 2. 复述确认

- 操作者要求：① 未编辑时地址栏显示当前页 URL 非旧搜索词；② BrowserTheme 须作用于会话/书签/历史/设置等全部浏览器页；③ 悬浮底栏底部留白严格参考 iOS Safari 截图。  
- Agent：视为确认开工。

## 3. 根因

1. 失焦未恢复 `lastUrl`；`onPageStarted` 未同步 omnibox。  
2. `BrowserTheme` 未提供 `LocalLegadoColorScheme` / `MaterialTheme`，而 `LegadoTheme.colorScheme` 读的是前者。  
3. 底栏 `SpaceMd + navigationBars` 过大，未对齐 Safari ~8dp + 安全区。

## 4. 验收

- [ ] AC-01：打开网页后地址栏为 URL；失焦恢复 URL。  
- [ ] AC-02：浏览器设深色后会话壳/书签/历史/设置均深色。  
- [ ] AC-03：底栏距屏幕底 ≈ Safari（约 8dp + 系统手势条）。  
