# PLAN-038：图标轴 400、iOS 箭头与浏览底栏按压选中

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-038 |
| 对应 Spec | [`../features/SPEC-038.md`] |
| 状态 | Done |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | 工作区含 034–036 未提交改动 |
| 对应 Log | [`../logs/LOG-038.md`] |
| 最后更新 | 2026-10-06 |

## 2. 现状与证据

- `MaterialSymbolAxes.WEIGHT = 300`（`SymbolIcon.kt`）。
- `Symbols.ArrowBack` / `ArrowForward` 为 `arrow_back` / `arrow_forward`。
- 浏览器底栏：`chromeTab` 默认 0 + `FloatingBottomBar(selectedIndex={chromeTab})` → 后退常显选中；`enableDragSwitch=false` 时无拖按动画，选中靠持久指示器。

```text
chromeTab=0 → FloatingBottomBar 指示器停在后退 → 用户误以为「已选中」
```

## 3. 方案与边界

开工授权继承自 SPEC-038。红色问题：无。

采用方案：改 UI 文档与轴常量；`ArrowBack`→`arrow_back_ios_new`；新增 `ArrowForwardIos` 供浏览器前进；阅读器「上一章」等非返回语义改用保留的 `arrow_back` 符号别名，避免与下一章视觉不对称；`FloatingBottomBar` 增加 `persistentSelection`（默认 true），浏览器传 false 并给 Item 按压反馈。

改动范围：

| 模块或目录 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `docs/UI设计文档.md` | 图标节 + BrowserPage 底栏规则 | 其它页面结构 |
| `icon/SymbolIcon.kt` / `Symbols.kt` / `AppIcons.kt` | WEIGHT 400；返回/前进符号 | 字体文件 |
| `FloatingBottomBar.kt` | `persistentSelection` + Item 按压 | 主页底栏默认行为 |
| `BrowserSessionScreen` / `TabletBrowserChrome` | 前进符号；无持久选中 | 导航逻辑 |
| 阅读器上一章等 | 非返回用途改用 `NavigateBefore` | 下一章仍 `ArrowForward` |

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 前置依赖 | 完成证据 |
| --- | --- | --- | --- | --- |
| 1 | SPEC/PLAN/LOG + UI 文档 | docs/specs | 确认 | 文件内容符合 AC-01 |
| 2 | 轴与符号 | icon 包 | 1 | WEIGHT=400；ligature 正确 |
| 3 | 底栏按压模式 | FloatingBottomBar + Browser | 2 | AC-04 代码意图 |
| 4 | 非返回 ArrowBack 用途分离 | 阅读器等 | 2 | 上一章非 ios_new |
| 5 | assembleAppDebug | Gradle | 3–4 | BUILD SUCCESSFUL |

## 5. 测试与验收计划

| ID | 对应 AC | 验证方法 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01 | 阅读 UI 文档图标节与 §3.2 | 含 400、ios 符号、底栏规则 |
| TC-02 | AC-02 | 阅读 SymbolIcon/Symbols/AppIcons | wght 400；arrow_back_ios_new |
| TC-03 | AC-03 | 阅读 BrowserSession/Tablet chrome | arrow_forward_ios |
| TC-04 | AC-04 | 阅读 FloatingBottomBar + Browser 调用 | persistentSelection=false；Item 按压 |
| TC-05 | AC-05 | GRADLE_USER_HOME=D:\cursor\.gradle-home assembleAppDebug | SUCCESS |

## 6. 风险、回退与停止条件

| 风险 | 触发信号 | 预防/处理 | 回退 |
| --- | --- | --- | --- |
| 字体缺 ligature | 图标空白 | Material Symbols Outlined 含该名；assemble 后真机看 | 改回 arrow_back |
| 底栏按压与主页互相干扰 | 主页无选中 | 默认 persistentSelection=true | 还原参数 |
| iOS 箭头视觉偏心 | 真机不居中 | 沿用 FontMetrics 居中；必要时再加光学偏移 | 加 contentOffset |

## 7. 完成门禁

- [x] 步骤有证据
- [x] AC 均有 TC
- [x] assemble 有结果（7.5）
- [x] 交接文档已更新
