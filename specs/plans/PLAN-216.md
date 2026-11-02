# PLAN-216：净读破图/点击区/翻页动画

## 步骤

1. **翻页动画 bug**：`ReadBookRouteScreen` LaunchedEffect 依赖去掉 `state.styleConfig.pageAnim`；仅 orientation / doubleHorizontalPage / bookUrl。单测补「用户改 anim 不因同向再触发」。  
2. **手机藏双页**：`GlobalThemePage` 用 `isPad`/`BrowserFormFactor.isTablet` 条件不渲染双页方卡。  
3. **滚动点击区**：新增 `PreferKey.clickActionScroll*` 九键，默认 -1/-1/-1/0/0/0/-1/-1/-1；`ReaderClickActionResolver` 按 `pageAnim==scroll` 选滚动档否则原档；`ReadBookRouteScreen` / `ClickActionConfigSheet` 接入；App 一次迁移写滚动默认。  
4. **破图+尺寸行**：`loadReaderImage` 对 errorBitmap 一律 null；绘制失败不画灰块；正文过滤 regex；必要时 getImageSize 失败不返回 error 尺寸。  
5. **图片点击**：`MoreConfigSheet` 删项；`handleComposeImageClick` 固定双击预览（净视除外）。  
6. 单测 + assemble debug。

## 风险

- 方向联动仍会在旋转时改 scroll↔slide（档 2/3 设计如此）；手动改后同方向不再被打回。
