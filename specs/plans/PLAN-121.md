# PLAN-121

| Plan ID | PLAN-121 |
| Spec | [../features/SPEC-121.md] |
| Log | [../logs/LOG-121.md] |
| Status | Ready |
| Updated | 2026-10-14 |

## Approach

1. **Find-in-page UI:** Move `BrowserFindInPageBar` from bottom overlay to below top omnibox in `BrowserSessionScreen` / tablet chrome; IME padding via `imePadding` or place in top chrome column.
2. **Find match engine:** Replace fragile `window.find` count loop with TreeWalker + mark/highlight script (or AwContents findAll reflection if present); return real count; next/prev cycle highlights.
3. **Reader More:** Audit all top-right MoreVert / overflow paths; wire only `ShowSheet(MoreActions)`; remove or dead-path old `OverflowDropdownMenu` if still reachable; verify browser ephemeral + local.
4. **Bookmarks sort:** On bookmarks tab (`!showAddFavorite`) omit 40dp Spacer; keep `spacedBy(4.dp)` between sort and new-folder.
5. **Tablet toolbox:** Replace end side-panel Toolbox with centered/end floating card: `Modifier.fillMaxSize().padding(16.dp)` card, compact `BrowserToolboxBody`, no Close IconButton; dim dismiss remains. History/Bookmarks top bar row `height(32.dp)` (and History GlassSmall equivalent).
6. **About open:** On `AboutEffect.OpenUrl`: `BrowserOpenBridge.offer` + navigate `MainRouteHome` (clear/pop to home) instead of sole `onNavigateBack()`.

## Tasks

- T1 SPEC/PLAN/LOG
- T2 Find bar position + match script
- T3 Reader More audit/fix
- T4 Bookmarks sort gap
- T5 Tablet floating toolbox + 32dp top bars
- T6 About → browser home
- T7 assembleAppDebug

## TC

- TC-01↔AC-01 … TC-07↔AC-07
