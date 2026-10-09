# SPEC-073：网页压缩/错位 + 返回滚动位置 + 滑动跟手

| Spec ID | SPEC-073 |
| 状态 | Done |
| 最后更新 | 2026-10-08 |
| Plan | [`../plans/PLAN-073.md`] |
| 需求依据 | 操作者截图与确认；SPEC-005/020/022/028 |

## 2. 确认

- 2026-10-08 复述后确认；完成后不构建 App
- 红色：否

## 3. 为什么做

部分站点（苹果、百度热搜）出现内容压扁/挤窄与控件错位；返回上一页总到顶部；部分页滑动卡顿不跟手。根因指向 AwContents 宿主 insets 缺失、浮动底栏动态改 WebView 高度、后退时布局注入打乱滚动，以及 DrawFn/触摸路径与参考实现不一致。

## 4. 范围

1. `AwContentsHostView`：补 `onApplyWindowInsets`；触摸顺序对齐参考容器；绘制前保持 scroll 通知。
2. `BrowserSessionScreen`：浮动底栏显隐不再改 WebView 测量高度（叠层，稳定视口）。
3. 后退/前进：标记 history 导航；跳过会触发布局的 CSS/广告隐藏注入；按 URL 记忆并恢复 `scrollY`。
4. 滚动钩子：上报位置供记忆。

明确不做：换内核；`LoadWithOverviewMode`；整包 HardwareView；assemble/install；commit/push。

## 6. 产品规则

- 网页视口高度不随底栏显隐变化。
- History 后退优先保留内核/页面滚动；应用层按 URL 兜底恢复。
- 站点 CSS 修复仅在非 history 导航时注入（或已存在则不重复破坏布局）。

## 8. 验收

- AC-01：apple.com.cn / 百度热搜无明显压扁/挤窄（相对修前与系统浏览器）。
- AC-02：底栏显隐时网页控件不因高度重算而明显错位。
- AC-03：长页滚到中部 → 点链接 → 返回，位置大致恢复（约 ±80px）。
- AC-04：同页滑动相对修前更跟手（主观）；Home 横向滑页不抢竖滑。

## 9. 对话确认结果

- 2026-10-08 确认；不构建。