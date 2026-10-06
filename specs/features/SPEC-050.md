# SPEC-050 NTP/启动体验修复

状态: In Progress | 2026-10-06 | 依赖 SPEC-049

## 问题
1. 冷启动更慢（首帧液态玻璃采样 + 壁纸/favicon）
2. Splash 未挂 Theme.App.Starting，adaptive icon 再裁切导致 logo 看不见
3. 顶栏 IconButton 默认 48dp + 玻璃阴影形成遮罩，间距过密
4. 胶囊内扫码/搜索无液态玻璃

## 验收
- AC-10: 启动页可见巡页 logo（非空白圆）
- AC-20: 首帧不拉壁纸/favicon、不采样 wall backdrop
- AC-30: 顶栏图标间距 12dp，圆形玻璃无阴影遮罩
- AC-40: 扫码与搜索为同款圆形液态玻璃
