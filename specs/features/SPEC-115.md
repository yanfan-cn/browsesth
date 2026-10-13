# SPEC-115: Software update link, About GitHub, XunYe icons everywhere

| Spec ID | SPEC-115 |
| Status | Done |
| Plan | [../plans/PLAN-115.md] |
| Updated | 2026-10-13 |
| Baseline | SPEC-114 / SPEC-052 |

## Confirm

- 2026-10-13: Operator confirmed A1 (public GitHub https://github.com/yanfan-cn/browsesth; repo made PUBLIC), B2 (all launcher skins = XunYe icon), C2 (no assemble). Software update opens https://share.feijipan.com/u/xcUCqQCUN. Ads stays placeholder.

## Scope

1. Me -> Software update opens the Feipan share URL (external browser/intent).
2. About -> Contributors opens https://github.com/yanfan-cn/browsesth; sync non_translat URL strings.
3. Replace legacy Legado bird bitmaps: drawable/ic_launcher, mipmap ic_launcher / round, and point launcher0-6 / launcherw adaptive icons at the same XunYe foreground.

Out of scope: assemble/install, commit/push, Ads implementation, GitHub visibility ops (already public).

## AC

- [ ] AC-01: Tap Software update opens the Feipan URL.
- [ ] AC-02: Tap Contributors opens https://github.com/yanfan-cn/browsesth (public).
- [ ] AC-03: Notification read-aloud large icon and APK/file-manager icon use XunYe art (not Legado bird).
- [ ] AC-04: Optional launcher skins 0-6 / w use the same XunYe adaptive foreground.