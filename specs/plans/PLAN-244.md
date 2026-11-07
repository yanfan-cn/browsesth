# PLAN-244

1. `ReaderImageGalleryDialog`：圆钮 44dp、图标 20dp、间距 4dp。
2. 退出全屏统一 `browserFullscreen=false` + `phoneChromeVisible=true`（FAB / BackHandler）。
3. `app/version-release.properties` 初值 1.0.0 / code 1；`build.gradle.kts`：
   - 仅 release/noR8 assemble → 读/bump release 文件，versionName 无 `-beta`。
   - 仅 debug assemble → 现逻辑。
   - 同命令混 debug+release → 失败提示。
   - 同命令双 release → 配置期只 bump 一次。
   - rename：aloha → `巡页{ver}.apk`；systemWv 目录 → `巡页{ver}-systemwv.apk`。
4. `assembleAlohaRelease assembleSystemWvRelease --no-configuration-cache`；GRADLE_USER_HOME 同盘；apksigner debug.keystore；删 unsigned/.idsig。
