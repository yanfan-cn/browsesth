# PLAN-137

1. `BrowserVideoPlayerScreen`：去字幕/清晰度；旋转切换；statusBars 顶栏；水平 seek 15s；时长文案；PiP 仅播停。
2. `VideoView`：关闭默认 controls（若 API 允许）。
3. `BrowserVideoPlaybackService` + Bridge：MediaSession 进度、ACTION_SEEK_FWD/BACK 15s。
4. assemble。
