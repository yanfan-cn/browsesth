from pathlib import Path
from PIL import Image

src = Path(r"E:\32082\Pictures\巡页\06-07-official-full.png")
dest = Path(r"E:\32082\Pictures\巡页")
im = Image.open(src)
w, h = im.size
print("size", w, h)
# Crop roughly: top pair and bottom pair; also singles
# Based on 780x900 screenshot - content may be letterboxed. Detect content bounds.
# Simple fixed crops from the rendered layout:
# top section ~ y 0-450, bottom ~450-900
top = im.crop((0, 0, w, h // 2))
bot = im.crop((0, h // 2, w, h))
top.save(dest / "06-official-outlined-pair.png")
bot.save(dest / "07-official-rounded-pair.png")
# singles from top
mid = w // 2
top.crop((0, 40, mid, top.height)).save(dest / "03-list-C-official-outlined.png")
top.crop((mid, 40, w, top.height)).save(dest / "04-mode-B-official-outlined.png")
bot.crop((0, 40, mid, bot.height)).save(dest / "03b-list-C-official-rounded.png")
bot.crop((mid, 40, w, bot.height)).save(dest / "04b-mode-B-official-rounded.png")
print("cropped ok")
for p in sorted(dest.glob("0*.png")):
    print(p.name, p.stat().st_size)
