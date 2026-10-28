# SPEC-184：快捷书签巡页图标换圆形版 + 桌面图标圆形自适应

状态：Done（操作者 2026-10-27 确认）
日期：2026-10-27
关联：SPEC-115（launcher/mipmap 体系）、SPEC-117/160（快捷书签）

## 开工确认

- 复述：①浏览器主页快捷书签「巡页」（id=xunye）图标由方版 `ic_shortcut_xunye.png` 换为
  `C:\Users\32082\Documents\browsesth-logo-512-circle.png`（同设计圆形版）。②桌面图标当前
  manifest 未声明 `roundIcon`；圆形启动器需自动用圆形版 → application 与全部 launcher 别名
  （launcherw/0–6，本就同图）加 `android:roundIcon="@mipmap/ic_launcher_round"`，并把
  anydpi-v26 `ic_launcher_round.xml` 改为：背景层=圆形 PNG 全幅（深蓝盘内遮罩圆不露边角）、
  前景层=同 PNG 内缩至遮罩圆（72/108，inset 16.67%），圆形遮罩下呈现即该图原样。monochrome 保留。
- 红色问题：否。资源与 manifest 改动；minSdk 26，旧 round webp 不再被使用，不动。

## 范围

- `drawable-xxhdpi/ic_shortcut_xunye.png`：以 circle 图重制（缩放至 ~192px）。
- 新增 `drawable-nodpi/ic_launcher_circle.png`（512px 原图）。
- `mipmap-anydpi-v26/ic_launcher_round.xml` 重写为上述自适应定义。
- `AndroidManifest.xml`：application + 8 个 activity-alias 加 `android:roundIcon`。

明确不做：不动 `ic_launcher` 主 icon 与各别名 `icon`（方形/默认遮罩仍走原层）；不动旧 webp。

## 验收

- [ ] AC-01：主页快捷书签「巡页」显示圆形新图。
- [ ] AC-02：圆形遮罩启动器（如 Pixel 默认）桌面显示圆形版；非圆遮罩启动器仍显示原 icon。
- [ ] AC-03：资源链接/manifest 合并无错（`processAppDebugResources` 验证；出包随下一 assemble）。
