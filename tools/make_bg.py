"""Convert phonescreen.png into a compact WebP background drawable for the APK.

The source PNG is ~3 MB; a lossy WebP at q85 keeps the same pixels and visual
quality while shrinking the payload by roughly an order of magnitude. WebP
decoding is native on Android 4.0+ (the app's minSdk is 21), so no extra
runtime dependency is introduced.
"""
import os

from PIL import Image

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(PROJ, "phonescreen.png")
OUT_DIR = os.path.join(PROJ, "res", "drawable-nodpi")
OUT = os.path.join(OUT_DIR, "bg_phone_img.webp")

os.makedirs(OUT_DIR, exist_ok=True)

src_size = os.path.getsize(SRC)
with Image.open(SRC) as im:
    print("source : %s  %s  %d bytes" % (im.size, im.mode, src_size))
    im = im.convert("RGB")
    im.save(OUT, "WEBP", quality=85, method=6)

out_size = os.path.getsize(OUT)
print("output : %s  %d bytes" % (OUT, out_size))
print("saved  : %.1f%% smaller" % (100.0 * (1 - out_size / src_size)))
