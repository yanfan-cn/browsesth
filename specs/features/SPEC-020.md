# SPEC-020：浏览器滑动掉帧 + 苹果顶栏压扁

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-020 |
| 状态 | In Progress |
| 负责人 | AO |
| 目标阶段 | 浏览器绘制稳定性 |
| 需求依据 | 聊天 2026-10-04「修复：1 滚动不跟手掉帧；2 苹果官网顶栏压缩」+ adb 真机证据 |
| 对应 Plan | [`../plans/PLAN-020.md`] |
| 最后更新 | 2026-10-04 |

## 2. 开工前需求确认

- 真机 prefs：`force_software=true`（后手动清为 false）；与 SOFTWARE 掉帧/顶栏压扁历史一致（SPEC-006/011/013）。
- 任务下达者要求：adb 自查并修复上述两点。
- Agent 复述：根因是 DrawFn 失败后永久锁软件绘制；修复为再清一次 sticky 锁、失败仅进程内回退不永久写入、加强苹果顶栏 CSS；装机验证。
- 确认：下达者原文「修复」作为开工确认（2026-10-04）。

### 红色问题判定

- 否（绘制回退策略收紧，不改内核选型）。

## 3. 为什么做

软件层导致滚动掉帧，苹果 sticky 顶栏被纵向压扁；现有 CSS min-height=48 不足，且 sticky `force_software` 会再次锁死硬件。

## 4. 本次范围

1. `BrowserDrawMode`：SPEC-020 一次性清 sticky `force_software`；DrawFn 失败仅进程内回退，**不**再永久写 prefs。
2. 加强 `BrowserSiteCssFixes` 苹果顶栏规则（更高 min-height / 现代 class）。
3. assemble + adb 安装；真机开 apple.com.cn 截图核对。

明确不做：换核；改广告拦截；平板壳结构。

## 5. 验收标准

- [ ] AC-01：冷启动日志 `BrowserDrawMode: init forceSoftware=false`，attach 为 hardware DrawFn（除非当次 DrawFn 失败）。
- [ ] AC-02：网页滚动较修前明显更跟手（真机主观；对照 SOFTWARE）。
- [ ] AC-03：apple.com / apple.com.cn 顶栏高度不明显纵向压扁。
- [ ] AC-04：DrawFn 失败时当次可回退 SOFTWARE 且页面仍可见；重启后仍优先尝试硬件。
- [ ] AC-05：assemble + adb 安装成功。
