# PLAN-118

| Plan ID | PLAN-118 |
| Spec | [../features/SPEC-118.md] |
| Log | [../logs/LOG-118.md] |
| Status | Ready |
| Updated | 2026-10-13 |

## Approach

1. Rewrite BookmarksModeTopBar center as weighted Row with start/end padding matching PagePaddingH.
2. colors/themes: splash_screen_background #F6F1EA; Theme.App.Starting uses it.
3. AboutScreen Image on Box background #F6F1EA + ContentScale.Fit.
4. assembleAppDebug GRADLE_USER_HOME.