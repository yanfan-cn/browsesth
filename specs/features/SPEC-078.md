# SPEC-078：按 9.1 旧版恢复标签页过渡

| Spec ID | SPEC-078 |
| 状态 | Done |
| 最后更新 | 2026-10-08 |
| Plan | [`../plans/PLAN-078.md`](../plans/PLAN-078.md) |
| 需求依据 | 11.0 仍闪；操作者：按以前版本做 |

## 2. 确认

- 2026-10-08：确认加强方案后，操作者改口「按照以前的版本做」→ 对齐 cab45c7 / SPEC-053
- 红色：否

## 3. 范围

1. 手机 `browserTabsScaleMetadata` 恢复 9.1：**仅 180ms fade**，Home `Exit/Enter None`；**无 scale**。
2. 打开标签：**直接 navigate**，去掉 pause/suppress/post 延迟与 tabs DisposableEffect。
3. 平板全屏标签：改为与手机相同的 180ms fade（旧平板 scale 在玻璃下不安全）。
4. 可删除或停用 `BrowserTabsOverlayChrome` 调用。

## 8. 验收

- AC-01：液态玻璃下有网页时开/关标签不闪退。
- AC-02：有淡入淡出过渡（约 180ms）。