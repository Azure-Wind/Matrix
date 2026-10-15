"""Inspect the DSH icon so the Android icon assets can be generated faithfully."""
import numpy as np
from PIL import Image

SRC = r"D:\Deepseek\resources\icon.png"

im = Image.open(SRC)
print("mode:", im.mode, "size:", im.size)
rgba = im.convert("RGBA")
a = np.asarray(rgba).astype(np.int32)
rgb, alpha = a[:, :, :3], a[:, :, 3]

print("\n--- corner pixels (RGB, A) ---")
for name, (y, x) in {"TL": (2, 2), "TR": (2, 1021), "BL": (1021, 2),
                     "BR": (1021, 1021), "centre": (512, 512)}.items():
    print("  %-6s %s  A=%d" % (name, rgb[y, x], alpha[y, x]))

print("\n--- alpha ---")
print("  min/max:", alpha.min(), alpha.max())
print("  fully transparent px:", int((alpha == 0).sum()))
print("  fully opaque px     :", int((alpha == 255).sum()))

luma = 0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2]
opaque = alpha > 200
print("\n--- luma within opaque area ---")
print("  min: %.0f  max: %.0f" % (luma[opaque].min(), luma[opaque].max()))
hist, edges = np.histogram(luma[opaque], bins=12, range=(0, 256))
for h, e in zip(hist, edges):
    print("  luma>=%3d : %7d" % (e, h))

dark = rgb[opaque & (luma < 90)]
print("\n--- whale (dark) pixels ---")
print("  count:", len(dark))
print("  mean RGB: %s" % np.round(dark.mean(axis=0), 1))
print("  std  RGB: %s" % np.round(dark.std(axis=0), 1))

light = rgb[opaque & (luma > 225)]
print("\n--- background (light) pixels ---")
print("  count:", len(light))
print("  mean RGB: %s" % np.round(light.mean(axis=0), 1))
print("  std  RGB: %s" % np.round(light.std(axis=0), 1))

mask = opaque & (luma < 128)
ys, xs = np.where(mask)
print("\n--- whale bounding box (in 1024px) ---")
print("  x: %d..%d  (w=%d)" % (xs.min(), xs.max(), xs.max() - xs.min() + 1))
print("  y: %d..%d  (h=%d)" % (ys.min(), ys.max(), ys.max() - ys.min() + 1))

print("\n--- opaque-area bounding box (the rounded square) ---")
ys2, xs2 = np.where(alpha > 8)
print("  x: %d..%d   y: %d..%d" % (xs2.min(), xs2.max(), ys2.min(), ys2.max()))

print("\n--- coarse luma map (16x16, '.'=light '#'=dark) ---")
step = 1024 // 16
for r in range(16):
    row = ""
    for c in range(16):
        block = luma[r * step:(r + 1) * step, c * step:(c + 1) * step]
        m = block.mean()
        row += "#" if m < 100 else ("+" if m < 200 else ".")
    print("  " + row)
