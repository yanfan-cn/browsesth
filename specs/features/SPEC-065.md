# SPEC-065：撤销浅色背景改色（还原 SPEC-063 项 1）

| Spec ID | SPEC-065 |
| 状态 | Done |
| 最后更新 | 2026-10-08 |
| Plan | [`../plans/PLAN-065.md`] |

## 2. 确认

- 2026-10-08 复述后确认
- 红色：否

## 4. 范围

1. 撤销 SPEC-063 将浅色页背景改为 `#F6F1EA` 的全部改动，还原原色值。
2. 同步还原 `docs/UI设计文档.md` 浅色背景 token。

明确不做：不改订阅默认隐藏、不改「更换图标/模糊效果」隐藏；不改深色背景；不改 Logo 深色/无痕 `#F6F1EA`（SPEC-052）。

## 8. 验收

- AC-01：`BrowserUiTokens.PageBgLight` 与设计文档浅色背景为 `#FCFBF8`。
- AC-02：`WHColorScheme` / `wh_theme_background` 为 `#FAFAFA`。
- AC-03：`AppTheme` 不再强制浅色 `background=#F6F1EA`。