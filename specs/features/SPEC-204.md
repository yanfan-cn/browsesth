# SPEC-204：阅读列表顶栏「布局设置」「选择模式」入口

| Spec ID | SPEC-204 |
| 状态 | Implemented（源码；待装包验） |
| 基线 | SPEC-104 |
| 依据 | 操作者确认（2026-10-30）：布局设置与选择模式移出菜单，放在添加本地与菜单键之间 |

## 做

1. 阅读列表顶栏（非编辑模式）：在「添加本地」与 ⋯ 菜单键之间依次新增两个 `TopBarActionButton`：
   - 布局设置：`Symbols.GridView`，CD `R.string.layout_setting` → `ShowOverlay(ConfigSheet)`。
   - 选择模式：`Symbols.Edit`，CD `R.string.selection_mode` → `toggleEditMode()`。
2. 顶栏溢出菜单删除「布局设置」「选择模式」两项；其余菜单项不变。
3. 编辑模式顶栏不改。

## 不做

- 不改布局设置面板、选择模式逻辑、其它入口。
- 本轮不 assemble（除非另说）。

## 验收

- 顶栏顺序：搜索 → 添加本地 → 布局设置 → 选择模式 → ⋯。
- 菜单内无「布局设置」「选择模式」。
- 两键行为与原菜单项一致。
