# PLAN-175

对应 Spec：[`../features/SPEC-175.md`](../features/SPEC-175.md)
状态：Ready
确认：2026-10-26「确认」。红色问题：无。

## 方案

手机主页搜索框不再在原地获得焦点，点击后盖上一层全屏页。平板仍走 `BrowserNtpScreen` 里现有的焦点和 `BrowserOmniboxSuggestPanel`。

全屏页的列表由纯函数 `BrowserNtpSearchRows.compose` 决定。联想词由 `BrowserSearchSuggest` 生成各引擎地址并解析响应；`BrowserSearchSuggestClient` 用独立的短超时、无磁盘缓存客户端去取，失败或取消时界面只看到空联想词。

进出场不缩放、不淡化液态玻璃控件：遮罩和列表用 Material `motionScheme` 的 effects 规格改变透明度，搜索框只用 spatial 规格改变位置和宽，玻璃字段本身不套 alpha/scale。主页那棵含玻璃的树不被动画透明度。

## 数据

- 未输入：忽略传入的联想词。剪贴板非空则第一行。非无痕时按传入顺序取历史，再补网址不重复的收藏，本机行最多 30 条。
- 已输入：联想词最多 8 条，按去掉首尾空白后的小写文字去重。非无痕时调用现有 `BrowserOmniboxSuggest.match`（收藏优先），显示文字与任一联想词相同则丢掉。
- 无痕：两种情况下都不读历史和收藏。
- 引擎地址：百度 `suggestion.baidu.com`、必应 `api.bing.com`、谷歌 `suggestqueries.google.com`、搜狗 `www.sogou.com`、抖音 `www.douyin.com`、360 `sug.so.360.cn`。抖音公开接口经常无词，按 AC-03 降级。
- 输入文字不写入日志，客户端 `cache(null)`，超时 2.5 秒。打字停 200 毫秒再请求，新输入取消上一次。

## 改动

| 位置 | 改动 | 不动 |
| --- | --- | --- |
| `BrowserNtpSearchRows.kt` | 列表规则、是否全屏 | 平板地址栏 |
| `BrowserSearchSuggest.kt` | 地址与解析 | 搜索结果页地址 |
| `BrowserSearchSuggestClient.kt` | 短超时拉取 | 共用磁盘缓存 |
| `BrowserNtpScreen.kt` | 手机点击进入全屏；平板分支保持 | 第二行芯片、资讯、快捷方式 |
| `PhoneNtpSearchOverlay.kt` | 全屏页与动效 | 网页地址栏 |
| `Symbols.kt` | 右上箭头图标 | 其他图标 |

## 测试

- `BrowserNtpSearchRowsTest`、`BrowserSearchSuggestTest`
- `:app:compileAppDebugKotlin`
- 不 assemble。真机进出场未在本计划中安装。

## 回退

去掉手机全屏入口，手机搜索框恢复原地输入和就地建议。
