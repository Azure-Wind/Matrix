"""Check transparency and subject bounds of the maid-atelier icon sources."""
import numpy as np
from PIL import Image

CUT = r"C:\Users\lepad\.dsh\profiles\desktop\node_modules\@smalltailqwq\dsh-client-ui-skin-maid-atelier\assets\icons\delighted-cutout.png"
P512 = r"C:\Users\lepad\.dsh\profiles\desktop\node_modules\@smalltailqwq\dsh-client-ui-skin-maid-atelier\assets\icons\delighted-512.png"
SRC = r"C:\Users\lepad\.dsh\profiles\desktop\node_modules\@smalltailqwq\dsh-client-ui-skin-maid-atelier\assets\icons\source\delighted.png"

for label, path in (("cutout", CUT), ("512", P512), ("source", SRC)):
    im = Image.open(path)
    print("=== %s : %s %s ===" % (label, im.mode, im.size))
    rgba = im.convert("RGBA")
    a = np.asarray(rgba)
    alpha = a[:, :, 3]
    print("  alpha min/max : %d / %d" % (alpha.min(), alpha.max()))
    print("  transparent   : %d px (%.1f%%)" % ((alpha == 0).sum(), 100.0 * (alpha == 0).mean()))
    print("  opaque        : %d px (%.1f%%)" % ((alpha == 255).sum(), 100.0 * (alpha == 255).mean()))
    h, w = alpha.shape
    print("  corners       : TL=%d TR=%d BL=%d BR=%d" % (alpha[0, 0], alpha[0, w-1], alpha[h-1, 0], alpha[h-1, w-1]))
    for thr in (8, 128, 250):
        ys, xs = np.where(alpha > thr)
        if len(xs):
            print("  bbox a>%3d    : x %4d..%4d  y %4d..%4d  (w=%d h=%d)" % (
                thr, xs.min(), xs.max(), ys.min(), ys.max(),
                xs.max()-xs.min()+1, ys.max()-ys.min()+1))
        else:
            print("  bbox a>%3d    : none" % thr)
    print()
