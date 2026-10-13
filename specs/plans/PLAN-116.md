# PLAN-116

| Plan ID | PLAN-116 |
| Spec | [../features/SPEC-116.md] |
| Status | Done |
| Log | [../logs/LOG-116.md] |
| Updated | 2026-10-13 |

## Approach

1. Add ReadingBookmarkOrgStore (prefs): folders + time->folderId.
2. BrowserBookmarksScreen: Tabs-style top bar + BookmarksModeSegmentedControl; show sort/newFolder always; add-favorite only on 鏀惰棌.
3. Filter reading entries by folder; folder rows on 涔︾; enable Move for reading.
4. Rewrite root README.md.
5. assembleAppDebug with GRADLE_USER_HOME; SPEC-108 bump.

## Verify

assemble + static AC checklist.