# SPEC-021：流畅度 + 网页加载 + 地址栏编辑

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-021 |
| 状态 | Done（待真机确认） |
| 优先级 | P0 |
| 创建日期 | 2026-10-04 |
| 负责人 | AO |

## 2. 背景与问题

1. 相对原版 Legado MD3，切换主导航页明显不跟手、动画掉帧。  
2. 5.4 后经常第二次起打不开网页（会话保留后只改状态、不 `loadUrl`）。  
3. 浏览器地址/搜索框无法删除内容、无法重新输入（URL 回写覆盖 draft；平板 `ifBlank` 强制回填）。

## 3. 复述确认

- 操作者确认：2026-10-04「确认」。  
- Agent 复述：修主壳滑动/玻璃采样、会话二次导航 `loadUrl`、地址栏焦点门闩；装机验证。

## 4. 范围

**做：**

1. `MainScreen`：浏览器主页不再整页禁滑；浏览器主页禁用液态玻璃内容采样；邻居预热在无 AwContents 时提高。  
2. `BrowserTabManager` / `BrowserSessionScreen` / `BrowserHomeTabScreen`：会话存活时导航必须触达 `host.loadUrl`。  
3. 地址栏：`addressFocused` 门闩；去掉平板 `ifBlank` 回填；编辑中禁止 `onPageMeta`/`onPageFinished` 覆盖。  
4. `BrowserDrawMode`：尽早 hidden API 豁免；方法缺失时直接走 SOFTWARE 空 DrawFn（避免硬件空绘白屏）。

**不做：** 换核、整包 UI 重做、阅读器/漫画阅读器大改。

## 5. 验收标准

- [ ] AC-01：手机主导航切「书架/发现/我的」跟手，无明显掉帧感（对比原版可接受）。  
- [ ] AC-02：打开网页后回 NTP 再输入新 URL / 书签 / 平板侧栏打开，页面实际加载（非空白/旧页）。  
- [ ] AC-03：地址栏聚焦后可全选删除并输入新内容；未提交前不被网页 URL 写回。  
- [ ] AC-04：冷启动 log：DrawFn 方法可用则 hardware；否则明确 SOFTWARE，无「hardware 空绘」。  
- [ ] AC-05：`assembleAppDebug` + adb 安装成功。

## 6. 依赖

- SPEC-009（玻璃采样禁令）、SPEC-018/019（平板/UI）、SPEC-020（Draw 锁）。
