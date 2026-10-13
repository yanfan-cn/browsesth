# PLAN-112: Reader UI and read-aloud defaults

| Plan ID | PLAN-112 |
| Spec | [`../features/SPEC-112.md`] |
| Status | Done |
| Baseline | ceff2ef |
| Log | [`../logs/LOG-112.md`] |

## Approach

Force-migrate prefs (App.kt flags); default bottom buttons; settings 3 tabs; remove title More; liquid-glass More sheet; speech-rate scale via ReadAloudSpeechRate; capsule settings bus; fix sysEngines + tablet nav bus.

Deviation: header force via ReadStyleGateway.applyHeaderHiddenDefault (architecture guard).

## Verify

- compileAppDebugKotlin OK
- assembleAppDebug -> XunYe 1.0.4-beta.apk; next 1.0.5
- Device AC-01..07 pending