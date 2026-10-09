# SPEC-075：荣耀15标签闪退 + NTP椭圆 + 底栏固定

| Spec ID | SPEC-075 |
| 状态 | Done |
| 最后更新 | 2026-10-08 |
| Plan | [`../plans/PLAN-075.md`] |
| 需求依据 | 操作者确认；Vivo 无闪退、荣耀 Android15 闪退；须保留过渡动画 |

## 2. 确认

- 2026-10-08：标签页必须有过渡（参考前几版 SPEC-028）；压扁/底栏固定确认
- 红色：否

## 3. 范围

1. 标签过渡恢复 SPEC-028：tabs 层 scale+fade（0.88/420ms），Home `Exit/Enter None`；开标签前 `pauseForTabsOverlay`（pause+INVISIBLE），退出 resume。
2. NTP 圆形控件液态玻璃关闭 lens（防椭圆压扁）。
3. 浏览器底栏恢复固定预留高度（不随显隐改 WebView 尺寸、不叠在网页上）。

## 8. 验收

- AC-01：荣耀 Android15 点标签不闪退，且有缩小/放大过渡。
- AC-02：NTP 快捷图标为正圆。
- AC-03：浏览底栏下方为预留区，网页不钻到底栏下。