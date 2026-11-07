# PLAN-248：README 补手机端实机截图

对应 Spec：[SPEC-248](../features/SPEC-248.md)

## 1. 方案

1. 新建 `docs/readme-shots/phone/`。
2. 自 `E:\32082\Pictures\github readme` 拷贝今日 16:00 后 12 张 JPG，重命名为 ASCII：

| 源文件 | 目标 |
| --- | --- |
| 主页 浅色模式.jpg | home-light.jpg |
| 主页 深色模式.jpg | home-dark.jpg |
| 网页浏览.jpg | browsing.jpg |
| 标签页.jpg | tabs.jpg |
| 收藏.jpg | bookmarks.jpg |
| 工具箱 (2).jpg | toolbox.jpg |
| 阅读列表 (2).jpg | reading-list.jpg |
| 阅读记录.jpg | reading-history.jpg |
| 净读 竖屏滚动.jpg | jingdu-scroll.jpg |
| 净读 设置.jpg | jingdu-settings.jpg |
| 净读 更多设置.jpg | jingdu-more-settings.jpg |
| 净视 横屏播放.jpg | jingshi-landscape.jpg |

3. README「### 手机」改为多列表格（风格对齐平板区）。

## 2. 验证

- `Test-Path` / 目录列表确认 12 文件存在。
- 目视 README 相对路径与「待补」已消失。
- `git diff` 确认仅 docs 截图 + README + SPEC/PLAN/LOG（及交接若更新）。
