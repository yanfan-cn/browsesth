# SPEC-116: Bookmarks chrome + reading folders + README

| Spec ID | SPEC-116 |
| Status | Done |
| Plan | [../plans/PLAN-116.md] |
| Updated | 2026-10-13 |
| Baseline | SPEC-113 / SPEC-115 |

## Confirm

- 2026-10-13: A3 (folders for reading bookmarks), B1 (draft README), C1 (assemble).
- Top: remove title/PrimaryTabRow; use tabs-page segmented control style for 鏀惰棌|涔︾.
- Bookmarks tab: sort + new folder both usable (A3 = reading-side folders).

## Scope

1. Me/bookmarks screen top bar matches BrowserTabs mode capsule (centered ~50%), labels 鏀惰棌 / 涔︾; keep swipe.
2. Sort + new folder available on both tabs; reading tab folders are independent of web favorites folders.
3. Reading bookmark overflow supports Move to folder.
4. Root README: public repo, product blurb, Chromium, doc links; drop private/scaffold-only wording.

Out of scope: Room schema change (prefs org store), commit/push unless asked.

## AC

- AC-01: Top bar has segmented 鏀惰棌|涔︾ (no PrimaryTabRow / no 鏀惰棌涓庝功绛?title).
- AC-02: On 涔︾ tab, sort menu works on reading list.
- AC-03: On 涔︾ tab, new folder creates a reading folder; open/delete/move work for reading bookmarks.
- AC-04: README states public GitHub and current product direction.