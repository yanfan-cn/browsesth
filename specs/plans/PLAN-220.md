# PLAN-220

| Plan | PLAN-220 |
| Spec | SPEC-220 |
| 状态 | Done |

## 方案

1. **间距**：手机收球态球与底栏之间保证 `Spacer(4.dp)` 且球本身无额外吃掉间距的负 margin；全宽胶囊收球前用的 16dp 仅展开态。
2. **动画**：`AnimatedContent`（或 size+fade）在 `videoCapsuleCollapsed` 切换时双向 morph。
3. **极致净化**：`strongContentExtract` 默认 `true`；文案改名；`BrowserReaderModeMenu` 增 Switch；手机/平板调用点接线。
4. **Release**：`proguard-rules.pro` 追加 SPEC-154 keep；预检 compile/test；`GRADLE_USER_HOME` + `assembleAppRelease --no-configuration-cache`；apksigner 单包。

## 步骤

| # | 结果 |
| --- | --- |
| 1 | 胶囊 UI |
| 2 | 极致净化 |
| 3 | SPEC-154 |
| 4 | 预检 + release |
