# SPEC-120: Splash, tablet tabs park, NTP search, force zoom, privacy page

| Spec ID | SPEC-120 |
| Status | Ready |
| Plan | [../plans/PLAN-120.md] |
| Updated | 2026-10-14 |
| Baseline | SPEC-119 |

## Confirm

- 2026-10-14: Operator confirmed four-item batch; privacy must be a **new settings page** (do as much as Aloha APIs allow).

## Scope

1. Splash background `#F6F1EA` (light) / `#121212` (system dark); splash + About logos use 24dp rounded rect plate.
2. Tablet tab switch must not reload: park browser sessions across tab switches.
3. Tablet NTP omnibox: resolve search/URL before load (no raw-query `net::ERROR`).
4. Toolbox font sheet「强制页面缩放」default ON; apply via AwSettings / JS viewport when available.
5. Browser settings: **new「隐私与安全」page** with Cookie / third-party Cookie / Safe Browsing (if API present) + entry to clear browsing data.

## Out of scope

- Chromium/SDK rebuild; Do Not Track / site permissions matrix beyond Aloha surface; M3 Expressive redesign.

## AC

- AC-01: Cold start splash uses `#F6F1EA` light / `#121212` night; splash animated icon and About logo show 24dp rounded plate.
- AC-02: On tablet, switch tab A→B→A without refresh: page content/scroll preserved (parked host), unless user refreshed or slot evicted.
- AC-03: Tablet NTP omnibox search keyword opens search results URL (not `net::ERR_*` from treating keywords as URL).
- AC-04: Fresh install / default:「强制页面缩放」ON; pinch/zoom works on viewport-locked pages when API/JS path available.
- AC-05: Browser settings →「隐私与安全」opens a dedicated page; toggles persist; Cookie/3P/Safe Browsing applied to sessions when Aloha exposes the API;「清除浏览数据」opens existing clear-data screen.
