# PLAN-028：浏览器页过渡动画

## 步骤

1. `MainNavGraph`：书签/历史/设置复用 `webViewEntryMetadata`（位移、无 fade）。
2. 标签页专用 metadata：进入 `scaleIn(0.88)+fadeIn`，下层网页 `ExitTransition.None` 或仅 slideOut；返回标签页 `scaleOut+fadeOut`，下层仅 slideIn。
3. `BrowserHomeTabScreen`：手机（及平板内容区）NTP↔session 用 `AnimatedContent` 左右 slide，无 fade。
4. 平板 `TabletSidePanel.Tabs`：缩放进出；收藏/历史/工具箱保持侧滑。
5. LOG；不 assemble。
