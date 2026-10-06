# SPEC-035：Material Symbols 替换图标并移除 Miuix

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-035 |
| 状态 | Ready |
| 负责人 | AO |
| 目标阶段 | 阶段 A 浏览壳 / 全 App 视觉 |
| 需求依据 | [`../../docs/UI设计文档.md`] 图标节；聊天 2026-10-04 |
| 对应 Plan | [`../plans/PLAN-035.md`] |
| 最后更新 | 2026-10-04 |

## 2. 开工前需求确认

- 已调查事实：Compose 功能图标来自 `androidx.compose.material.icons`（旧 Material Icons）。底座另有已禁用的 Miuix 控件引擎（`composeEngine`，设置项 `enabled=false`）。
- 疑问处理：已说明 Miuix 与小米系统主题无关；任务下达者要求全部删除 Miuix，图标替换做范围 A。
- Agent 最终复述：全 App Compose 功能图标改为 Material Symbols（Weight 300、Grade 0、Optical Size 24px），按功能选符号；删除全部 Miuix 相关内容；更新 UI 文档图标节；不换网站图标/封面/启动图。
- 任务下达者确认：2026-10-04「把 Miuix 相关内容全部删除。确认只做范围 A」
- 确认后需求变化：在范围 A 之上增加「删除全部 Miuix」

### 红色问题判定

- 是否涉及六类红色问题：否
- 判定理由：视觉资产与去掉已关闭的第二套控件皮肤，不改变产品边界、阅读/浏览主路径或数据隐私。
- 上级决策包：无
- 上级批准转述：无

## 3. 为什么做

旧 Material Icons 与文档目标不一致；用户要求统一 Material Symbols 轴参数。Miuix 分支用户无法开启，却让图标与主题代码分叉，增加崩溃与维护成本。

## 4. 本次范围

要实现：

- Compose 界面中所有 **Material Icons** 换成 **Material Symbols Outlined** 可变字体绘制，轴：wght 300、GRAD 0、opsz 24；未选中 FILL 0，选中/当前 FILL 1。
- 按功能选择符号名（返回、前进、刷新、书签、历史、标签、阅读、搜索、关闭、设置等），不按旧类名硬套。
- 删除 Miuix 依赖、主题引擎分支、Miuix 专用界面与设置项；只保留 Material 控件路径。
- 更新 `docs/UI设计文档.md` 图标节：删除线性描边 1.75；写入上述 Symbols 参数；保留显示尺寸 12–48（4 的倍数）与内间距规则。

明确不做：

- 不替换网站 favicon、书籍封面、启动图、品牌标。
- 不改 XML 里非 Material Icons 的业务矢量图（除非它只是 Miuix 资源）。
- 不引入 SF Symbols 或其他第三方图标库。
- 不借机重做信息架构或页面布局。

## 5. 用户如何使用

1. 打开任意含功能图标的界面（浏览器底栏、书签、历史、书架、阅读、设置等）。
2. 看到同一套线性 Material Symbols，粗细与光学尺寸一致。
3. 当前选中项（如底栏当前页）为实心（FILL 1），其余为空心（FILL 0）。
4. 设置中不再出现「Material Design / Miuix」控件引擎选项；无法再切到 Miuix。

## 6. 产品规则

- 功能图标必须来自 Material Symbols，轴固定为 Weight 300、Grade 0、Optical Size 24px。
- 图标显示边长仍从 12–48dp 按 4 的倍数选取；光学尺寸轴恒为 24，不随显示边长改轴。
- 系统不得再加载或展示 Miuix 控件皮肤。
- 旧备份里若带 `composeEngine=miuix` 或 `useMiuixMonet`，导入后必须按 Material 显示，不得崩溃。
- 系统不得把站点图标或封面换成 Symbols。

## 7. 重要状态与边界

| 情况 | 用户应该看到什么 | 数据或状态如何处理 |
| --- | --- | --- |
| 正常完成 | 全 App 功能图标为 Symbols | 无用户数据迁移 |
| 旧主题备份含 Miuix 字段 | 界面仍为 Material | 忽略 Miuix 字段 |
| 深色模式 | 图标随内容色着色 | 字体着色，不换字形轴 |
| RTL | 返回/前进等方向图标随布局镜像 | 仅方向类符号 |

## 8. 验收标准

- [x] AC-01：任意 Compose 功能图标界面不再引用 `androidx.compose.material.icons` 的 Material Icons。
- [x] AC-02：图标绘制使用 Material Symbols，轴为 wght=300、GRAD=0、opsz=24；未选中 FILL=0，选中 FILL=1。
- [x] AC-03：浏览器主路径图标按功能：搜索 search、返回 arrow_back、前进 arrow_forward、刷新 refresh、书签 bookmark、历史 history、标签 tabs、阅读 menu_book、关闭 close、设置 settings。
- [ ] AC-04：工程内无 `top.yukonga.miuix` 依赖与源码引用；无 Miuix 设置项。（源码已去掉设置项；7.3 包打出前仍带下拉）
- [x] AC-05：`docs/UI设计文档.md` 图标节含 Weight 300 / Grade 0 / Optical Size 24px，且不含「线性描边 1.75」。
- [x] AC-06：`assembleAppDebug` 成功。
- [ ] AC-07：阅读菜单与其它 Compose 功能按钮内，符号落在控件几何中心（不贴上/贴下）。

## 9. 对话确认结果

- 2026-10-04 范围 A；删除全部 Miuix；任务下达者确认。

## 10. Agent 开工自检

- [x] 范围、主流程和关键异常清楚且一致
- [x] 每个目标都有 AC
- [x] 实质不确定性已在聊天确认
- [x] 已复述且已确认
- [x] 确认后变化（删除 Miuix）已写入复述
- [x] 红色问题检查完成
- [x] 不以 Mock 代替完成
- [x] 与宪章及 UI 文档目标一致
