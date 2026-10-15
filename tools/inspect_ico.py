"""Inspect the shortcut's custom .ico and export its largest frame as PNG."""
import os

from PIL import Image

SRC = r"C:\Users\lepad\.dsh\skin-manager\desktop-icons\maid-atelier-c94d48e02db5ac71.ico"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "shortcut_icon.png")

print("file   :", SRC)
print("bytes  :", os.path.getsize(SRC))

im = Image.open(SRC)
print("format :", im.format, " mode:", im.mode)
sizes = sorted(im.info.get("sizes", []), key=lambda t: t[0] * t[1])
print("frames :", sizes)

best = None
for (w, h) in sizes:
    frame = Image.open(SRC)
    frame.size = (w, h)
    rgba = frame.convert("RGBA")
    if best is None or w > best[0]:
        best = (w, rgba)

if best is None:
    raise SystemExit("no frames found")

img = best[1]
img.save(OUT)
print("largest: %sx%s -> %s" % (img.width, img.height, OUT))

# corner / centre sample, to learn the background treatment
px = img.load()
print("corners:", px[0, 0], px[img.width - 1, 0], px[0, img.height - 1], px[img.width - 1, img.height - 1])
print("centre :", px[img.width // 2, img.height // 2])
