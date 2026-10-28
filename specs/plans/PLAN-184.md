# PLAN-184

对应 Spec：[`../features/SPEC-184.md`](../features/SPEC-184.md)
状态：Done
确认：操作者 2026-10-27 确认。

## 步骤

1. PowerShell System.Drawing：circle 512 PNG → 192px 覆写 `drawable-xxhdpi/ic_shortcut_xunye.png`。
2. 原图 512px 复制为 `drawable-nodpi/ic_launcher_circle.png`。
3. 重写 `mipmap-anydpi-v26/ic_launcher_round.xml`（背景=ic_launcher_circle 全幅；前景=inset 16.67%；monochrome 保留）。
4. Manifest：application + launcherw/launcher0–6 加 `android:roundIcon="@mipmap/ic_launcher_round"`。
5. `:app:processAppDebugResources` 验证资源链接与 manifest 合并（不 assemble、不进位）。

## 回退

恢复旧 PNG、删除 roundIcon 属性、还原 ic_launcher_round.xml 即回原状。
