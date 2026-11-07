# PLAN-239：主页菜单关闭动画 · 壁纸正方形 1080P

对应 Spec：[`../features/SPEC-239.md`](../features/SPEC-239.md)

## 1. 现状证据

- `BrowserNtpScreen.NtpCustomizePanel`：`Text("背景")`；壁纸横滑 + `NtpWallpaperIds.label`。
- `AnimatedMenuPopup`：仅 `fadeIn`+`scaleIn`；注释写明消失即时；`onDismissRequest` 直接卸组合。
- `drawable-nodpi/ntp_wall_{morning,mist,linen}.jpg`：720×1280；三渐变无位图，`isGradient` 分支画 `Brush.verticalGradient`。
- Logo：凡预置壁纸固定 `NtpDarkWordmark`（深色预置会踩坑）。

## 2. 方案

### 2.1 关闭动画

`AnimatedMenuPopup`：系统 dismiss → 先 `visible=false` 播 `fadeOut`+`scaleOut(0.95)`（90ms）→ 再调用外部 `onDismissRequest`。防重入。

### 2.2 壁纸资产

| id | 名 | 资源 | 明暗 |
| --- | --- | --- | --- |
| grad_blue / green / pink | 蓝天 / 绿茵 / 落樱 | 仍代码竖向渐变（仅改 `label`） | 浅 |
| morning / mist / linen | 晨纸/薄雾/窗纱 | 同名 jpg，重制 1920² | 浅 |
| cyber | 赛博 | `ntp_wall_cyber.jpg` | 深 |
| neon | 霓虹 | `ntp_wall_neon.jpg` | 深 |

- 图片正方形 **1920×1920**：竖屏 9:16 Crop 后短边约 1080；`ContentScale.Crop` 不变；中心约 70% 安全区。
- 保留 `isGradient` 与渐变绘制路径。
- 旧三张照片：居中入方 + 柔边扩展 + 升到 1920。
- 仅新增赛博/霓虹两张位图（JPEG ~85）。

### 2.3 深色预置可读性

`NtpWallpaperIds.isDarkPreset(id)`；`wallpaperScrim` / `shortcutLightText` / logo 色对深色预置走浅色字路径（浅色主题也如此）。

### 2.4 菜单文案

删除 `Text("背景")` 及其后多余 Spacer 若只服务于该标题。

## 3. 改动文件

- `AnimatedMenuPopup.kt`
- `NtpWallpaperStore.kt`（ids、label、builtins、isDarkPreset）
- `BrowserNtpScreen.kt`（面板、图层、logo/scrim）
- `res/drawable-nodpi/ntp_wall_*.jpg`（替换晨纸/薄雾/窗纱 3 + 新增赛博/霓虹 2）

## 4. 验证

| TC | 步骤 | 期望 |
| --- | --- | --- |
| TC-01 | 打开定制菜单 | 无「背景」 |
| TC-02 | 点外侧关闭 | 可见淡出缩放后再消失 |
| TC-03 | 切换八张；平板横竖 | 铺满不变形 |
| TC-04 | 赛博/霓虹 + 浅色主题 | Logo/书签浅色可读 |
| TC-05 | prefs 仍为 grad_blue | 显示蓝天图 |
| TC-06 | assembleAlohaDebug | 成功出包 |

## 5. 回退

还原上述文件与旧 jpg；prefs id 未改名故兼容。
