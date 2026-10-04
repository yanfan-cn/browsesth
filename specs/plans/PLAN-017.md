# PLAN-017：进阅读加速 + A 修复 + 集合 B + 去噪 + 居中状态

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-017 |
| 对应 Spec | [`../features/SPEC-017.md`] |
| 状态 | Done |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | `1772d8b77068dce6d4cae561cc4cb6c1355a8197` |
| 对应 Log | [`../logs/LOG-017.md`] |
| 最后更新 | 2026-10-03 |

## 2. 现状与证据

- 步进：`BrowserSessionScreen.expandMangaChapter` / `openNovelReader` 多轮 `SCROLL_STEP` + delay（可达数十秒）。
- A 入口：`openMangaReader` → `CimocAdapterRegistry.match`；`getComicId` 失败或空 `filter` → 返回 null → **静默**启发式。
- 许多 Parser 的 `UrlFilter` 默认 regex `(\\d+)`，章节路径无数字时 cid=null（如 Ohmanhua）。
- B：官方 `sourceBaseUrl.json` 约 155 键；含 `parseChapterList1` 的富规则约 21；本地无引擎。
- 状态：`showSnack` → 底部 `SnackbarHost`，与悬浮底栏重叠。
- 小说：`WebArticleExtractJs` 取 `innerText`，混入导航控件文案。

```text
点阅读 -> 分析 -> (A miss) -> 步进滚动/翻页 -> 临时书 -> 阅读器
```

## 3. 方案与边界

采用方案：去掉步进环；加强 A 的 host/cid 回退；新增 B JSON 规则引擎（内置 assets + 可选远程刷新）；匹配顺序 A → B → 轻量启发式；阅读状态改居中 Expressive 浮层；小说后处理去噪。

```text
点阅读 -> 居中状态
  -> A match(宽松 cid) -> CimocMangaBridge
  -> B match(JSON 规则) -> JsonMangaBridge
  -> 轻量启发式(无步进) -> 临时书
```

| 模块 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `BrowserSessionScreen` | 去步进；A→B→轻量；居中状态 | 工具箱 sheet |
| `CimocAdapterRegistry` | 宽松 cid / host | Parser 业务逻辑主体 |
| `browser/manga/cimoc/json*` | B 引擎 + bridge | 无图源 UI |
| `WebArticleExtractJs` / 去噪 | UI 文案过滤 | 无书源引擎 |
| docs / SPEC-016 交接 | B 提前 | 宪章不改 |

明确不做：图源商店；小说 Parser；改 CONSTITUTION。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 前置 | 完成证据 |
| --- | --- | --- | --- | --- |
| 1 | SPEC/PLAN/LOG 就位 | specs/ | 确认 | 文件存在 |
| 2 | A 匹配宽松 + 单测 | CimocAdapterRegistry | 1 | 单测 |
| 3 | B JSON 引擎 + assets + 桥接 | manga/cimoc/json | 1 | 单测解析计数 |
| 4 | 入口接线 A→B→轻量；去步进 | BrowserSessionScreen | 2–3 | 代码审查 |
| 5 | 小说去噪 + 单测 | extract / filter | 1 | 单测 |
| 6 | 居中状态 UI | BrowserSessionScreen | 1 | 代码审查 |
| 7 | 文档 + assemble | docs / gradle | 4–6 | APK |

## 5. 测试与验收计划

| ID | AC | 验证 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01 | ripgrep + assemble | 无步进环；APK |
| TC-02 | AC-02 | unit tests | A cid 回退；B≥1 富规则 |
| TC-03 | AC-03 | 代码路径含提示文案 | 降级可见 |
| TC-04 | AC-04 | unit test | 控件短句被滤 |
| TC-05 | AC-05 | UI 代码 | 居中状态非底 Snack |
| TC-06 | AC-06 | 读文档 + APK | 路径与版本 |

## 6. 风险、回退与停止条件

| 风险 | 信号 | 处理 | 回退 |
| --- | --- | --- | --- |
| 远程 JSON 不可达 | 拉取失败 | 用内置 assets | 关远程 |
| 富规则站结构变 | 解析空 | 降级启发式并提示 | 仅 A+启发式 |
| 宽松 cid 误匹配 | 错站 | 校验 host；失败提示 | 收紧规则 |

暂停：扩到图源商店/小说书源/改宪章。

## 7. 完成门禁

- [ ] 步骤有证据；AC 有 TC；LOG 更新；未扩大批准。

## 8. 实际偏差（实施后回填）

- 与计划不同：B 仅内置 assets，未接线远程拉取；小说 catalogHint WebView 跳转取消以换速度。
- 原因与影响：提速优先；富规则仍可从章节页抽图、从父路径猜详情。
- 是否影响 Spec/AC：否（AC 未要求远程热更新）。
- 最终提交：未提交
