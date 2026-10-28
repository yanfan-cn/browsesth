# PLAN-178

对应 Spec：[`../features/SPEC-178.md`](../features/SPEC-178.md)
状态：Done（见 LOG-178）
确认：2026-10-26 真机调试 +「默认 MediaStore；下载设置所有文件访问」→「确认，完成后构建」。

## 方案

1. 真机 1.7.4 抓日志：YouTube 无 publish；全屏键灰、无回调。
2. poster 非空 + `addContentView`；YouTube 恢复可列；保留 sniff query。
3. 视频默认暂存 → MediaStore；下载设置「所有文件访问」可走公共 Movies。
4. `assembleAppDebug` → 巡页1.7.5-beta 并 adb 安装。

## 回退

恢复公共 Movies 优先、渐进 itag 硬过滤、null poster。
