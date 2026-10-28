# PLAN-176

对应 Spec：[`../features/SPEC-176.md`](../features/SPEC-176.md)
状态：Done（见 LOG-176）
确认：2026-10-26「确认。改完构建」。红色问题：无。

## 方案

下载失败来自两处会把整次任务打成失败的副作用：进度通知抛错会中断写入；公共影片目录建不出来时仍往那里写。通知改为失败只忽略，目录不可写时改存应用自己的影片目录。请求头按大小写取 UA，并丢掉会破坏连接的头。失败胶囊固定文案「下载失败」，叉只清掉播放器上的这条提示。

列表方面，`publish` 只接受 `classifyPlayableUrl` 判定能直接播的地址。YouTube 的 `videoplayback` 仅保留带音视频的 itag（18/22 等），并去掉单次请求才有的 `range`/`rn`/`rbuf`。不再用「地址里有 `/video`」放行。

网页全屏不再等下一帧再把画面挪到窗口上。系统回调里当场加到窗口顶层；没有窗口就立刻 `onCustomViewHidden`，让网页把画面放回去。

## 测试

- `BrowserVideoOfferBusTest`
- 然后 `assembleAppDebug`（操作者要求构建；`--no-configuration-cache`）

## 回退

恢复异步全屏挂载、宽松的 `/video` 列表规则，以及失败胶囊显示原始错误。
