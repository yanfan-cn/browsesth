# SPEC-100：Expressive 动画与液态玻璃安全（P0）

| Spec ID | SPEC-100 |
| 状态 | Done（P0） |
| 基线 | SPEC-099 之后 |
| 依据 | 操作者确认：P0→P1→P2；P0 后构建；预见性返回继续关 |

## 用户问题

1. 水平滑入滑出：网页→NTP / Nav Pop 进入卡顿、退出几乎无动画。
2. 铺开 M3 Expressive 前须厘清与液态玻璃的冲突。

## P0 范围（本轮）

### 做

1. 网页→NTP：先完成会话右滑退出，再 `destroy` AwContents；进入时 NTP 垫底、会话自右滑入，进入后再卸 NTP（避免空白上硬挂内核）。
2. Nav Pop：浏览器相关路由统一可见右滑退出；缩短过长 480ms；仍禁止对 AwContents 使用 fade/scale。
3. 写入液态玻璃×动画安全表（红线）。
4. assembleAppDebug。

### 不做（P0）

- P1/P2（控件全面接 MotionScheme、Shared Element、填充-only scale）。
- 重开预见性返回；标签页 fade/scale；玻璃 ClearAll。

## 验收

| ID | 前提 | 操作 | 可观察结果 |
| --- | --- | --- | --- |
| AC-01 | 手机打开网页 | 点 Home 回 NTP | 可见会话向右滑出；滑出过程网页未立刻空白 |
| AC-02 | 手机打开网页 | 再进网页 | 进入明显卡顿减轻（相对 P0 前） |
| AC-03 | 打开书签/历史/标签等 | 系统返回 | 可见向右滑出 |
| AC-04 | 工程 | assembleAppDebug | 退出码 0 |

## 回退

还原 TabManager / NtpSessionSwitch / MainNavGraph·MainActivity 转场与文档。