# PLAN-016：Cimoc A 集适配 + 小说启发式实施计划

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-016 |
| 对应 Spec | [`../features/SPEC-016.md`] |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main（或实施时开 feature/spec-016） |
| 基线提交 | 实施前记录 `git rev-parse HEAD` |
| 对应 Log | [`../logs/LOG-016.md`] |
| 最后更新 | 2026-10-02 |

## 2. 现状与证据

- 当前入口：`BrowserSessionScreen` 进漫画前 `expandMangaChapter` + `WebTocDiscoverJs`（启发式），见 upstream `BrowserSessionScreen.kt`。
- 可复用：Legado 漫画阅读器、`WebMangaBookFactory` 临时书、OkHttp 栈。
- 当前缺口：无站点 Parser；无 Lazy 页槽模型；TOC 仅扫当前页。
- 旁系：`D:\cursor\Cimoc` — `Parser`/`MangaParser`/`Manga.getComicInfo`/`getChapterImage`/`loadLazyUrl`；`SourceManager` 注册约 44 个网页源。
- 未验证：各源线上是否仍存活（实施中抽样 AC-02）。

```text
浏览器章节页 -> WebView JS 抽图/TOC 启发式 -> 临时书 -> Legado 漫画阅读器
（断点：目录不全、同章页不全）
```

## 3. 方案与边界

- Agent 最终复述：见 SPEC-016 §2/§9。
- 任务下达者确认：2026-10-02「确认。小说先做1」。
- 红色问题：已批准（浏览器路径合入 Cimoc A 解析；非图源商店；B 二期）。
- 本 Plan 不得扩大批准范围（不做 B 引擎、不做小说 Parser、不做图源 UI）。

采用方案：在 browsesth 增加 **MangaSiteAdapter** 层，契约对齐 Cimoc `Parser`（info/chapter/images/lazy）；将集合 A 的 Java Parser **移植或薄封装为 Kotlin**（保留许可证声明）；浏览器用 URL 匹配适配器后走 `Manga.getComicInfo` 等价流程写入临时书；图片列表支持 Lazy，阅读器绑定时解析。未命中与小说走加强启发式。

```text
浏览器 URL
  -> AdapterRegistry.match(url)
       |命中 -> HTTP: info + chapter + images(+lazy)
       |         -> 临时漫画书(TOC+页槽) -> Legado 漫画阅读器
       |未命中 -> 启发式抽图/TOC（漫画）或小说启发式 1
```

改动范围：

| 模块或目录 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `browser/manga/adapter/`（新） | 契约、注册表、A 集 Parser 移植 | 不引 Fresco |
| `BrowserSessionScreen` / 工厂 | 命中适配器时新链路 | 未命中旧路径 |
| 漫画阅读/加载 | Lazy 回调补图 URL | Legado UI 壳 |
| 小说 extract/expand/TOC | 通用启发式 1 | 无站点 Parser |
| `docs/产品与开发决策基线.md` | 红色批准与定位 | 宪章文件不改 |
| Cimoc 旁系仓库 | 只读参考 | 不 submodule 强依赖运行时（代码可拷贝改写） |

明确不做：图源商店；B JSON 规则引擎；小说硬编码源；修改 CONSTITUTION.md。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 前置 | 完成证据 |
| --- | --- | --- | --- | --- |
| 1 | 适配器接口 + 注册表 + URL 匹配 | `browser/manga/adapter` | 无 | 单元测试 match |
| 2 | 核心编排（info/chapter/images/lazy）+ 写入临时书 | fetcher + `WebMangaBookFactory` | 1 | 单测假 HTML |
| 3 | 阅读器 Lazy 补图接线 | manga loader / adapter bridge | 2 | 单测或仪器 |
| 4 | 移植 SourceManager 全部网页 Parser 并注册 | `adapter/sources/*` | 1–2 | AC-01 清单 |
| 5 | 浏览器入口切换命中/降级 | `BrowserSessionScreen` | 2–4 | 手工/日志 |
| 6 | 小说启发式 1 | `WebChapterExpandJs`/`WebTocDiscoverJs`/工厂 | 无强依赖 | AC-04 |
| 7 | 可选 sourceBaseUrl baseUrl 热更新 | prefs + 启动拉取 | 4 | 配置可关 |
| 8 | 更新基线文档 + LOG + assemble | docs/specs | 5–6 | APK 路径 |

并行：步骤 4 可按源分批合入；步骤 6 可与 4 并行。

## 5. 测试与验收计划

| ID | AC | 验证 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01 | 注册表测试列出全部 A 类名 | 与 SourceManager 网页源一一对应 |
| TC-02 | AC-02 | 真机/模拟器打开 ≥3 存活站 | 多章 TOC + 可翻完本章 |
| TC-03 | AC-03 | 打开非 A 站漫画页 | 启发式或明确失败，无崩溃 |
| TC-04 | AC-04 | 小说分页/目录样例 | 正文或 TOC 可观察增益 |
| TC-05 | AC-05 | 导览主界面 | 无新图源商店入口 |
| TC-06 | AC-06 | 读基线文档 + `assembleAppDebug` | 文档有批准记录；APK 产出 |

## 6. 风险、回退与停止条件

| 风险 | 信号 | 处理 | 回退 |
| --- | --- | --- | --- |
| 大量源站已失效 | AC-02 不足 3 站 | Log 记录；用可访问站 + 假 HTML 证架构 | 保留注册与降级 |
| Parser 依赖 Cimoc 专用工具类过多 | 编译失败 | 抽公共 Node/StringUtils 到 adapter 包 | 单源延后须 Log |
| Lazy 与 Legado 图片模型不兼容 | 阅读空白 | 进阅读前解析完非 Lazy；Lazy 站先预解析 N 页 | 临时关 Lazy |
| 许可证 | 合入 Java | 保留 Cimoc 头注释与 NOTICE | 不整包 APK 依赖 |

暂停条件：范围扩到 B 引擎/图源 UI/小说 Parser；宪章修改请求。

## 7. 完成门禁

- [ ] 步骤均有证据；
- [ ] AC 均有 TC；
- [ ] 失败可感知；
- [ ] LOG-016 已建；
- [ ] 未扩大红色批准范围。

## 8. 实际偏差（实施后回填）

- 与计划不同：待填
