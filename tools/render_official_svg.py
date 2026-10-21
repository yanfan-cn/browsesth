from pathlib import Path
from io import BytesIO
from PIL import Image, ImageDraw, ImageFont
from svglib.svglib import svg2rlg
from reportlab.graphics import renderPM
from reportlab.lib.colors import black, Color

DEST = Path(r"E:\32082\Pictures\巡页")
OFF = DEST / "material-official"

def svg_to_pil(svg_path: Path, scale: int = 8) -> Image.Image:
    drawing = svg2rlg(str(svg_path))
    if drawing is None:
        raise RuntimeError(f"failed to parse {svg_path}")
    drawing.width *= scale
    drawing.height *= scale
    drawing.scale(scale, scale)
    # paint black
    png_data = renderPM.drawToString(drawing, fmt="PNG")
    im = Image.open(BytesIO(png_data)).convert("RGBA")
    return im

def place_on_tile(icon: Image.Image, out: Path, label: str):
    canvas = Image.new("RGB", (320, 360), "#FFFFFF")
    d = ImageDraw.Draw(canvas)
    d.rounded_rectangle([24, 24, 296, 296], radius=36, fill="#F5F5F5")
    # fit icon into 220 box
    icon = icon.copy()
    icon.thumbnail((220, 220), Image.Resampling.LANCZOS)
    # force black: if icon has alpha, paste
    px = (320 - icon.width) // 2
    py = 24 + (272 - icon.height) // 2
    if icon.mode == "RGBA":
        canvas.paste(icon, (px, py), icon)
    else:
        canvas.paste(icon, (px, py))
    try:
        font = ImageFont.truetype("msyh.ttc", 16)
    except OSError:
        font = ImageFont.load_default()
    d.text((160, 330), label, fill="#424242", font=font, anchor="mt")
    canvas.save(out, quality=95)
    print("wrote", out.name, out.stat().st_size)

# Prefer Google Material Symbols official SVG (960 viewBox)
pairs = [
    (OFF / "outlined-collections_bookmark.svg", "03-list-C-official-outlined.jpg", "list C Outlined (official)"),
    (OFF / "outlined-chrome_reader_mode.svg", "04-mode-B-official-outlined.jpg", "mode B Outlined (official)"),
    (OFF / "rounded-collections_bookmark.svg", "03b-list-C-official-rounded.jpg", "list C Rounded (official)"),
    (OFF / "rounded-chrome_reader_mode.svg", "04b-mode-B-official-rounded.jpg", "mode B Rounded (official)"),
]

icons = []
for src, name, label in pairs:
    im = svg_to_pil(src, scale=6)
    place_on_tile(im, DEST / name, label)
    icons.append((im, label))

# pair sheets
def pair_sheet(path, title, left_src, right_src, left_label, right_label):
    w, h = 720, 420
    canvas = Image.new("RGB", (w, h), "#FFFFFF")
    d = ImageDraw.Draw(canvas)
    try:
        title_f = ImageFont.truetype("msyh.ttc", 22)
        lab_f = ImageFont.truetype("msyh.ttc", 15)
    except OSError:
        title_f = lab_f = ImageFont.load_default()
    d.text((360, 20), title, fill="#212121", font=title_f, anchor="mt")
    for i, (src, label) in enumerate([(left_src, left_label), (right_src, right_label)]):
        tx = 48 + i * 344
        d.rounded_rectangle([tx, 64, tx + 280, 364], radius=20, fill="#F5F5F5")
        d.text((tx + 140, 88), label, fill="#424242", font=lab_f, anchor="mt")
        icon = svg_to_pil(src, scale=5)
        icon.thumbnail((200, 200), Image.Resampling.LANCZOS)
        px = tx + (280 - icon.width) // 2
        py = 130 + (200 - icon.height) // 2
        if icon.mode == "RGBA":
            canvas.paste(icon, (px, py), icon)
        else:
            canvas.paste(icon, (px, py))
    canvas.save(path, quality=95)
    print("wrote", path.name)

pair_sheet(
    DEST / "06-official-outlined-pair.jpg",
    "Official Material Symbols Outlined",
    OFF / "outlined-collections_bookmark.svg",
    OFF / "outlined-chrome_reader_mode.svg",
    "list C collections_bookmark",
    "mode B chrome_reader_mode",
)
pair_sheet(
    DEST / "07-official-rounded-pair.jpg",
    "Official Material Symbols Rounded",
    OFF / "rounded-collections_bookmark.svg",
    OFF / "rounded-chrome_reader_mode.svg",
    "list C collections_bookmark",
    "mode B chrome_reader_mode",
)
print("ALL OK")
