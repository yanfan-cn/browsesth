# SPEC-121: Find-in-page, reader More, bookmarks sort, tablet toolbox, About open

| Spec ID | SPEC-121 |
| Status | Ready |
| Plan | [../plans/PLAN-121.md] |
| Updated | 2026-10-14 |
| Baseline | SPEC-119 / SPEC-120 |

## Confirm

- 2026-10-14: Operator confirmed restatement (fix SPEC-119 #4/#5 + three extras).

## Scope

1. **页内查找：** 搜索条放在顶栏地址/搜索框正下方（勿贴底被键盘挡）；页面有匹配文本时须正确计数/高亮，禁止误报「无匹配」。
2. **阅读右上角更多：** 必须打开原设置「更多菜单」对应的 `ReaderMoreActionsSheet`；禁止旧 Overflow 下拉或其它菜单顶替。
3. **收藏|书签：** 切到「书签」时去掉「加入收藏」占位，排序键右移，与「新建文件夹」间距 4dp。
4. **平板工具箱：** 独立悬浮卡片（非贴边侧栏），四周 16dp 边距，紧凑布局；取消右上角 X。历史 / 收藏与书签页顶栏高度改为 32dp。
5. **关于→开发人员：** URL 在巡页浏览器内打开；不得只退回「我的」。

## Out of scope

- Chromium/SDK rebuild; unrelated SPEC-120 privacy; commit unless asked.

## AC

- AC-01: 页内查找条在顶栏搜索框下方；键盘弹出时条仍可见可输入。
- AC-02: 页内可见文字可被找到并显示匹配数>0（或当前位置指示），不再对存在内容误报「无匹配」。
- AC-03: 阅读顶栏右上角更多 → `ReaderMoreActionsSheet`；无旧 Overflow 顶替。
- AC-04: 「书签」Tab：排序与新建文件夹相邻且间距 4dp（无 Add 空槽）。
- AC-05: 平板工具箱为四周 16dp 的悬浮卡；无 X；点遮罩或再点工具箱可关。
- AC-06: 历史、收藏与书签顶栏内容行高 32dp。
- AC-07: 关于「开发人员」→ 巡页浏览器加载 GitHub URL；不停留在「我的」空白回退。
