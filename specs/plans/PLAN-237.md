# PLAN-237：修复导入其他浏览器书签真机必失败（sink 自递归）

| Plan ID | PLAN-237 |
| Spec | SPEC-237 |
| 状态 | Executed（2026-11-05） |

## 步骤

1. **诊断（已完成）**
   - 临时 JVM 诊断：真实 Edge 文件 parse+importTree（内存 sink）通过 → 排除解析层。
   - 临时作用域诊断：匿名对象内无限定 `addBookmark` 自递归 `StackOverflowError`（`outerCalls=0 stackOverflow=true`）→ 根因确凿。两个临时测试已删。
2. **修复**：`BrowseStore.kt` `importSink()` 内 `addBookmark` 调用加 `this@BrowseStore.` 限定。
3. **回归测试**：新增 `BrowseStoreImportSinkTest`（Robolectric，`Application` + sdk 35 基线）：
   - 经 `importSink().addBookmark` 落库可查（直接暴露自递归回归）；
   - `findOrCreateFolder` 同名同父复用/新建；
   - `updateBookmarkToFolder` 更新标题并移文件夹；
   - `importTree` 用真实 sink 端到端跑通（内存小树）。
4. **验证**：跑新增测试 + `BrowserBookmarkHtmlImportTest` + `compileAlohaDebugKotlin`。
5. **出包装机**：`assembleAlohaDebug`（GRADLE_USER_HOME 同盘；本包 2.3.9-beta/281，构建后 bump 2.3.10/282）；apksigner 签名 `巡页2.3.9-beta.apk`，删除未签名副本；adb 安装。
6. **Log**：LOG-237 记录根因、修复、验证与真机结果。

## 不做

- 不给 `runCatching` 加日志以外的行为改动（保持 UI 现状；吞异常静默的提示语已能区分失败点，不在本 Spec 扩面）。
- 不动 `ReadingBookmarkOrgStore`（其 sink 无此问题）。
