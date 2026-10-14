# PLAN-120

| Plan ID | PLAN-120 |
| Spec | [../features/SPEC-120.md] |
| Log | [../logs/LOG-120.md] |
| Status | Ready |
| Updated | 2026-10-14 |

## Approach

1. **Splash:** `values` / `values-night` `splash_screen_background`; `ic_splash_logo_rounded` layer-list (UTF-8); theme `windowSplashScreenAnimatedIcon`; About already 24dp plate — verify only.
2. **Park tabs:** Rewrite `BrowserSessionHolder` with parked map (MAX=8); `BrowserTabManager` park/destroyKey; attach restores parked host.
3. **NTP search:** `BrowserHomeTabScreen` / tablet chrome resolve via `resolveBrowserNavigationInput` before `loadUrl` / open bridge.
4. **Force zoom:** `BrowserChromePrefs.forcePageZoom` default true; sheet switch; `BrowserPrivacyControls.applyForceZoom` + viewport JS fallback on `AwContentsHostView`.
5. **Privacy page:** New `BrowserPrivacyScreen` + `MainRouteBrowserPrivacy`; Browser config shows clickable entry (not inline switches); wire CookieManager / AwSettings; re-apply to active+parked hosts; clear-data deep link.

## Tasks

- T1 SPEC/PLAN/LOG
- T2 Splash colors + rounded drawable (UTF-8)
- T3 Session park / no reload
- T4 NTP omnibox resolve
- T5 Force page zoom default ON
- T6 Dedicated privacy settings page + nav + apply
- T7 assembleAppDebug (GRADLE_USER_HOME)

## TC

- TC-01↔AC-01 … TC-05↔AC-05
