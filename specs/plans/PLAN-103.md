# PLAN-103：网页底栏 48dp + 主壳指示 56

| Plan ID | PLAN-103 |
| Spec | SPEC-103 |

## 方案

1. FloatingBottomBar：新增 barHeight（默认 64）、indicatorHeight（默认 56）；硬编码改用参数；主壳调用不变。
2. Symbols.Handyman。
3. BrowserTabCountBadge：可选 borderWidth（默认 1dp）；网页底栏传 size=32、borderWidth=2。
4. BrowserSessionScreen：phoneChromeBarHeight=48；FloatingBottomBar(barHeight=48, indicatorHeight=48)；FAB 48 + 图标 24；工具箱 Handyman；徽章 32/2。
5. 交接：纠正 SPEC-102 误述；记 SPEC-103。

## 顺序

Symbols → FloatingBottomBar → Badge → SessionScreen → Spec/Log/交接。

## 回退

还原参数默认与 Session 调用即可。