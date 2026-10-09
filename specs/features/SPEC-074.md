# SPEC-074：标签页点击闪退（RenderThread scale）

| Spec ID | SPEC-074 |
| 状态 | Done |
| 最后更新 | 2026-10-08 |
| Plan | [`../plans/PLAN-074.md`] |
| 需求依据 | 操作者：10.4 点标签页闪退；`_crash3.txt` SIGSEGV |

## 2. 确认

- 2026-10-08「继续修复，修复完构建 app」作为开工确认
- 红色：否

## 3. 为什么做

10.4 点标签计数闪退。堆栈为 RenderThread `computeTransformImpl` 递归栈溢出。071/072 的 scale 过渡在 Home 仍挂 AwContents 时触发；072 仅去掉底层 exit 动画不够。

## 4. 范围

1. 手机 `browserTabsScaleMetadata`：仅 fade + Home `None`（恢复 cab45c7 安全模式，去 scale）。
2. 平板标签 overlay：仅 fade，去 scale。
3. 会话底栏开标签：先导航再截缩略图。

明确不做：改标签页布局；改 073 视口逻辑以外内容。

## 8. 验收

- AC-01：有/无网页时点标签计数不闪退，能进网格。
- AC-02：返回标签页不闪退。
- AC-03：assemble 成功。

## 9. 对话确认结果

- 2026-10-08 继续修复并构建。