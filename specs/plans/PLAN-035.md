# PLAN-035：Material Symbols 与移除 Miuix

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-035 |
| 对应 Spec | [`../features/SPEC-035.md`] |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | 1ae2c7a（工作区另有未提交 034） |
| 对应 Log | [`../logs/LOG-035.md`] |
| 最后更新 | 2026-10-04 |

## 2. 现状与证据

- 当前入口：`AppIcons.kt` 在 Miuix 与 `Icons.Default.*` 间切换；大量界面直接 `import androidx.compose.material.icons`。
- 可复用：`AppTheme` 的 `MaterialThemeWrapper` 已是默认路径。
- 当前缺口：无 Material Symbols 字体与统一绘制组件；`AppThemeActual` 在 `isMiuixEngine` 时走 `MiuixThemeWrapper`。
- 依赖：`app/build.gradle.kts` 的 `libs.compose.materialIcons` 与五条 `miuix-*`。

```text
Icon(Icons.Filled.*) / MiuixIcons -> 用户看到旧图标或不可达的 Miuix 皮肤
```

## 3. 方案与边界

开工授权继承自 SPEC-035。红色问题：无。

采用方案：捆绑 Material Symbols Outlined 可变字体；`SymbolIcon` 用 FontVariation 固定轴；语义目录 `Symbols` 按功能命名。删除 Miuix 依赖后只保留各控件的 Material 分支；`composeEngine` / `useMiuixMonet` 从设置模型去掉，读取旧值时忽略。

改动范围：

| 模块或目录 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `res/font` + `ui/widget/components/icon` | 字体、SymbolIcon、Symbols、AppIcons | 站点图标 |
| Compose 界面 | Icons.* → SymbolIcon | 页面结构 |
| `ui/theme`、控件封装 | 去掉 Miuix 分支 | Material 主题与配色 |
| gradle | 去掉 miuix 与 material-icons-extended | 其余依赖 |
| `docs/UI设计文档.md` | 图标节 | 其它 UI 规则 |

明确不做：布局重做；XML 业务矢量大扫除。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 前置依赖 | 完成证据 |
| --- | --- | --- | --- | --- |
| 1 | 文档就绪 | SPEC/PLAN/LOG/UI | 无 | 文件存在 |
| 2 | 字体 + SymbolIcon | icon 包 | 1 | 组件可编译意图 |
| 3 | 替换全部 Material Icons | app/src | 2 | 无 material.icons import |
| 4 | 删除 Miuix | theme/控件/gradle/测试 | 3 或并行 | 无 yukonga.miuix |
| 5 | assembleAppDebug | Gradle | 4 | BUILD SUCCESSFUL |
| 6 | 符号在控件内居中 | SymbolIcon | 5 | 按测得边长绘制并用 FontMetrics 垂直居中 |

## 5. 测试与验收计划

| ID | 对应 AC | 验证方法 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01 | rg `androidx.compose.material.icons` | 无匹配（测试除外若无） |
| TC-02 | AC-02 | 阅读 SymbolIcon 轴常量 | wght 300 GRAD 0 opsz 24 |
| TC-03 | AC-03 | 阅读浏览器 Symbols 映射 | 与 Spec 表一致 |
| TC-04 | AC-04 | rg `yukonga.miuix` 与 gradle | 无匹配 |
| TC-05 | AC-05 | 阅读 UI 文档图标节 | 无 1.75；有三轴 |
| TC-06 | AC-06 | GRADLE_USER_HOME=D:\cursor\.gradle-home assembleAppDebug | SUCCESS |
| TC-07 | AC-07 | 真机打开阅读菜单 | 底栏/工具图标落在圆形按钮正中 |

## 6. 风险、回退与停止条件

| 风险 | 触发信号 | 预防/处理 | 回退 |
| --- | --- | --- | --- |
| 可变字体 API 低版本失败 | 图标空白 | minSdk 上 FontVariation；失败用静态 24px 导出 | 恢复 Icons |
| 漏改 ImageVector 参数 | 编译失败 | 按编译错误补 Symbol 类型 | 同左 |
| 去掉 Miuix 后控件缺口 | 缺 TopBar/Sheet | 只留已有 Material 分支 | git 还原该文件 |

## 7. 完成门禁

- [x] 步骤有证据
- [x] AC 均有 TC
- [x] assemble 有结果
- [x] 未扩大确认范围以外的产品行为（布局除外图标与去 Miuix）

## 8. 实际偏差（实施后回填）

- 与计划不同：Miuix 模糊/混合模式用本地空桩代替；滑动露出区暂不绘制符号。
- 2026-10-05：`SymbolIcon` 改为 Canvas + FontMetrics 居中，避免 ligature 文本在 20dp 按钮里仍按 24sp 绘制。
