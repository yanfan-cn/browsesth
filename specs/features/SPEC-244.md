# SPEC-244：图廊键 44dp · 退出全屏立刻出栏 · Release 独立版本双包

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-244 |
| 状态 | Implemented（待真机） |
| 负责人 | AO |
| 需求依据 | 2026-11-06 确认：无 -beta；aloha+systemWv 同号只进一位；0–9 里程表 |
| 对应 Plan | [`../plans/PLAN-244.md`](../plans/PLAN-244.md) |
| 关联 | SPEC-108/122/222/243；Release 经验（no-configuration-cache、单签名包） |

## 2. 开工前需求确认

- 图廊三键：44dp / 图标 20dp / 间距 4dp。
- 退出全屏：底栏立刻出现，无空白占位。
- Release：`assembleAlohaRelease` + `assembleSystemWvRelease`；不同时打 debug。
- Release versionName：`MAJOR.MINOR.PATCH`（**无 `-beta`**），自 **1.0.0**；同一次双包共用一号，结束后 **+0.0.1**（0–9 里程表）。
- Debug：仍 `version.properties` 的 `x.y.z-beta`，仅 debug assemble 进位。
- 红色：否（版本策略变更已确认）。

## 3. 验收

- [ ] AC-01：图廊三键 44/20/4。
- [ ] AC-02：退出全屏后底栏立即可见，无空白条。
- [x] AC-03：本轮产物 `巡页1.0.0.apk`（aloha）与 `巡页1.0.0-systemwv.apk`；下一 release 起点 1.0.1；debug `version.properties` 未因本轮 release 进位。
- [x] AC-04：两包均签名可装；无 unsigned 残留。
