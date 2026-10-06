# PLAN-040：NTP 双行搜索与主页自定义

## 1. 基本信息

| Plan ID | PLAN-040 |
| Spec | SPEC-040 |
| 状态 | Done |

## 2. 设计方案（frontend-design）

主题：巡页 = 清爽阅读浏览器主页。签名元素 = **双行搜索胶囊**（上输入、下快捷动作，中间细分割线），参考图气质但不照搬 Google 登录/探索。

Token：PageBg `#FCFBF8` / Primary `#243B5A` / Field `#EFEDE8`；图标 Symbols wght 400；圆角搜索容器 ~28dp；无阴影。

避免：紫渐变、奶油衬线模板、独立第二行大 pill 抢主焦点。

## 3. 改动

| 模块 | 改动 |
| --- | --- |
| BrowserNtpPrefs | showBookshelf / showShortcuts |
| BrowserChromePrefs | 壁纸开关沿用；手机也拉壁纸 |
| Symbols | qr_code_scanner |
| BrowserNtpScreen | 双行搜索、自定义菜单、FlowRow 书签、55% |
| UI 文档 §3.1 | 同步 |

## 4. TC

AC-01..06 对应读代码/装机；assemble SUCCESS。
