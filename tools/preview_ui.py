"""Render a layout mockup of every screen.

SIMULATION drawn with Pillow, not a screenshot of the running app. It follows
the same dp values, colours and grid geometry as the XML layouts and uses the
real background artwork at the same alpha, so it shows the intended layout.

Outputs:
  preview_ui_a.png  - the two working screens (bigger for detail checking)
  preview_ui_b.png  - home, wizard step, stats, wrong-book grid, wrong list
"""
import os

from PIL import Image, ImageDraw, ImageFont

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(PROJ, "phonescreen.png")
OUT_A = os.path.join(PROJ, "preview_ui_a.png")
OUT_B = os.path.join(PROJ, "preview_ui_b.png")

S = 2.0
W, H = int(360 * S), int(740 * S)

WHITE = (255, 255, 255)
BORDER = (169, 184, 204)
NUMBER = (22, 32, 46)
HINT = (100, 115, 138)
TIMER = (47, 111, 237)
PRIMARY = (47, 111, 237)
NAVY = (59, 74, 99)
SECONDARY = (227, 232, 240)
SECONDARY_TEXT = (42, 53, 71)
DANGER = (201, 69, 63)
GREEN = (11, 138, 75)


def font(sp, bold=False):
    for p in ((r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc"),
              r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\simhei.ttf"):
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, max(8, int(round(sp * S))))
            except OSError:
                pass
    return ImageFont.load_default()


def px(v):
    return int(round(v * S))


def draw_center(d, text, f, cx, cy, fill):
    box = d.textbbox((0, 0), text, font=f)
    d.text((cx - (box[2] - box[0]) / 2 - box[0], cy - (box[3] - box[1]) / 2 - box[1]),
           text, font=f, fill=fill)


def base_screen(art):
    base = Image.new("RGB", (W, H), WHITE)
    scale = max(W / art.width, H / art.height)
    resized = art.resize((max(1, round(art.width * scale)), max(1, round(art.height * scale))),
                         Image.LANCZOS)
    left, top = (resized.width - W) // 2, (resized.height - H) // 2
    return Image.blend(base, resized.crop((left, top, left + W, top + H)), 0.5)


def back_button(d):
    x, y, s = px(12), px(12), px(42)
    d.ellipse([x, y, x + s, y + s], fill=(255, 255, 255), outline=(210, 216, 226), width=1)
    cx, cy = x + s / 2, y + s / 2
    a = px(7)
    d.line([cx + a * 0.5, cy - a, cx - a * 0.7, cy, cx + a * 0.5, cy + a],
           fill=NUMBER, width=max(2, px(1.6)), joint="curve")
    d.line([cx - a * 0.7, cy, cx + a * 0.75, cy], fill=NUMBER, width=max(2, px(1.6)))


def grid(d, x, y, rows, cols, cell, gap, values, f, blank=False):
    for r in range(rows):
        for c in range(cols):
            x0, y0 = x + c * (cell + gap), y + r * (cell + gap)
            d.rounded_rectangle([x0, y0, x0 + cell, y0 + cell], radius=max(2, int(cell * 0.16)),
                                fill=WHITE, outline=BORDER, width=max(1, px(1)))
            v = values[r][c]
            if v != "":
                draw_center(d, str(v), f, x0 + cell // 2, y0 + cell // 2, NUMBER)


def pill(d, cx, cy, text, f):
    box = d.textbbox((0, 0), text, font=f)
    tw, th = box[2] - box[0], box[3] - box[1]
    w, h = tw + px(36), px(38)
    d.rounded_rectangle([cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2],
                        radius=h // 2, fill=(255, 255, 255), outline=(215, 220, 228), width=1)
    draw_center(d, text, f, cx, cy, SECONDARY_TEXT)
    return w


def bottom_bar(d, f):
    cy = H - px(29)
    pill(d, px(12) + _pill_half(d, "错题集", f), cy, "错题集", f)
    pill(d, W - px(12) - _pill_half(d, "统计", f), cy, "统计", f)


def _pill_half(d, text, f):
    box = d.textbbox((0, 0), text, font=f)
    return (box[2] - box[0] + px(36)) // 2


def action_buttons(d, f, y):
    half = (W - px(20) - px(10)) // 2
    d.rounded_rectangle([px(10), y, px(10) + half, y + px(52)], radius=px(10), fill=PRIMARY)
    d.rounded_rectangle([px(10) + half + px(10), y, W - px(10), y + px(52)],
                        radius=px(10), fill=SECONDARY)
    draw_center(d, "下一个", f, px(10) + half // 2, y + px(26), WHITE)
    draw_center(d, "查看答案", f, px(10) + half + px(10) + half // 2, y + px(26), SECONDARY_TEXT)


def wrong_button(d, f, y):
    d.rounded_rectangle([px(10), y, W - px(10), y + px(46)], radius=px(10), fill=DANGER)
    draw_center(d, "❌ 做错了", f, W // 2, y + px(23), WHITE)


# ---------------------------------------------------------------- screens

def screen_det(art, f22, f13, f26, f26c, f20, f17, f16, f14):
    im = base_screen(art)
    d = ImageDraw.Draw(im)
    cx = W // 2
    vals = [[6, 1, 1], [4, -2, 5], [2, 8, 7]]
    draw_center(d, "三阶行列式", f22, cx, px(76), NUMBER)
    draw_center(d, "心算下面 3×3 矩阵的行列式", f13, cx, px(101), HINT)
    draw_center(d, "12.4 秒", f26, cx, px(134), TIMER)
    cell, gap = px(78), px(8)
    gw = 3 * cell + 2 * gap
    grid(d, (W - gw) // 2, px(186), 3, 3, cell, gap, vals, f26c)
    draw_center(d, "行列式的值 = -306", f20, cx, px(186) + 3 * cell + 2 * gap + px(40), GREEN)
    action_buttons(d, f17, px(770))
    wrong_button(d, f16, px(880))
    bottom_bar(d, f14)
    back_button(d)
    return im


def screen_matmul(art, f22, f14, f26, f24, f17, f16, f13, f12):
    im = base_screen(art)
    d = ImageDraw.Draw(im)
    cx = W // 2
    M = K = N = 3
    A = [[-10, -10, -10], [2, 7, -4], [6, -8, 9]]
    B = [[4, -6, 2], [9, 1, -5], [-3, 8, 7]]
    C = [[-100, -30, -40], [83, -37, -59], [-75, 28, 115]]
    revealed = 4                       # 逐步揭晓到第 4 个

    draw_center(d, "矩阵相乘", f22, cx, px(74), NUMBER)
    draw_center(d, "A %d×%d  ·  B %d×%d" % (M, K, K, N), f14, cx, px(98), HINT)
    draw_center(d, "23.5 秒", f26, cx, px(128), TIMER)

    # A x B : smaller cells
    avail = 360 - 20 - 28
    cellAB = px(max(26.0, min(54.0, avail / (K + N) * 0.88)))
    gapAB = max(1, round(cellAB * 0.05))
    fAB = font(max(10.0, cellAB / S * 0.34))
    aW = K * cellAB + (K - 1) * 2 * gapAB
    bW = N * cellAB + (N - 1) * 2 * gapAB
    xW = px(28)
    x0 = (W - (aW + xW + bW)) // 2
    rowY = px(160)
    aH = M * cellAB + (M - 1) * 2 * gapAB
    bH = K * cellAB + (K - 1) * 2 * gapAB
    grid(d, x0 + gapAB, rowY + gapAB, M, K, cellAB, 2 * gapAB, A, fAB)
    draw_center(d, "×", f24, x0 + aW + xW // 2, rowY + aH // 2, NUMBER)
    grid(d, x0 + aW + xW + gapAB, rowY + (aH - bH) // 2 + gapAB, K, N, cellAB, 2 * gapAB, B, fAB)

    # = C : result cells noticeably larger
    cellC = px(max(42.0, min(58.0, (360 - 20) / N * 0.9)))
    gapC = max(1, round(cellC * 0.05))
    fC = font(max(12.0, cellC / S * 0.34))
    yc = rowY + aH + px(6)
    draw_center(d, "=", f24, cx, yc + px(14), NUMBER)
    cW = N * cellC + (N - 1) * 2 * gapC
    cxLeft = (W - cW) // 2 + gapC
    partial = [["" for _ in range(N)] for _ in range(M)]
    for i in range(revealed):
        partial[i // N][i % N] = C[i // N][i % N]
    grid(d, cxLeft, yc + px(28), M, N, cellC, 2 * gapC, partial, fC)

    hintY = yc + px(28) + (M * cellC + (M - 1) * 2 * gapC) + px(18)
    draw_center(d, "%d / %d" % (revealed, M * N), f13, cx, hintY, HINT)

    action_buttons(d, f17, H - px(190))
    wrong_button(d, f16, H - px(128))
    bottom_bar(d, f14)
    back_button(d)
    return im


def screen_home(art, f18, f28, f14, f13):
    im = base_screen(art)
    d = ImageDraw.Draw(im)
    cx = W // 2
    draw_center(d, "线性代数小双基", f28, cx, px(245), NUMBER)
    draw_center(d, "心算练习", f14, cx, px(275), HINT)
    bw = W - px(56)
    d.rounded_rectangle([px(28), px(315), px(28) + bw, px(315) + px(58)], radius=px(10), fill=PRIMARY)
    draw_center(d, "行列式计算", f18, cx, px(344), WHITE)
    draw_center(d, "3×3 矩阵求行列式的值", f13, cx, px(393), HINT)
    y2 = px(415)
    d.rounded_rectangle([px(28), y2, px(28) + bw, y2 + px(58)], radius=px(10), fill=NAVY)
    draw_center(d, "矩阵相乘", f18, cx, y2 + px(29), WHITE)
    draw_center(d, "两个矩阵相乘，求积矩阵", f13, cx, y2 + px(78), HINT)
    return im


def screen_step(art, f13, f24, f22, f13b):
    im = base_screen(art)
    d = ImageDraw.Draw(im)
    cx = W // 2
    draw_center(d, "第 2 / 3 步", f13, cx, px(235), HINT)
    draw_center(d, "A 的列数 = B 的行数", f24, cx, px(272), NUMBER)
    draw_center(d, "A 的列数必须等于 B 的行数", f13, cx, px(302), HINT)
    cw, ch, gap = px(62), px(52), px(6)
    x0 = (W - (3 * cw + 4 * gap)) // 2
    for i, opt in enumerate([2, 3, 4]):
        x = x0 + gap + i * (cw + gap)
        sel = (i == 1)
        d.rounded_rectangle([x, px(335), x + cw, px(335) + ch], radius=px(8),
                            fill=PRIMARY if sel else WHITE,
                            outline=PRIMARY if sel else BORDER, width=max(1, px(1)))
        draw_center(d, str(opt), f22, x + cw // 2, px(335) + ch // 2,
                    WHITE if sel else SECONDARY_TEXT)
    draw_center(d, "A 的行数 = 3　·　A列=B行 = 3", f13b, cx, px(420), HINT)
    d.rounded_rectangle([px(28), px(459), W - px(28), px(459) + px(54)], radius=px(10), fill=PRIMARY)
    draw_center(d, "确认", f22, cx, px(486), WHITE)
    back_button(d)
    return im


def screen_stats(art, f22, f14, f34, f15):
    im = base_screen(art)
    d = ImageDraw.Draw(im)
    cx = W // 2
    draw_center(d, "成绩统计", f22, cx, px(76), NUMBER)
    draw_center(d, "矩阵相乘", f14, cx, px(102), HINT)
    draw_center(d, "最近 100 次平均", f14, cx, px(152), HINT)
    draw_center(d, "18.7 秒", f34, cx, px(190), TIMER)
    draw_center(d, "总次数：43 次", f14, cx, px(228), HINT)
    left = px(24)
    d.text((left, px(268)), "最近 10 次", font=f14, fill=HINT)
    times = ["23.5 秒", "18.2 秒", "31.0 秒", "12.4 秒", "9.8 秒",
             "27.6 秒", "15.3 秒", "21.1 秒", "19.0 秒", "16.7 秒"]
    for i, t in enumerate(times):
        d.text((left, px(292 + i * 20)), "%d.   %s" % (i + 1, t), font=f15, fill=NUMBER)
    back_button(d)
    return im


def screen_wrong_shapes(art, f22, f13, f14):
    im = base_screen(art)
    d = ImageDraw.Draw(im)
    cx = W // 2
    draw_center(d, "矩阵相乘错题集", f22, cx, px(76), NUMBER)
    draw_center(d, "选择一个尺寸查看错题", f13, cx, px(102), HINT)
    opts = [2, 3, 4]
    labels = []
    for k in opts:
        for m in opts:
            for n in opts:
                labels.append("(%d×%d,%d×%d)" % (m, k, k, n))
    top, bh, gapx = px(128), px(52), px(8)
    gx = px(16)
    colw = (W - 2 * gx - 2 * gapx) // 3
    for i, lab in enumerate(labels):
        r, c = i // 3, i % 3
        x = gx + c * (colw + gapx)
        y = top + r * (bh + gapx)
        d.rounded_rectangle([x, y, x + colw, y + bh], radius=px(7),
                            fill=WHITE, outline=BORDER, width=max(1, px(1)))
        draw_center(d, lab, f14, x + colw // 2, y + bh // 2, NUMBER)
    back_button(d)
    return im


def screen_wrong_list(art, f22, f13, f18, f12):
    im = base_screen(art)
    d = ImageDraw.Draw(im)
    cx = W // 2
    draw_center(d, "(3×3,3×3)", f22, cx, px(76), NUMBER)
    draw_center(d, "共 8 道　·　已做 3 / 8", f13, cx, px(102), HINT)
    total, done = 8, 3
    top, bh, gapx, gapy = px(140), px(50), px(8), px(8)
    gx = px(16)
    colw = (W - 2 * gx - 4 * gapx) // 5
    for i in range(total):
        r, c = i // 5, i % 5
        x = gx + c * (colw + gapx)
        y = top + r * (bh + gapy)
        is_done = i < done
        d.rounded_rectangle([x, y, x + colw, y + bh], radius=px(9),
                            fill=GREEN if is_done else WHITE,
                            outline=(8, 107, 58) if is_done else BORDER, width=max(1, px(1)))
        draw_center(d, str(i + 1), f18, x + colw // 2, y + bh // 2,
                    WHITE if is_done else NUMBER)
    draw_center(d, "点击序号可以重做这道错题，做完会变绿", f12, cx, top + 2 * (bh + gapy) + px(24), HINT)
    back_button(d)
    return im


with Image.open(ART) as raw:
    art = raw.convert("RGB")

det = screen_det(art, font(22, True), font(13), font(26, True), font(26, True),
                 font(20, True), font(17, True), font(16, True), font(14, True))
mat = screen_matmul(art, font(22, True), font(14), font(26, True), font(24, True),
                    font(17, True), font(16, True), font(13), font(12))
home = screen_home(art, font(18, True), font(28, True), font(14), font(13))
step = screen_step(art, font(13, True), font(24, True), font(22, True), font(13))
stats = screen_stats(art, font(22, True), font(14), font(34, True), font(15))
shapes = screen_wrong_shapes(art, font(22, True), font(13), font(14))
wlist = screen_wrong_list(art, font(22, True), font(13), font(18, True), font(12))


def sheet(panels, path, cols):
    GAP, PAD, CAP = px(16), px(16), px(30)
    rows = (len(panels) + cols - 1) // cols
    w = PAD * 2 + cols * W + (cols - 1) * GAP
    h = PAD * 2 + rows * (H + CAP) + (rows - 1) * GAP
    im = Image.new("RGB", (w, h), (238, 241, 246))
    sd = ImageDraw.Draw(im)
    for i, (p, name) in enumerate(panels):
        c, r = i % cols, i // cols
        x = PAD + c * (W + GAP)
        y = PAD + r * (H + CAP + GAP)
        im.paste(p, (x, y))
        draw_center(sd, name, font(14, True), x + W // 2, y + H + CAP // 2, NUMBER)
    im.save(path)
    print("written:", path, im.size)


sheet([(det, "行列式"), (mat, "矩阵相乘（揭晓 4/9）")], OUT_A, 2)
sheet([(home, "首页"), (step, "向导第2步"), (stats, "统计"),
       (shapes, "27 尺寸错题集"), (wlist, "错题目录")], OUT_B, 3)
