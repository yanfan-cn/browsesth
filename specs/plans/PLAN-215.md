# PLAN-215：净读视频占位/过滤 + 壳配色 surface

| Plan ID | PLAN-215 |
| Spec | [`../features/SPEC-215.md`](../features/SPEC-215.md) |

## 步骤

1. `WebArticleVideoThumb`：相邻未标记 http 图 + jingshi → 合并为「第一张 URL + MARKER」；接入 `ensureThumb`；单测。
2. `handleComposeImageClick`：jingshi 且缓存无可用位图 → 不打开播放器（吞掉误点）。
3. `WebArticleChromeFilter`：补短行 + 单测。
4. `VideoPlayerPromptCapsule` 与 `FloatingBottomBar` 容器色 → `surface`。
5. 单测 + assemble debug + 可装机。

## 回退

还原本 Plan 触及文件。
