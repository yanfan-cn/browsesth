# PLAN-012：Chrome 风格三页 + 多标签 MVP + Logo

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-012 |
| 对应 Spec | [`../features/SPEC-012.md`] |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main |
| 基线 | VERSION_TENTHS=31；SPEC-011 后 |
| 对应 Log | [`../logs/LOG-012.md`] |
| 最后更新 | 2026-10-02 |

## 2. 方案

1. **多标签**：`BrowserTabStore`（内存+轻量持久）管理 tab 列表；`BrowserHomeTabScreen` 按 activeTab 显示会话；`BrowserTabsScreen` 网格卡片；底栏标签钮进该页；新标签可回 NTP 或空白。
2. **历史/收藏**：重做 UI 为类 Chrome 列表（日期分组历史、图标行收藏）；复用 `BrowseStore`。
3. **NTP**：top 148→168；右上角 IconButton×3；回调导航。
4. **Logo**：`res/drawable` 或 `assets/browser/engines/*.png` 内置六引擎图；`BrowserSearchEngine.logoPainter` 走本地资源；Coil 可作 fallback 但默认本地。

## 3. 执行步骤

| 步骤 | 结果 |
| --- | --- |
| 1 | 内置 logo 资源 + 引擎绑定 |
| 2 | BrowserTabStore + Home/Session 接线 |
| 3 | BrowserTabsScreen + 路由 |
| 4 | 重做 History/Bookmarks UI |
| 5 | NTP 下移 + 右上角入口 |
| 6 | assemble + Log |

## 4. 风险

- 多标签与 `BrowserSessionHolder`：按 tabId 保活或切换时保存 URL；关闭非活动 tab 释放。
- Logo 素材：使用公开品牌 favicon 风格简易矢量/PNG，避免侵权争议的完整商标包时可接受简化官方色标+字标。
