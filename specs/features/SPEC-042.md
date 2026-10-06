# SPEC-042：浏览器阅读模式接入章节翻译

## 1. 基本信息
| Spec ID | SPEC-042 |
| 状态 | Done |
| 确认 | 同 2026-10-06 |
| Plan | [`../plans/PLAN-042.md`](../plans/PLAN-042.md) |
| Log | [`../logs/LOG-042.md`](../logs/LOG-042.md) |

## 2. 范围
- 浏览器临时小说书支持阅读器「翻译」模式。
- 浏览器设置增加「打开阅读时自动翻译」开关（默认开）。
- 不实现浏览器 Tab 整页 DOM 实时翻译（二期）。

## 3. 验收
- [x] AC-01：临时网页书可开关 translationMode 并触发章节翻译管线。（打开时按开关 setTranslationMode；真机待验）
- [x] AC-02：设置项可关闭自动翻译。
