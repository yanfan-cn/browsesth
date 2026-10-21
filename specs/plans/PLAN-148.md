# PLAN-148 实施计划：工程问题修复与液态玻璃启动提速

关联：SPEC-148。执行顺序即步骤顺序；每步完成即在 LOG-148 登记。

## P1 单测修复（R1）

1. **R1a** `app/build.gradle.kts` `testOptions.unitTests` 块（~205 行，现仅 `isIncludeAndroidResources = true`）加 `isReturnDefaultValues = true`，附注释说明（MainNavigator/BrowserVideoOfferBus 的 JVM 纯单测触 Diag Log）。
2. **R1b** 重写 `app/src/test/java/io/legado/app/ui/book/readaloud/player/ReadAloudPlayerSpeedRangeTest.kt`：
   - 契约依据：`help/readaloud/ReadAloudSpeechRate.kt`（MIN_STORED=2、MAX_STORED=16、sliderSteps=13、toMultiplier=coerce/4、formatLabel 整数 "x.0"/非整数 "%.2f"）。
   - 用例：范围=2..16；steps=13；coerce 下/上界/界内；formatLabel(2)="0.50"、(8)="2.0"、(5)="1.25"、(7)="1.75"、(16)="4.0"、(20) 夹取→"4.0"、(0) 夹取→"0.50"。
3. **R1c** 改 `app/src/test/java/io/legado/app/browser/reader/WebMangaBookFactoryTest.kt`：
   - 契约依据：`WebMangaBookFactory.parseImageCandidates`（对象 {url,w,h} → Candidate）+ `WebMangaImageFilter.passesDimensionAndUrl`（w/h>0 需短边≥240、面积≥240²、横幅比；未知尺寸需 bytes≥8KB）。
   - mangaKind 用例改对象图像（800×1200、900×1300）；unknown 用例图像改对象（600×800）；新增用例「纯字符串 URL 无尺寸字节 → images 为空、candidates 为空」。
4. **R1d** 改 `app/src/test/java/io/legado/app/help/coil/MangaPrefetchRequestsTest.kt`（依据 SPEC-014、代码注释「四个后台任务（SPEC-014 由 2 提升）」）：
   - 用例 1（3 键）：starts==3、closes==3。
   - 用例 2（6 键）：首轮 `keys.take(4)`；reversed 重排后仍 `[k0..k3]`；完成 k0 → `+k5`；全放行 → `[k0,k1,k2,k3,k5,k4]`；再 update → 6。用例 3/4 断言复核（limit 4 下语义不变）。
5. 跑 `:app:testAppDebugUnitTest` 全量，目标 0 失败（GRADLE_USER_HOME 必设 D:\cursor\.gradle-home）。

## P2 仓库卫生（R2）

根 `.gitignore` 追加（锚定根目录，不误伤 specs/）：

```
# 会话临时产物（AI 体检/编译日志/截图等，勿提交）
/_*
/compile*
/tmp-*
/spikes/_tmp-*
/specs/_wip_backup_before_pr1/
```

验证：`git status --porcelain` 中 `tmp-*`/`_crash*`/`compile*`/`_*.txt`/`_*.kt` 消失；`brand/`、`specs/features|plans|logs` 仍在。

## P3 玻璃就绪门闸（R3）

1. 新增 `app/src/main/java/io/legado/app/ui/theme/GlassReadiness.kt`（UTF-8 无 BOM）：`var ready by mutableStateOf(false); private set` + `fun markReady()`，注释写明动机（首帧 15+ 玻璃节点特效链 → 推迟建层）。
2. MainActivity 内容组合处加 `LaunchedEffect(Unit) { withFrameNanos{}; withFrameNanos{}; GlassReadiness.markReady() }`。
3. `BrowserChromeGlass.kt`：三个工厂（browserControlLiquidGlass / browserFloatingChromeGlass / browserReaderFabGlass）的玻璃分支判定处并入 `GlassReadiness.ready`（未就绪走现有 fallback 分支）。
4. `FloatingBottomBar.kt`：三处 `liquidActive` 判定并入 `GlassReadiness.ready`。
5. `TopBarLiquidGlass.kt`：同样并入门闸。
6. 编译 + 全量单测 + 组装 `巡页1.5.3-beta.apk`（assemble 成功后版本自动进位 1.5.4）。

## P5 顶栏与状态栏重叠回归修复（R12，2026-10-20 追加）

1. 根因定位：`AppScaffold` R11 无条件消费 `systemBars.only(Top)`，对无 topBar 的包裹（`MainScreen` 外层、`TabletSettingsScreen`）不具布局等价性 → 内容区子级 `statusBarsPadding()` 失效、顶栏与状态栏重叠（1.5.2-beta 实机截图）。
2. `AppScaffold.kt`：`systemBars.asPaddingValues()` 求 `topCoveredByScaffold = scaffoldPadding.top ≥ 状态栏高度`；悬浮分支门控 `systemBars.only(Top)` / `WindowInsets(0)`；非悬浮分支在 Horizontal+Bottom 基础上同样门控 Top。注释写明等价性成立条件与恒定性论证。
3. 版本纪律：中断 assemble 已进位 1.5.4 → 回写 1.5.3-beta/VERSION_CODE=195（保留 `#Bumped after assemble` 头）→ 全量单测 → `assembleAppRelease` → `巡页1.5.3-beta.apk`。

## P6 收尾

LOG-148（含偏差）；`git status` 复核垃圾文件已隐去；向操作者交付（装机验证项：冷启动体感、玻璃切换无闪烁）。

## 回滚

R1b/c/d 仅动测试文件；R1a/P3/R12 单点可逆；`.gitignore` 追加行可删。无数据/迁移风险。