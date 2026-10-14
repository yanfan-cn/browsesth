# PLAN-122

| Plan ID | PLAN-122 |
| Spec | [../features/SPEC-122.md] |
| Log | [../logs/LOG-122.md] |
| Status | Ready |
| Updated | 2026-10-14 |

## Approach

1. **Reader chrome:** Tablet floating bottom bar width 0.5 / 0.6 by orientation; `LocalReaderUnderBrowserChrome` → top bar 16dp under omnibox (skip statusBars).
2. **Tab blank:** `key(sessionKey)` on `AlohaBrowserCompose` so AndroidView remounts after `parkActive` (empty container bug).
3. **Session restore:** `BrowserTabPersist` + `BrowserChromePrefs.restoreSessionOnLaunch` (default true); `BrowserTabManager.init` in App; settings switch.
4. **Version:** Odometer bump in `app/build.gradle.kts`; `version.properties` = 1.1.3 for this assemble.

## Tasks

- T1 Docs
- T2 Tablet reader layout
- T3 Tab blank fix
- T4 Persist/restore tabs + settings switch
- T5 Version odometer + 1.1.3
- T6 assembleAppDebug

## TC

- TC-01 ↔ AC-01 … TC-05 ↔ AC-05
