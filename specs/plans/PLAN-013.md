# PLAN-013：百度误拦/UA、苹果顶栏绘制、加载提速

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-013 |
| 对应 Spec | [`../features/SPEC-013.md`] |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main |
| 基线 | SPEC-012 后；真机见百度栏缺失、缩略图失败、苹果顶栏压扁 |
| 对应 Log | [`../logs/LOG-013.md`] |
| 最后更新 | 2026-10-02 |

## 2. 现状证据

- `BrowserAdRules` + `assets/browser/adblock_rules.txt`：含 `/ads?`、`adsystem` 等宽子串，易误伤百度资源。
- UA：`…Chrome/150… Browsesth/0.4`，站点可能降级旧版 SERP（缺现代功能栏）。
- `BrowserDrawMode`：DrawFn 一次失败即 prefs 永久 `force_software`；SOFTWARE 与苹果 sticky 顶栏压扁相关（SPEC-006/009/011）。
- 截图对照：Browsesth 百度为旧式「百度一下」壳且无 Ai+/综合栏；参考为现代功能栏+缩略图齐全。

## 3. 方案

1. **广告规则**：删除 `/ads?`、裸 `adsystem`；保留 `amazon-adsystem.com`、`doubleclick`、`googlesyndication` 等明确域名；`shouldBlock` 对百度系首方 host（baidu.com / bdstatic.com / bcebos.com 等）默认放行（探针标记除外）。
2. **UA**：`getUserAgent()` 改为标准 Chrome Mobile 150，无 Browsesth 后缀。
3. **绘制**：增加 `clearForceSoftware`（升级/本 Spec 首次 attach 时清一次锁）；DrawFn 失败仍可回退但不在同一进程无限重试；`onPageFinished` 对 apple.com / apple.com.cn 注入顶栏 `min-height` CSS。
4. **速度**：attach 时确保图片自动加载；硬件优先；误拦减少即减少破图与脚本失败重试。

## 4. 执行步骤

| 步骤 | 结果 |
| --- | --- |
| 1 | 改 adblock_rules + BrowserAdRules 白名单/DEFAULT |
| 2 | UA 去后缀 |
| 3 | BrowserDrawMode 清锁 API + 首次重试 |
| 4 | 苹果顶栏 CSS 注入接线 |
| 5 | 单元测 ad 规则 + assemble + LOG |

## 5. 风险与回退

| 风险 | 回退 |
| --- | --- |
| 百度广告位增多 | 仅加精确百度广告 host，不恢复宽 `/ads?` |
| DrawFn 仍失败 | 保持 SOFTWARE；CSS 兜底苹果栏 |
| 清锁导致空白帧 | 日志可观察；用户再进页即 SOFTWARE |
