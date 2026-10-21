# PLAN-158

关联：SPEC-158。日期：2026-10-21。

1. **资源** 把晨纸 / 薄雾 / 窗纱拷入 `res/drawable-nodpi/`（`ntp_wall_morning.jpg`、`ntp_wall_mist.jpg`、`ntp_wall_linen.jpg`）。三张渐变不落图片，用 `Brush.verticalGradient`（上 `#B7D6F2` / `#C9E6D2` / `#F6D5DE`，下白）。
2. **存储** `NtpWallpaperStore.kt`：内置 id（默认 `grad_blue`）写入 `BrowserChromePrefs`；用户图复制到 `filesDir/ntp_wallpapers/`，长边压到 1920，prefs 记 id 列表。提供选择、导入、删除；删除当前选中则回到 `grad_blue`。
3. **主页** `BrowserNtpScreen.kt`：非无痕且每日壁纸关时铺选中背景（图片 `ContentScale.Crop`，渐变铺满）。深色主题遮罩约 0.28；用户图遮罩约 0.32（深色主题下取较高者）。遮罩存在或深色主题时书签名与状态栏图标用浅色。液态玻璃采样层在任一张壁纸可见时打开（与必应壁纸相同，避免玻璃采到空白）。
4. **菜单** `NtpCustomizePanel` 在「每日壁纸」下增加横滑缩略图：六张内置、用户图（角标可删）、末位「从相册添加」（`PickVisualMedia`，不申请存储权限）。
5. **组装** `GRADLE_USER_HOME=D:\cursor\.gradle-home`，`assembleAppDebug`。本包含 SPEC-157 未出包改动。APK 名 `巡页`+versionName。
6. 回填 LOG-158，并更新 `docs/会话状态交接.md` 版本焦点。
