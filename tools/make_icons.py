"""Generate the Android launcher icon set from the maid-atelier "delighted" cutout.

Produces:
  res/mipmap-<density>/ic_launcher.png             legacy icon (API 21-25)
  res/mipmap-<density>/ic_launcher_foreground.png  adaptive foreground (API 26+)
  res/mipmap-anydpi-v26/ic_launcher.xml            adaptive icon definition
  res/drawable/ic_launcher_background.xml          adaptive background gradient

Also renders preview_icon.png comparing background palettes under the masks
launchers actually apply.
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont

SKIN = (r"C:\Users\lepad\.dsh\profiles\desktop\node_modules"
        r"\@smalltailqwq\dsh-client-ui-skin-maid-atelier\assets\icons")
CUTOUT = os.path.join(SKIN, "delighted-cutout.png")

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(PROJ, "res")
PREVIEW = os.path.join(PROJ, "preview_icon.png")

# name -> (top RGB, bottom RGB)
PALETTES = {
    "blue":  ((253, 254, 255), (214, 228, 247)),
    "cream": ((255, 253, 248), (240, 227, 203)),
    "navy":  ((44, 56, 84), (20, 28, 48)),
}
DEFAULT_PALETTE = "blue"

LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
ADAPTIVE_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

# Adaptive icons: 108dp canvas, the launcher shows the centre 72dp (radius 36dp).
ADAPTIVE_FILL = 0.97     # how much of the visible radius the art should span
LEGACY_FILL = 0.45       # art radius as a fraction of the legacy icon size


def load_cutout():
    im = Image.open(CUTOUT).convert("RGBA")
    a = np.asarray(im)[:, :, 3]
    ys, xs = np.where(a > 8)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    r_max = float(np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2).max())
    print("cutout %s  subject bbox x%d..%d y%d..%d  centre(%.0f,%.0f)  R=%.0f"
          % (im.size, x0, x1, y0, y1, cx, cy, r_max))
    return im, cx, cy, r_max


def paste_alpha(canvas, im, x, y):
    """Alpha-composite `im` onto `canvas` at (x, y), clipping to the canvas."""
    cw, ch = canvas.size
    iw, ih = im.size
    dx, dy = max(0, x), max(0, y)
    sx, sy = max(0, -x), max(0, -y)
    w = min(iw - sx, cw - dx)
    h = min(ih - sy, ch - dy)
    if w <= 0 or h <= 0:
        return
    region = im.crop((sx, sy, sx + w, sy + h))
    canvas.paste(region, (dx, dy), region)


def vertical_gradient(size, top, bottom):
    t = np.linspace(0.0, 1.0, size).reshape(size, 1, 1)
    base = np.array(top, float).reshape(1, 1, 3) * (1 - t) + \
        np.array(bottom, float).reshape(1, 1, 3) * t
    arr = np.repeat(base, size, axis=1).astype(np.uint8)
    return Image.fromarray(arr, "RGB").convert("RGBA")


def scaled_art(cut, cx, cy, r_max, target_r):
    scale = target_r / r_max
    size = (max(1, int(round(cut.width * scale))), max(1, int(round(cut.height * scale))))
    return cut.resize(size, Image.LANCZOS), scale


def make_foreground(cut, cx, cy, r_max, size_px):
    """Adaptive foreground: art centred on the 108dp canvas, nothing outside the mask."""
    target_r = ADAPTIVE_FILL * (36.0 / 108.0) * size_px
    art, scale = scaled_art(cut, cx, cy, r_max, target_r)
    canvas = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
    paste_alpha(canvas, art, int(round(size_px / 2 - cx * scale)),
                int(round(size_px / 2 - cy * scale)))
    return canvas


def make_legacy(cut, cx, cy, r_max, size_px, palette):
    top, bottom = PALETTES[palette]
    canvas = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
    mask = Image.new("L", (size_px, size_px), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size_px - 1, size_px - 1], radius=int(round(size_px * 0.22)), fill=255)
    canvas.paste(vertical_gradient(size_px, top, bottom), (0, 0), mask)

    target_r = LEGACY_FILL * size_px
    art, scale = scaled_art(cut, cx, cy, r_max, target_r)
    paste_alpha(canvas, art, int(round(size_px / 2 - cx * scale)),
                int(round(size_px / 2 - cy * scale)))
    return canvas


def write_adaptive_xml():
    d = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@drawable/ic_launcher_background" />\n'
                '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
                '</adaptive-icon>\n')

    top, bottom = PALETTES[DEFAULT_PALETTE]
    with open(os.path.join(RES, "drawable", "ic_launcher_background.xml"),
              "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<!-- Soft gradient behind the maid-atelier icon artwork. -->\n'
                '<shape xmlns:android="http://schemas.android.com/apk/res/android"\n'
                '    android:shape="rectangle">\n'
                '    <gradient\n'
                '        android:angle="270"\n'
                '        android:startColor="#%02X%02X%02X"\n'
                '        android:endColor="#%02X%02X%02X" />\n'
                '</shape>\n' % (top + bottom))


def render_preview(cut, cx, cy, r_max):
    """Show each palette under the circle / squircle masks launchers use."""
    S, PAD, LABEL = 190, 18, 30
    shapes = ["circle", "squircle"]
    cols, rows = len(shapes), len(PALETTES)
    w = PAD + cols * (S + PAD)
    h = PAD + rows * (S + PAD + LABEL) + 34
    sheet = Image.new("RGB", (w, h), (232, 235, 240))
    d = ImageDraw.Draw(sheet)

    def font(sz):
        for p in (r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\segoeui.ttf"):
            if os.path.exists(p):
                try:
                    return ImageFont.truetype(p, sz)
                except OSError:
                    pass
        return ImageFont.load_default()

    d.text((PAD, 8), "Android launcher icon preview (simulated launcher masks)",
           font=font(17), fill=(25, 32, 44))
    for ci, shape in enumerate(shapes):
        d.text((PAD + ci * (S + PAD) + 4, 30), shape, font=font(14), fill=(70, 80, 96))

    for ri, name in enumerate(PALETTES):
        top = PAD + 34 + ri * (S + PAD + LABEL)
        d.text((PAD, top - 0), "", font=font(12), fill=(0, 0, 0))
        for ci, shape in enumerate(shapes):
            icon = make_legacy(cut, cx, cy, r_max, S * 2, name).resize((S, S), Image.LANCZOS)
            mask = Image.new("L", (S, S), 0)
            md = ImageDraw.Draw(mask)
            if shape == "circle":
                md.ellipse([0, 0, S - 1, S - 1], fill=255)
            else:
                md.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.28), fill=255)
            x = PAD + ci * (S + PAD)
            sheet.paste(icon, (x, top), mask)
            if ci == 0:
                d.text((x + 2, top + S + 6), name, font=font(15), fill=(25, 32, 44))

    sheet.save(PREVIEW)
    print("preview ->", PREVIEW, sheet.size)


def main():
    cut, cx, cy, r_max = load_cutout()

    old = os.path.join(RES, "drawable", "ic_launcher.xml")
    if os.path.exists(old):
        os.remove(old)
        print("removed old 3x3 grid icon:", old)

    for density, px in LEGACY_SIZES.items():
        d = os.path.join(RES, "mipmap-" + density)
        os.makedirs(d, exist_ok=True)
        make_legacy(cut, cx, cy, r_max, px, DEFAULT_PALETTE).save(
            os.path.join(d, "ic_launcher.png"))

    for density, px in ADAPTIVE_SIZES.items():
        d = os.path.join(RES, "mipmap-" + density)
        os.makedirs(d, exist_ok=True)
        make_foreground(cut, cx, cy, r_max, px).save(
            os.path.join(d, "ic_launcher_foreground.png"))

    write_adaptive_xml()
    print("legacy icons   :", ", ".join("%s=%d" % kv for kv in LEGACY_SIZES.items()))
    print("adaptive icons :", ", ".join("%s=%d" % kv for kv in ADAPTIVE_SIZES.items()))
    render_preview(cut, cx, cy, r_max)


if __name__ == "__main__":
    main()
