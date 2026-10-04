# PLAN-020：滑动掉帧 + 苹果顶栏

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-020 |
| 对应 Spec | SPEC-020 |
| 状态 | In Progress |
| 执行人 | AO |
| 最后更新 | 2026-10-04 |

## 2. 现状证据

- adb：`shared_prefs/browsesth_browser_draw.xml` 曾 `force_software=true`。
- SOFTWARE → 掉帧 + 苹果顶栏压扁（SPEC-006/009/011/013）。
- 现有苹果 CSS `min-height: 48px` 偏弱。

## 3. 方案

```text
冷启动 → SPEC-020 清 sticky force_software
DrawFn 失败 → 仅进程内 drawFnFailed=true（不写 prefs）
苹果站 → 更强 CSS + 硬件绘制主路径
```

## 4. 步骤

1. 改 `BrowserDrawMode`（清锁 key + 不永久失败）
2. 改 `BrowserSiteCssFixes` + 单测
3. `GRADLE_USER_HOME=D:\cursor\.gradle-home` assemble + adb install
4. 真机打开 apple.com.cn 截图 + logcat

## 5. 回退

还原 `BrowserDrawMode` / CSS；prefs 可再设 `force_software=true`。
