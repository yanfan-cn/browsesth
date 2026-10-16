# PLAN-126：平板顶栏/工具箱/主页滑动与空白

| Plan | PLAN-126 |
| Spec | SPEC-126 |
| 日期 | 2026-10-15 |

## 方案

1. **工具箱**：`Column` 去 `fillMaxHeight`，`Alignment.BottomEnd` + `padding(16.dp)`；`AnimatedVisibility`/`animateEnterExit` 自右滑入+淡入。
2. **顶栏**：`TabletFunctionBar` 重排；omnibox `widthIn(max)` + 中间 `Spacer(weight)`；`onOpenMy`；阅读=`Book2`+Text「阅读」于 `RoundedCornerShape(24.dp)` + `surfaceContainerHighest`。
3. **pager**：`TabletBrowserLayout.pagerUserScrollEnabled` 平板恒 `false`；单测更新。
4. **NTP**：全宽 `verticalScroll`，内层居中 `fillMaxWidth(0.75/0.55)`；去掉过大 `bottom` 占位（改为 ≤16dp 或不加）。
5. **接线**：`BrowserHomeTabScreen`/`MainScreen` 增加 `onOpenMy`→`animateScrollToPage(My)`。