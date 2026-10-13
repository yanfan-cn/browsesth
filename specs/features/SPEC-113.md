# SPEC-113: Read-aloud capsule, More menu fix, favorites/bookmarks split

| Spec ID | SPEC-113 |
| Status | Ready |
| Plan | [../plans/PLAN-113.md] |
| Updated | 2026-10-12 |
| Baseline | SPEC-112 |

## Confirm

- 2026-10-12: Agent recap confirmed; C = Material Symbol ook for reading-list / join-list; D option 2 = remove all More* entries from settings Menu (MoreConfig reachable from top-right More panel).

## Scope

### Read-aloud
1. Default capsuleAutoCollapse = false; force-overwrite installed prefs.
2. Research + add voice-switch entry on read-aloud capsule (opens engines/voices management).
3. Keep capsule settings entry.

### Bug
4. Browser utoTranslateOnOpen default false; force-overwrite installed prefs so reader no longer auto-enables translation.

### Labels / icons
5. dd_to_bookshelf user-facing = Join list (加入列表); reading-list and join-list icons = Material Symbol ook (Symbols.Book).

### Reader More (correct SPEC-112)
6. Restore top-right More button on reader title bar.
7. Click opens ReaderMoreActionsSheet (more actions), not full settings.
8. Settings Menu tab: remove BOTH「更多菜单」and「更多设置」entries.
9. 「更多设置」reachable from inside the top-right More panel.

### Favorites / Bookmarks / List
10. Combined bookmarks UI: top tabs 收藏 | 书签 + horizontal swipe.
11. Browser add = 加入收藏 → only in 收藏 (BrowseStore).
12. Reader (incl. local) add bookmark → only in 书签 (Room bookmarks).
13. Join list → only reading-list page (bookshelf).

Out of scope: manga-only UI, commit/push unless asked.

## AC

- AC-01: Fresh/migrated install: capsule auto-collapse off.
- AC-02: Expanded capsule shows voice-switch control; opens engines/voices.
- AC-03: Browser open reader: translation not auto-on (migrated prefs).
- AC-04: Join-list label + ook icon; reading-list chrome uses ook.
- AC-05: Reader top-right More opens MoreActions; settings Menu has no More* rows; MoreConfig openable from More panel.
- AC-06: Bookmarks page tabs/swipe: 收藏 = web only; 书签 = reading only.
- AC-07: Browser add labeled 加入收藏; reader bookmark stays 添加书签.
