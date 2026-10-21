from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

DEST = Path(r"E:\32082\Pictures\巡页")
FONT = Path(r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\res\font\material_symbols_outlined.ttf")
font = ImageFont.truetype(str(FONT), 256)

def one(name, lig):
    im = Image.new("RGB", (320, 320), "#FFFFFF")
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([24, 24, 296, 296], radius=36, fill="#F5F5F5")
    d.text((160, 160), lig, fill="#212121", font=font, anchor="mm")
    p = DEST / name
    im.save(p, quality=95)
    print(p.name, p.stat().st_size)

one("03-list-C-official-outlined.jpg", "collections_bookmark")
one("04-mode-B-official-outlined.jpg", "chrome_reader_mode")
print("done")
