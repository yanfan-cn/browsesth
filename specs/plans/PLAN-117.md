# PLAN-117

| Plan ID | PLAN-117 |
| Spec | [../features/SPEC-117.md] |
| Status | Done |
| Log | [../logs/LOG-117.md] |
| Updated | 2026-10-13 |

## Approach

1. BookshelfSettings.saveTabPosition default IdReadFinished; App.kt migrate saveTabPosition.
2. BookDao IdReadFinished queries/counts/previews: durChapterTime > 0.
3. WebArticleBookFactory / WebMangaBookFactory: drop BookType.notShelf.
4. BrowserShortcutStore new defaults + migrateShortcutsSpec117; App.kt call migrate.
5. BrowserShortcut.localIconRes + NTP FaviconBubble prefer local painter for baidu.
6. Refresh drawable ic_engine_baidu from Pictures if needed.

## Verify

Static; no assemble (D2).