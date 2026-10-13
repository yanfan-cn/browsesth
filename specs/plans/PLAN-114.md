# PLAN-114: Me list placeholders

| Plan ID | PLAN-114 |
| Spec | [../features/SPEC-114.md] |
| Status | Done |
| Log | [../logs/LOG-114.md] |
| Updated | 2026-10-13 |

## Confirm

Inherit SPEC-114: remove file manage; add software update + ads between download and about; Toast placeholders; no build.

## Evidence

- List was in MyScreen.kt: Download -> File manage -> About.
- Navigation via PrefClickEvent; placeholders need no new route.

## Approach

1. MyScreen: drop file-manage item and FileManageActivity import.
2. Insert Software update / Ads; icons Symbols.Update / Symbols.Block.
3. Click: toastOnUi(R.string.feature_coming_soon).
4. Strings in values + zh-rCN/HK/TW: software_update, ads, feature_coming_soon.

## Verify

- Static order check.
- No assemble (operator request).

## Rollback

Revert MyScreen.kt and added string keys.