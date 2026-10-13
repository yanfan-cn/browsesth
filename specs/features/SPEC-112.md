# SPEC-112: Reader UI defaults and read-aloud tweaks

| Spec ID | SPEC-112 |
| Status | Done |
| Updated | 2026-10-12 |
| Plan | [`../plans/PLAN-112.md`] |

## Confirm

- 2026-10-12 confirmed: bottom bar 2 rows (row2 fixed 5 actions); force-overwrite installed prefs.
- Red issues: none.

## Scope

Reader: titleBarMode=title-only + title capsule; icon labels on; 2-row bottom bar (row1: search/auto/catalog/aloud/setting; row2: bookmark/translate/re-segment/add-to-list/chapter-summary); move page More into settings More menu with liquid glass; settings tabs only Global/Menu/Info; volume-key-on-play off; header hidden.

Read-aloud: default player bg FlowingLight; capsule settings entry; speed 0.5x-4x step 0.25x; investigate engines/voices (see Log).

Out of scope: manga reader.

## AC

- AC-01 title-only + capsule
- AC-02 two-row bottom bar + labels
- AC-03 no top-right More; 3 settings tabs; More menu from Menu; liquid glass
- AC-04 volume-key-on-play off; header hidden
- AC-05 player bg FlowingLight
- AC-06 capsule opens aloud settings
- AC-07 speed 0.5-4.0 / 0.25
- AC-08 TTS investigation in Log