# SPEC-034：打开网页闪退（对齐 6.9 打开路径）

## 1. 基本信息

| Spec ID | SPEC-034 |
| 状态 | Implemented（工程完成；真机待验） |
| 需求依据 | 2026-10-04：历史/书签/主页搜索均闪退；6.9 及以前无此问题 |
| 对应 Plan | [`../plans/PLAN-034.md`] |

## 2. 确认

紧急回归：SPEC-032 把 Cookie/Eyeo 从冷启动挪到 `AwContents.attach`，二次启动好了，但打开网页会崩。6.9 打开网页时 attach 不做这段 JNI。

## 3. 规则

- 主页搜索、历史、书签、快捷方式打开网站，均须进入该页，不得闪退。
- 冷启动退出后再开，不得回到 SPEC-032 前的启动闪退（这段 JNI 也不放回 `initializeAll`）。
- CommandLine（loadLibrary 前）保留。

## 4. 验收

- [x] AC-01：历史点一条、书签点一条、主页搜索，都能打开且不闪退。（代码对齐 6.9 attach；真机待验）
- [x] AC-02：完全退出后再打开应用，不在启动页闪退。（Cookie/Eyeo 未加回 initializeAll；真机待验）
- [x] AC-03：assemble 成功（7.2）。
