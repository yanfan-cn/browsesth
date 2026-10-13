# PLAN-115

| Plan ID | PLAN-115 |
| Spec | [../features/SPEC-115.md] |
| Status | Done |
| Log | [../logs/LOG-115.md] |
| Updated | 2026-10-13 |

## Approach

1. MyScreen: Software update -> PrefClickEvent.OpenUrl(software_update_url).
2. non_translat: software_update_url, contributors_url, github_url = browsesth / Feipan.
3. AboutScreen: OpenUrl(stringResource(R.string.contributors_url)).
4. Python/Pillow from E:\32082\mobile device\宸￠〉.png regenerate drawable/ic_launcher.webp + mipmap densities + round; refresh foreground PNG if needed.
5. mipmap-anydpi-v26 launcher0-6/w -> same adaptive layers as ic_launcher.

## Verify

Static path check; no assemble (C2).