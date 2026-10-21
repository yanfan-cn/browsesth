from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

DEST = Path(r"E:\32082\Pictures\巡页")
FONT = Path(r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\res\font\material_symbols_outlined.ttf")

def render_pair(path, title, left, right):
    w, h = 720, 420
    im = Image.new("RGB", (w, h), "#FFFFFF")
    d = ImageDraw.Draw(im)
    try:
        ui = ImageFont.truetype("msyh.ttc", 22)
        ui2 = ImageFont.truetype("msyh.ttc", 16)
        ui3 = ImageFont.truetype("msyh.ttc", 13)
    except OSError:
        ui = ui2 = ui3 = ImageFont.load_default()
    icon_font = ImageFont.truetype(str(FONT), 160)
    d.text((w // 2, 20), title, fill="#212121", font=ui, anchor="mt")
    labels = [
        ("list C collections_bookmark", left),
        ("mode B chrome_reader_mode", right),
    ]
    for i, (label, lig) in enumerate(labels):
        tx = 48 + i * 344
        d.rounded_rectangle([tx, 64, tx + 280, 364], radius=20, fill="#F5F5F5")
        d.text((tx + 140, 88), label, fill="#424242", font=ui2, anchor="mt")
        d.text((tx + 140, 112), "App font ligature", fill="#757575", font=ui3, anchor="mt")
        d.text((tx + 140, 240), lig, fill="#212121", font=icon_font, anchor="mm")
    im.save(path, quality=95)
    print("wrote", path, path.stat().st_size)

DEST.mkdir(parents=True, exist_ok=True)
print("font exists", FONT.exists(), FONT)
render_pair(
    DEST / "06-official-outlined-pair.jpg",
    "Official Material Outlined (app font)",
    "collections_bookmark",
    "chrome_reader_mode",
)
