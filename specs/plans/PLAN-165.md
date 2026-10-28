# PLAN-165

1. 引导只渲染主题页：去掉协议页和书籍文件夹页，隐藏深浅色切换，强制浅色，完成时写 `privacyPolicyOk`。
2. `ReaderProvider.onCreate` 不再注册快捷方式。`MainActivity` 在首帧之后再注册，并去掉可变字体轴，避免 1.6.8 启动页闪退。
3. `assembleAppDebug`（`--no-configuration-cache`，`GRADLE_USER_HOME=D:\cursor\.gradle-home`）。
