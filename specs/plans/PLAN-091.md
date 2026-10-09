# PLAN-091
1. OnboardingUiState.pageCount=3；Screen 页映射 0 隐私 / 1 文件夹 / 2 主题；标题摘要同步。
2. BookFolderPage：删 select_book_folder；按钮 scale 1.5f 或等倍 padding。
3. ThemePage：Switch + 说明；Intent 写 ThemeSettings.chromeSurfaceStyle + topBarButtonStyle（ChromeSurfaceStylePolicy）；初始跟随 resolve/defaultStyle。