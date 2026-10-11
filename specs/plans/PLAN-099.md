# PLAN-099

| Plan ID | PLAN-099 |
| Spec | SPEC-099 |

## 方案

1. `browserControlLiquidGlass` 增加可选 `overlayAlpha`。
2. `AppModalBottomSheet` 增加可选 `containerColor`（工具箱传 Transparent）。
3. `BrowserToolboxSheet`：透明 Sheet + 内容区玻璃（0.35/0.3）；统一 `ToolboxCell`；间距 12dp；去 Divider。
4. 平板侧栏同玻璃底与 Body。
5. 按键 overlay 提高（浅约 0.68 / 深约 0.72）。
6. 不强制 assemble。

## 步骤

| ID | 步骤 |
| --- | --- |
| T01 | glass overlayAlpha + Sheet containerColor |
| T02 | Toolbox UI |
| T03 | Log |