# SPEC-117: Read group default + ever-read semantics + NTP shortcuts

| Spec ID | SPEC-117 |
| Status | Done |
| Plan | [../plans/PLAN-117.md] |
| Updated | 2026-10-13 |
| Baseline | SPEC-116 |

## Confirm

- 2026-10-13: A1 (宸茶 = entered reading mode), B1 (force migrate shortcuts), C1 (animation research only), D2 (no assemble).
- Baidu shortcut icon: local high-res (E:\32082\Pictures\鐧惧害.jpg / ic_engine_baidu).

## Scope

1. Reading list default tab = 宸茶 (IdReadFinished); force for installed via migrate key.
2. 宸茶 lists books with durChapterTime > 0 (opened/read in reader), not only last-chapter-finished.
3. Browser novel/manga books no longer use notShelf so they stay on shelf and appear in 宸茶.
4. Default NTP shortcuts: Baidu, Bilibili (given URL), rebang.open2hub.com, ithome.com, thepaper.cn; remove YouTube/Hupu/Facebook; force migrate.
5. Baidu shortcut uses bundled local icon (not remote favicon).
6. Animation: no code change (research already delivered).

## AC

- AC-01: Fresh/migrated open reading list lands on 宸茶.
- AC-02: After browser reading mode, book appears under 宸茶 (survives cold start).
- AC-03: Local book opened in reader appears under 宸茶.
- AC-04: Migrated NTP shortcuts match new default set; Baidu icon is sharp local asset.