# SPEC-096

| Spec ID | SPEC-096 |
| 状态 | 实施中 |
| 基线 | SPEC-095 / 13.2 之后 |
| 依据 | 操作者确认：阅读列表 Book、阅读模式 Book 2；手机网页左上角改 Home 且必回主页 |

## 用户问题

1. 阅读列表 / 阅读模式入口图标需换成 Material Symbols 的 `book` / `book_2`，并在产品入口全局一致。
2. 手机端网页会话顶栏左上角仍像「返回」，且语义易与网页后退混淆；应改为与主页底栏相同的 Home，并保证点击一定回到浏览器主页（NTP）。

## 范围

### 做

- 阅读列表入口图标 → `Symbols.Book`（ligature `book`）
- 阅读模式入口图标 → `Symbols.Book2`（ligature `book_2`，需在 Symbols 中新增）
- 覆盖：平板顶栏、手机网页阅读 FAB、主导航底栏「阅读列表」项
- 手机网页顶栏左上角：图标 `Symbols.Home`；点击回调保证回 NTP（不走 WebView history.back）

### 不做

- 不改系统返回键 / 底栏「后退」的网页历史行为
- 不改非「阅读列表/阅读模式」业务里对 Book / LibraryBooks / AutoStories 的其它用法
- 不采用此前自定义圆角 / CollectionsBookmark / ChromeReaderMode 方案

## 产品规则

1. 阅读列表可见入口统一 `book`。
2. 阅读模式可见入口统一 `book_2`。
3. 手机网页顶栏左键视觉与主页底栏 Home 一致；点击后当前标签回到 NTP（`showHome` / 清空会话 URL 展示），用户看到浏览器主页而非上一浏览页。

## 验收标准

| ID | 前提 | 操作 | 可观察结果 |
| --- | --- | --- | --- |
| AC-01 | 平板浏览器顶栏可见 | 目视阅读列表 / 阅读模式图标 | 分别为 Book / Book 2 |
| AC-02 | 手机网页会话 | 目视底栏旁阅读 FAB；底栏「阅读列表」 | FAB=Book 2；底栏阅读列表=Book |
| AC-03 | 手机打开任意网页 | 点顶栏左上角 Home | 图标为 Home；进入 NTP/主页，非 WebView 后退 |
| AC-04 | 工程可构建 | assembleAppDebug | 退出码 0 |

## 回退

还原本 Spec 触及的 Symbols / Chrome / Session / AppIcons 改动。
