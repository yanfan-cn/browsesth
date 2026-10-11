# SPEC-101：转场根因修复（推迟底栏 + Nav Keep/None）

| Spec ID | SPEC-101 |
| 状态 | Done |
| 基线 | SPEC-100 / 13.5 |
| 依据 | 操作者确认：网页→NTP/标签书签历史仍无退出动画、进入卡；根因已说明 |

## 用户问题

SPEC-100 后真机仍：网页→NTP 无退出动画；标签/书签/历史回主页无退出动画；进入很卡。

## 根因（合同）

1. `showHome` 瞬间 `sessionActive=false` → 底栏/Pager 重组掐死 NTP↔会话退出动画。
2. Nav 未用官方「顶层滑出、底层 Keep/None」配方，且进覆盖页时拖动含 AwContents 的 Home。

## 范围

### 做

1. `onSessionActiveChange(false)` 推迟到会话滑出结束（与 release 同点）；true 仍立即。
2. 浏览器覆盖路由（书签/历史/标签/下载/清除数据等）Transition：`slideIn` + `KeepUntilTransitionsFinished`；Pop/PredictivePop：`Enter None` + 向右 `slideOut`。
3. 全局 NavDisplay pop/push 默认同上原则。
4. assembleAppDebug。

### 不做

- 标签 fade/scale；重开预见性返回；P1 MotionScheme 全面铺开。

## 验收

| ID | 操作 | 可观察结果 |
| --- | --- | --- |
| AC-01 | 网页点 Home | 会话向右滑出，滑出过程可见网页 |
| AC-02 | 书签/历史/标签返回 | 顶层向右滑出，露出主页 |
| AC-03 | 进入书签/历史/标签 | 明显轻于「整页带网页一起滑」 |
| AC-04 | assemble | 退出码 0 |