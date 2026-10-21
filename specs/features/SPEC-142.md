# SPEC-142 返回手势跟手 + 设置类页面全量 Overlay

| Spec ID | SPEC-142 |
|---|---|
| 状态 | Ready |
| 创建 | 2026-10-19 |
| Plan | [`../plans/PLAN-142.md`] |
| Log | [`../logs/LOG-142.md`] |
| 前置 | SPEC-139（SlideOverlaySceneStrategy，R1 已覆盖设置四页）、SPEC-088（预见性返回恒关，本 spec 局部推翻） |

## 1. 问题陈述（操作者 2026-10-19 11:31 指示）

1. 「手势返回不用松手才动，滑动马上就响应」——现在所有页面手势返回都要等松手才派发。
2. 「要求其他页面也跟历史/收藏相同的 SlideOverlaySceneStrategy」——设置类页面全部纳入。

NTP 顶栏/内容解耦同时确认：顶栏固定 54dp、内容保持 90dp 下移，两 token 永久独立（仅加注释护栏，无行为改动）。

## 2. 根因（取证）

- **松手才派发**：`BaseActivity.onCreate` 注册了**非预测性** `OnBackInvokedCallback`（转发 `onBackPressedDispatcher.onBackPressed()`）。Android 14 上该垫片使系统按「提交即派发」处理手势，`OnBackAnimationCallback` 的进度事件进不来。
- **overlay 不跟手**：nav3 1.2.0 内置预测返回（`SeekableTransitionState.seekTo(progress, previousScene)`，NavDisplay.kt L740-756）只驱动其主 `AnimatedContent`；overlay 场景渲染在 AnimatedContent 之上，seek 作用不到 → 需场景自接进度事件。

## 3. 方案

1. 删除 `BaseActivity` 垫片：androidx.activity 1.13 的 `OnBackPressedDispatcher` 自行注册系统回调（API 33+ 为 `OnBackAnimationCallback`），既保留 dispatcher 路径又流出进度事件。
2. `SlideOverlaySceneStrategy`：内容接 `PredictiveBackHandler`（仅顶层 RESUMED 场景启用，nav3 把被覆盖 overlay 压到 STARTED）；进度 → `Animatable` → `graphicsLayer.translationX` 跟手右移；提交 → 120ms 补完到 1 → 经元数据注入的 `onBack` 回调弹栈（onRemove 检测 progress≥0.99 跳过重播）；取消 → 150ms 回位。
3. MainNavGraph：`browserSlideOverlayMetadata(onBack)` 注入回调；设置类全量转换（Settings 根、Other/Read/Cover/CoverAlbums/Theme/Backup/Ai/AiSummary/AiPrompt/AiProviderEdit/AiModelEdit/DownloadCache/Translation/LabConfig/Private/CustomTheme/ThemeManage、About）。
4. 阅读/漫画/网页会话**不转 overlay**（内存红线）。网页已有 slide 预测元数据；阅读保持 PredictivePop=None（视觉为即时露出，无 fade，红线不破）。

## 4. 范围与非目标

- 范围：BaseActivity 垫片、SlideOverlaySceneStrategy、MainNavGraph 元数据；NTP token 注释。
- 非目标：阅读/漫画/网页转 overlay；SPEC-141 性能排查（另行推进）；`effectivePredictiveBackEnabled` 其余使用点（播放器/替换规则等行为不变）。

## 5. 产品规则

- 设置类页面手势返回：页面必须**跟手**滑动，松手（提交）后滑出完成、取消则回位；全程 slide-only。
- 顶栏 54dp 与 NTP 内容位移永久解耦。

## 6. 验收标准（AC）

- **AC-01**：设置类页面（浏览器设置等）手势返回全程跟手，无松手延迟感。
- **AC-02**：手势中途松手取消 → 页面回位；提交 → 滑出露出活的底层页。
- **AC-03**：返回键行为不变（280ms slide）；双层设置页（设置→封面→封面管理）逐层正常。
- **AC-04**：阅读/漫画/网页返回不回归（网页跟手滑出为加分项）；私有页生物验证门正常。
- **AC-05**：系统级返回（退出 App 的预测性 home 动画）不受影响；无 SIGSEGV。

## 7. 风险

- 删垫片影响全部 Activity 的返回路径（对话框、无 handler 场景回退到 finish）——需真机回归。
- 阅读页预测 seek 即时露出的观感变化（不转 overlay 的折衷）。
