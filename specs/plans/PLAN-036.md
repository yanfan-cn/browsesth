# PLAN-036：平板浏览壳五项优化

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-036 |
| 对应 Spec | [`../features/SPEC-036.md`] |
| 状态 | Done |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | 工作区含未提交 034/035 |
| 对应 Log | [`../logs/LOG-036.md`] |
| 最后更新 | 2026-10-05 |

## 2. 现状与证据

- 当前入口：`TabletBrowserChrome.kt` 键盘 `onGo` 先 `clearFocus`；`BrowserHomeTabScreen.kt` 失焦还原 omnibox。
- 可复用：`TabletBrowserShell` 内容槽；`BrowserTabManager`；`ReadBookRouteScreen` / `MangaReaderRouteScreen`。
- 当前缺口：主页提交竞态；`MainScreen` `userScrollEnabled = !browserSessionActive`；侧栏 `fillMaxWidth(1f/3f)`；阅读走 `MainRouteReadBook` 卸掉壳。
- 已验证命令：无（本次不 assemble）
- 未验证：真机五项

当前流程：

```text
平板 NTP 输入 -> onGo 清焦点 -> 还原空 omnibox -> onSubmit 空串 -> 不加载
平板 NTP 左右滑 -> HorizontalPager -> 书架
阅读模式 -> MainRouteReadBook -> 无浏览器顶栏
```

## 3. 方案与边界

开工授权继承自 SPEC-036。红色问题：无。

采用方案：抽出纯函数 `TabletBrowserLayout` 统一侧栏比例、Pager 禁滑、失焦还原、页眉可见性。顶栏先提交再失焦。平板主页禁 Pager 滑动。阅读/漫画在壳内叠层，不导航离开 Home。页眉仅平板把 mode 0 视为隐藏。

实施后流程：

```text
提交 -> 用当前文字 openUrlInActive/loadUrl -> 再失焦
平板 Home -> Pager 不可滑
阅读模式 -> TabletBrowserShell 内容区叠层，顶栏强制显示
```

改动范围：

| 模块或目录 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `TabletBrowserLayout.kt` | 纯规则 | 无 |
| `TabletBrowserChrome.kt` | 提交顺序、侧栏比例 | 功能键顺序 |
| `BrowserHomeTabScreen.kt` | 提交标志、阅读叠层 | SPEC-033 互斥 NTP |
| `MainScreen.kt` | 平板 Home 禁滑 | 手机 NTP 可滑 |
| `LegacyReaderPageDecorationFactory` | 平板 mode 0 隐藏页眉 | 手机页眉 |
| 测试 | 上述纯函数 | 不 assemble |

## 4. 实施步骤

1. 写入 Spec/Plan/Log 与纯函数 + 单测。
2. 接线顶栏、Pager、侧栏、叠层阅读。
3. 接线页眉规则。
4. 更新交接文档。不 assemble。

## 5. 测试计划

| 编号 | 覆盖的 AC | 类型 | 步骤 | 期望 |
| --- | --- | --- | --- | --- |
| TC-01 | AC-01 | 单元 | 提交中失焦不还原 | false |
| TC-02 | AC-02 | 单元 | 平板 Home 禁滑；手机 NTP 可滑 | 与表一致 |
| TC-03 | AC-03 | 单元 | 竖屏 0.5 / 横屏 1/3 | 相等 |
| TC-04 | AC-04 | 审阅 | 叠层在壳内且 chrome 强制可见 | 代码 |
| TC-05 | AC-05 | 审阅 | 书架仍 `onOpenBookshelfBook` → 全屏路由 | 代码 |
| TC-06 | AC-06 | 单元 | 平板 mode0 隐藏；手机 mode0 跟 hideStatusBar | 相等 |

## 6. 回退

还原本 Spec 文件与上述 Kotlin 改动；不涉及 APK。

## 7. 风险与依赖

- 壳内阅读与独立阅读页共享 ViewModel 键，关掉叠层须 `OnDispose`。
- 叠层内部分二级导航可能弱于独立阅读页。
