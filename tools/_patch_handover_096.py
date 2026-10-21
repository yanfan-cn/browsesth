from pathlib import Path
p = Path(r"D:\cursor\browsesth\docs\会话状态交接.md")
text = p.read_text(encoding="utf-8")
replacements = [
(
"更新日期：2026-10-10（**最后成功组装 13.2；含 SPEC-055～095；094/095 未 commit；待真机验**）",
"更新日期：2026-10-10（**最后成功组装 13.3；含 SPEC-055～096；094～096 未 commit；待真机验**）",
),
(
"**最后成功组装：13.2（`VERSION_TENTHS=132`）。含 072～095。真机优先验 095→094。**",
"**最后成功组装：13.3（`VERSION_TENTHS=133`）。含 072～096。真机优先验 096→095→094。**",
),
(
"| 最后成功组装 | **13.2**（`VERSION_TENTHS=132`；`VERSION_CODE=132`） |",
"| 最后成功组装 | **13.3**（`VERSION_TENTHS=133`；`VERSION_CODE=133`） |",
),
]
for a,b in replacements:
    if a in text:
        text = text.replace(a,b,1)
        print("ok", a[:20])
    else:
        print("miss", a[:40])
row = "| **SPEC-096** | 阅读列表=book、阅读模式=book_2；手机网页顶栏左键=Home 且点击 clearActiveToNtp | **13.3** |\n"
if "SPEC-096" not in text:
    text = text.replace("| **SPEC-095** |", row + "| **SPEC-095** |", 1)
    print("row added")
# mainline
old_main = "本对话主线：**094**（启动色/提示条/标签恢复/设置返回/资讯）→ **095**（HTTPS→HTTP 回退 + 滚动连续排版）。"
new_main = "本对话主线：**094**→**095**→**096**（阅读列表 Book / 阅读模式 Book2；手机网页左上角 Home 必回 NTP）。"
if old_main in text:
    text = text.replace(old_main, new_main, 1)
    print("mainline ok")
else:
    print("mainline miss")
p.write_text(text, encoding="utf-8")
print("done")
