# PLAN-166

对应 Spec：[`../features/SPEC-166.md`](../features/SPEC-166.md)
状态：Ready
确认：2026-10-25「确认」。红色问题：无。

## 现状

- 四项动态 Shortcut 已在 `ShortCuts.kt`，但绘制未按 App 内 `SymbolIcon` 的字体轴，且只在启动时画一次，不跟系统深浅色重画。
- 倍速 `STEPS` 含 1.25（`%.2g` 显示成 1.3×）和 2.5。长按滑动步进为屏宽 / 5。倍速提示是 12dp 文本。
- ExoMedia 在 surface 销毁时 `envelope.release()` 卸掉 Surface 回调，播放器继续走进度，回来没有新画面。`restart()` 在未结束时只 seek，修不好。
- 更多菜单四项纯文字。下载只有 Toast，不返回条目 id。
- 净视只听 `onMediaPlay` / `onHlsDetected`，且 `looksPlayable` 把 `/videoplayback` 里的 `/video` 当成可播，blob 地址被丢掉。YouTube、澎湃走 MSE/blob 时胶囊不出现。
- 网页全屏放在 Compose `Dialog` 里再摘一次 View，容易把网页画面弄丢。`shouldPlayBackgroundVideo()` 为 false。

## 方案

1. Shortcut 用与 `SymbolIcon` 相同的字体轴绘制，圆角按 108dp 图标上的 24dp。颜色读系统夜间模式。配置变化后重注册。绘制失败只记日志，不杀进程。
2. `STEPS` 去掉 1.25 与 2.5。长按步进改为屏宽 / 10。倍速条 24dp，选中项浅 `#F6F1EA` / 深 `#121212`。
3. 离开前台（非画中画）记下进度与是否在播；回到前台新建播放器并跳回该进度。「重新播放」同样新建，从 0 开始。
4. 菜单五项 `leadingIcon`。收藏写入 `BrowseStore`，地址用来源页。
5. `enqueue` 带回 id。播放器右上角卡片订阅该条进度，点击用已有下载路由打开主界面。
6. 资源请求只观察、返回 null。识别 m3u8/mp4/webm/mkv/mpd，以及 YouTube `videoplayback` 的视频流（排除音频和分片）。同一页面保留更高优先级的可播地址（渐进 itag 优先）。另外听 `<video>` 的 http(s) `currentSrc`。
7. 网页全屏改挂到窗口 decor，不再套 Dialog。`shouldPlayBackgroundVideo()` 改为 true。不改内核二进制。

## 测试

- `BrowserVideoSpeedTest`：档位、滑动一步的距离。
- `BrowserVideoOfferBusTest`：音频丢弃、渐进流优先、澎湃 mp4/m3u8、分片丢弃。
- 编译后 `assembleAppDebug`。真机画面、YouTube/澎湃、网页播放器标为未验证（无设备则写入 Log）。
