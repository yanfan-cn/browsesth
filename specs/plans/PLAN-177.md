# PLAN-177

对应 Spec：[`../features/SPEC-177.md`](../features/SPEC-177.md)
状态：Ready
确认：2026-10-26「确认」。红色问题：无。

## 方案

底栏：与会话页同一套路，从 `BrowserNtpScreen` 上报「全屏搜索进行中」，经 `BrowserHomeTabScreen` 到 `MainScreen`，与 `browserSessionActive` 一并决定是否隐藏主界面底栏。

搜索记录：本机 SharedPreferences 存最近查询词（带时间，去重置顶）。提交搜索或点联想词时写入；打开收藏/历史/剪贴板网址不写。未输入列表改为剪贴板 + 最多 5 条记录。设置开关放在浏览器设置「浏览」组，默认开。清除「历史记录」时一并清搜索记录。

横屏：全屏搜索结束态宽度改为 `rootWidth * 0.75`，水平居中；竖屏仍为左右页边距内满宽。

## 测试

- 更新 `BrowserNtpSearchRowsTest`（空查询改为搜索记录规则）
- 新增搜索记录 store 单测（可选）

## 回退

恢复空查询的浏览历史/收藏列表、底栏不因搜索隐藏、横屏满宽。
