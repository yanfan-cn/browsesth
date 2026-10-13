# SPEC-114: Me list - remove file manage; add update/ads placeholders

| Spec ID | SPEC-114 |
| Status | Done |
| Plan | [../plans/PLAN-114.md] |
| Updated | 2026-10-13 |
| Baseline | SPEC-089 / SPEC-059 |

## Confirm

- 2026-10-13: Agent recap - remove file manage from Me; add Software update and Ads between Download and About; both option C (placeholder only); no build. Operator: confirm, both C, no build after done.

## Scope

1. Remove File Management entry from Me list (keep FileManageActivity code).
2. Between Download and About, order: Download -> Software update -> Ads -> About.
3. Software update / Ads: Toast Coming soon / ji jiang tui chu only; no check-update, no ad settings page.

Out of scope: real update check, ad settings UI, ad SDK, assemble/install, commit/push.

## AC

- [x] AC-01: Me list has no File Management (static).
- [x] AC-02: Between Download and About: Software update then Ads (static).
- [ ] AC-03: Tapping either shows coming-soon Toast (not installed this session).