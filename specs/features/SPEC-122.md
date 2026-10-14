# SPEC-122: Tablet reader chrome, tab blank fix, session restore, version odometer

| Spec ID | SPEC-122 |
| Status | Ready |
| Plan | [../plans/PLAN-122.md] |
| Updated | 2026-10-14 |
| Baseline | SPEC-121 / SPEC-108 |

## Confirm

- 2026-10-14: Operator confirmed restatement; next assemble ships **1.1.3-beta**.

## Scope

1. **Tablet reading chrome:** Landscape bottom bar width = 50% screen; portrait = 60%. Reader top bar sits 16dp below browser omnibox (reduce excessive gap).
2. **Tab blank regression:** After tab switch, tabs must not all go blank; history/bookmarks must open pages again.
3. **Restore session on launch:** Prefer keep all open tabs across process death/restart; settings toggle「启动时恢复上次浏览页面」, **default ON**.
4. **Version odometer (amend SPEC-108):** Each of MAJOR.MINOR.PATCH is 0–9; after PATCH=9 → MINOR+1, PATCH=0; after MINOR=9 → MAJOR+1, MINOR=0. Retcon 1.0.12→1.1.2; this assemble = **1.1.3-beta**, then bump to 1.1.4.

## Out of scope

- Chromium rebuild; phone reader chrome width; commit unless asked.

## AC

- AC-01: Tablet landscape reader bottom bar ≈ 50% width; portrait ≈ 60%.
- AC-02: Tablet reader top chrome gap under omnibox ≈ 16dp.
- AC-03: Switch tabs repeatedly: pages remain / restore; history & bookmarks open URLs in a tab.
- AC-04: With restore ON (default): kill app with N tabs → relaunch shows those tabs (urls).
- AC-05: Assemble APK named `巡页1.1.3-beta`; version.properties next is 1.1.4 (odometer).
