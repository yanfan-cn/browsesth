# PLAN-186

对应 Spec：[`../features/SPEC-186.md`](../features/SPEC-186.md)
状态：Done

1. `CoverInterceptor`：`pdf-page://` 直接 `chain.proceed()`。
2. `PdfFile.readPdf` try/catch + AppLog + 半开 fd 清理；`getImage` 失败记 AppLog。
3. 编译验证；真机复现取 AppLog 定位根因。
