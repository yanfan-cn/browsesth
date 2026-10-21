"""SPEC-115: regenerate default launcher / notification icons from XunYe source."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

SRC = Path(r"D:\cursor\browsesth\brand\browsesth-logo-transparent.png")
RES = Path(r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\res")
BG = (246, 241, 234, 255)  # #F6F1EA


def fit_on_canvas(src: Image.Image, size: int, pad_ratio: float = 0.12) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), BG)
    max_side = int(size * (1 - 2 * pad_ratio))
    ratio = min(max_side / src.width, max_side / src.height)
    w = max(1, int(src.width * ratio))
    h = max(1, int(src.height * ratio))
    logo = src.resize((w, h), Image.Resampling.LANCZOS)
    x = (size - w) // 2
    y = (size - h) // 2
    canvas.alpha_composite(logo, (x, y))
    return canvas


def to_round(im: Image.Image) -> Image.Image:
    size = im.size[0]
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    out = im.copy()
    out.putalpha(mask)
    return out


def make_mono(fg: Image.Image) -> Image.Image:
    # Turn non-background pixels into grayscale with alpha; bg -> transparent.
    rgba = fg.convert("RGBA")
    r, g, b, a = rgba.split()
    # distance from bg (approx)
    import numpy as np

    arr = np.array(rgba)
    bg = np.array(BG, dtype=np.int16)
    dist = np.abs(arr.astype(np.int16) - bg).sum(axis=2)
    keep = (arr[:, :, 3] >= 8) & (dist >= 24)
    lum = (0.299 * arr[:, :, 0] + 0.587 * arr[:, :, 1] + 0.114 * arr[:, :, 2]).astype(np.uint8)
    out = np.zeros_like(arr)
    out[:, :, 0] = lum
    out[:, :, 1] = lum
    out[:, :, 2] = lum
    out[:, :, 3] = np.where(keep, arr[:, :, 3], 0)
    return Image.fromarray(out, "RGBA")


def main() -> None:
    if not SRC.exists():
        raise SystemExit(f"missing source: {SRC}")
    src = Image.open(SRC).convert("RGBA")
    print("src", src.size)

    fg = fit_on_canvas(src, 512, pad_ratio=0.08)
    fg_path = RES / "drawable-xxhdpi" / "ic_launcher_foreground.png"
    fg.save(fg_path, "PNG", optimize=True)
    print("wrote", fg_path, fg_path.stat().st_size)

    mono_path = RES / "drawable-xxhdpi" / "ic_launcher_foreground_m.png"
    try:
        make_mono(fg).save(mono_path, "PNG", optimize=True)
    except Exception as e:
        print("mono numpy failed:", e, "; fallback copy fg")
        # fallback: simple grayscale RGB + keep alpha-ish via convert
        g = fg.convert("L").convert("RGBA")
        # punch out near-bg via difference
        arr_fg = list(fg.getdata())
        arr_g = list(g.getdata())
        out = []
        for (r, gg, b, a), (lr, lg, lb, _) in zip(arr_fg, arr_g):
            if a < 8 or abs(r - BG[0]) + abs(gg - BG[1]) + abs(b - BG[2]) < 24:
                out.append((0, 0, 0, 0))
            else:
                out.append((lr, lr, lr, a))
        img = Image.new("RGBA", fg.size)
        img.putdata(out)
        img.save(mono_path, "PNG", optimize=True)
    print("wrote", mono_path, mono_path.stat().st_size)

    icon256 = fit_on_canvas(src, 256, pad_ratio=0.1)
    out = RES / "drawable" / "ic_launcher.webp"
    icon256.convert("RGB").save(out, "WEBP", quality=90)
    print("wrote", out, out.stat().st_size)

    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in densities.items():
        d = RES / folder
        d.mkdir(exist_ok=True)
        im = fit_on_canvas(src, size, pad_ratio=0.1)
        p1 = d / "ic_launcher.webp"
        p2 = d / "ic_launcher_round.webp"
        im.convert("RGB").save(p1, "WEBP", quality=90)
        to_round(im).save(p2, "WEBP", quality=90)
        print("wrote", folder, size, p1.stat().st_size, p2.stat().st_size)

    print("done")


if __name__ == "__main__":
    main()