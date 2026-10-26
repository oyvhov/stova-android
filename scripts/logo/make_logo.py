"""Run: python -I scripts/logo/make_logo.py app/src/main/res <preview-folder>

Stova's mark and wordmark: one geometry, written as Android vectors and rendered as previews.

The mark is a soft house whose window is a glowing screen (the living room's warm light). The
wordmark is a geometric lowercase "stova" drawn with round strokes, so no font licence is involved.
"""
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

RES = Path(sys.argv[1])          # app/src/main/res
PREVIEW = Path(sys.argv[2])      # folder for PNG previews
PREVIEW.mkdir(parents=True, exist_ok=True)

INK = (16, 18, 17)
SAGE = (186, 199, 156)
TEXT = (233, 233, 226)
GLOW_TOP = (255, 196, 122)
GLOW_BOTTOM = (232, 135, 63)
EMBER = (255, 167, 79)


# ---- geometry -----------------------------------------------------------------------------------
def house(cx, top, w, h, r=None):
    """Rounded house silhouette: peak at (cx, top), eaves at 36 % height, body to top+h."""
    r = r or w * 0.09
    left, right, bottom = cx - w / 2, cx + w / 2, top + h
    eave = top + h * 0.36
    pr = w * 0.06  # peak rounding
    k = (eave - top) / (w / 2)  # roof slope
    dx = pr / math.sqrt(1 + k * k)
    return [
        ("M", cx - dx, top + k * dx),
        ("Q", cx, top, cx + dx, top + k * dx),
        ("L", right - r * 0.9, eave - k * r * 0.9 + r * 0.25),
        ("Q", right, eave, right, eave + r),
        ("L", right, bottom - r),
        ("Q", right, bottom, right - r, bottom),
        ("L", left + r, bottom),
        ("Q", left, bottom, left, bottom - r),
        ("L", left, eave + r),
        ("Q", left, eave, left + r * 0.9, eave - k * r * 0.9 + r * 0.25),
        ("Z",),
    ]


def rrect(x0, y0, x1, y1, r):
    return [
        ("M", x0 + r, y0), ("L", x1 - r, y0), ("Q", x1, y0, x1, y0 + r),
        ("L", x1, y1 - r), ("Q", x1, y1, x1 - r, y1), ("L", x0 + r, y1),
        ("Q", x0, y1, x0, y1 - r), ("L", x0, y0 + r), ("Q", x0, y0, x0 + r, y0), ("Z",),
    ]


def mark(cx, top, w):
    """House plus its screen window; returns (house path, screen path, screen box)."""
    h = w * 0.94
    body = house(cx, top, w, h)
    eave = top + h * 0.36
    sw = w * 0.70
    sh = sw * 9 / 16
    sx0, sx1 = cx - sw / 2, cx + sw / 2
    sy1 = top + h - w * 0.13
    sy0 = sy1 - sh
    sy0 = max(sy0, eave + w * 0.02)
    screen = rrect(sx0, sy0, sx1, sy1, w * 0.075)
    return body, screen, (sx0, sy0, sx1, sy1)


def circle(cx, cy, r):
    k = 0.5523 * r
    return [("M", cx + r, cy), ("C", cx + r, cy + k, cx + k, cy + r, cx, cy + r),
            ("C", cx - k, cy + r, cx - r, cy + k, cx - r, cy),
            ("C", cx - r, cy - k, cx - k, cy - r, cx, cy - r),
            ("C", cx + k, cy - r, cx + r, cy - k, cx + r, cy)]


def wordmark(x, baseline, xh, sw, gap):
    """Stroke centrelines for s t o v a; returns (list of paths, right edge)."""
    top = baseline - xh + sw / 2
    bot = baseline - sw / 2
    mid = (top + bot) / 2
    r = (bot - top) / 2
    paths = []
    # s: two counter-turning bowls
    sx = x + sw / 2
    sW = r * 1.7
    paths.append([
        ("M", sx + sW, top + r * 0.32),
        ("C", sx + sW * 0.78, top - r * 0.05, sx + sW * 0.05, top - r * 0.08, sx + sW * 0.02, top + r * 0.48),
        ("C", sx, mid + r * 0.08, sx + sW, mid - r * 0.12, sx + sW, bot - r * 0.48),
        ("C", sx + sW * 0.98, bot + r * 0.08, sx + sW * 0.2, bot + r * 0.05, sx, bot - r * 0.32),
    ])
    x = sx + sW + sw / 2 + gap
    # t: stem from above the x-height, foot turning right; bar on the x-height
    tx = x + sw / 2 + r * 0.25
    paths.append([("M", tx, top - xh * 0.42), ("L", tx, bot - r * 0.55), ("Q", tx, bot, tx + r * 0.8, bot)])
    paths.append([("M", x + sw / 2 - r * 0.1, top), ("L", tx + r * 0.95, top)])
    x = tx + r * 0.95 + sw / 2 + gap
    # o
    paths.append(circle(x + sw / 2 + r, mid, r) + [("Z",)])
    x = x + sw + 2 * r + gap
    # v
    vx = x + sw / 2
    paths.append([("M", vx, top), ("L", vx + r * 1.05, bot), ("L", vx + r * 2.1, top)])
    x = vx + r * 2.1 + sw / 2 + gap
    # a: single-storey bowl and stem
    acx = x + sw / 2 + r
    paths.append(circle(acx, mid, r) + [("Z",)])
    paths.append([("M", acx + r, top), ("L", acx + r, bot)])
    return paths, acx + r + sw / 2


# ---- serialise ------------------------------------------------------------------------------------
def fmt(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def path_data(cmds):
    out = []
    for c in cmds:
        out.append(c[0] + ",".join(fmt(v) for v in c[1:]) if len(c) > 1 else c[0])
    return " ".join(out)


def flatten(cmds, steps=24):
    """Polylines (one per subpath) for rendering."""
    lines, cur, start = [], [], None
    p = (0.0, 0.0)
    for c in cmds:
        op = c[0]
        if op == "M":
            if cur: lines.append(cur)
            p = (c[1], c[2]); cur = [p]; start = p
        elif op == "L":
            p = (c[1], c[2]); cur.append(p)
        elif op == "Q":
            (x1, y1, x2, y2) = c[1:]
            for i in range(1, steps + 1):
                t = i / steps
                cur.append(((1 - t) ** 2 * p[0] + 2 * (1 - t) * t * x1 + t * t * x2,
                            (1 - t) ** 2 * p[1] + 2 * (1 - t) * t * y1 + t * t * y2))
            p = (x2, y2)
        elif op == "C":
            (x1, y1, x2, y2, x3, y3) = c[1:]
            for i in range(1, steps + 1):
                t = i / steps; u = 1 - t
                cur.append((u ** 3 * p[0] + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t ** 3 * x3,
                            u ** 3 * p[1] + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t ** 3 * y3))
            p = (x3, y3)
        elif op == "Z":
            if start: cur.append(start)
    if cur: lines.append(cur)
    return lines


class Canvas:
    def __init__(self, w, h, scale, bg):
        self.s = scale * 4
        self.img = Image.new("RGB", (int(w * self.s), int(h * self.s)), bg)

    def _pts(self, line):
        return [(x * self.s, y * self.s) for x, y in line]

    def fill(self, cmds, color):
        d = ImageDraw.Draw(self.img)
        for line in flatten(cmds): d.polygon(self._pts(line), fill=color)

    def fill_gradient(self, cmds, box, top, bottom):
        x0, y0, x1, y1 = [v * self.s for v in box]
        grad = Image.new("RGB", self.img.size)
        gd = ImageDraw.Draw(grad)
        for y in range(int(y0), int(y1) + 1):
            t = (y - y0) / max(1, (y1 - y0))
            gd.line([(0, y), (grad.width, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bottom)))
        mask = Image.new("L", self.img.size, 0)
        md = ImageDraw.Draw(mask)
        for line in flatten(cmds): md.polygon(self._pts(line), fill=255)
        self.img.paste(grad, (0, 0), mask)

    def glow(self, cx, cy, r, color, alpha):
        layer = Image.new("RGB", self.img.size, color)
        mask = Image.new("L", self.img.size, 0)
        md = ImageDraw.Draw(mask)
        steps = 40
        for i in range(steps, 0, -1):
            rr = r * i / steps * self.s
            a = int(alpha * 255 * (1 - i / steps) ** 1.6)
            md.ellipse([cx * self.s - rr, cy * self.s - rr, cx * self.s + rr, cy * self.s + rr], fill=a)
        self.img.paste(layer, (0, 0), mask)

    def stroke(self, cmds, color, width):
        d = ImageDraw.Draw(self.img)
        w = width * self.s
        for line in flatten(cmds):
            pts = self._pts(line)
            d.line(pts, fill=color, width=int(w), joint="curve")
            for x, y in (pts[0], pts[-1]):
                d.ellipse([x - w / 2, y - w / 2, x + w / 2, y + w / 2], fill=color)

    def save(self, path, size):
        self.img.resize(size, Image.LANCZOS).save(path)


# ---- Android XML ----------------------------------------------------------------------------------
HEADER = '<?xml version="1.0" encoding="utf-8"?>\n'


def vector(w, h, body, comment):
    return (HEADER + f"<!-- {comment} -->\n"
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            f'    android:width="{w}dp"\n    android:height="{h}dp"\n'
            f'    android:viewportWidth="{w}"\n    android:viewportHeight="{h}">\n' + body + "</vector>\n")


def x_fill(cmds, color):
    return f'    <path\n        android:fillColor="{color}"\n        android:pathData="{path_data(cmds)}" />\n'


def x_gradient(cmds, box):
    x0, y0, x1, y1 = box
    return (f'    <path android:pathData="{path_data(cmds)}">\n'
            '        <aapt:attr name="android:fillColor">\n'
            f'            <gradient android:type="linear" android:startX="{fmt((x0 + x1) / 2)}" android:startY="{fmt(y0)}"\n'
            f'                android:endX="{fmt((x0 + x1) / 2)}" android:endY="{fmt(y1)}">\n'
            '                <item android:offset="0" android:color="@color/stova_glow_top" />\n'
            '                <item android:offset="1" android:color="@color/stova_glow_bottom" />\n'
            '            </gradient>\n        </aapt:attr>\n    </path>\n')


def x_radial(cx, cy, r, box):
    x0, y0, x1, y1 = box
    return (f'    <path android:pathData="{path_data(rrect(x0, y0, x1, y1, 0.01))}">\n'
            '        <aapt:attr name="android:fillColor">\n'
            f'            <gradient android:type="radial" android:centerX="{fmt(cx)}" android:centerY="{fmt(cy)}"\n'
            f'                android:gradientRadius="{fmt(r)}">\n'
            '                <item android:offset="0" android:color="@color/stova_hearth" />\n'
            '                <item android:offset="1" android:color="@color/stova_ink_clear" />\n'
            '            </gradient>\n        </aapt:attr>\n    </path>\n')


def x_stroke(cmds, color, width):
    return (f'    <path\n        android:strokeColor="{color}"\n        android:strokeWidth="{fmt(width)}"\n'
            '        android:strokeLineCap="round"\n        android:strokeLineJoin="round"\n'
            f'        android:pathData="{path_data(cmds)}" />\n')


# ---- launcher icon foreground (108 viewport, mark inside the 66 dp safe circle) ------------------
body, screen, sbox = mark(54, 29, 52)
fg = x_fill(body, "@color/stova_lime") + x_gradient(screen, sbox)
(RES / "drawable" / "ic_stova_foreground.xml").write_text(
    vector(108, 108, fg, "Stova's mark: a soft house whose window is the room's warm screen."), encoding="utf-8")
c = Canvas(108, 108, 4, INK)
c.fill(body, SAGE); c.fill_gradient(screen, sbox, GLOW_TOP, GLOW_BOTTOM)
c.save(PREVIEW / "icon.png", (432, 432))

# ---- TV banner 320x180 ---------------------------------------------------------------------------
mw = 60
XH, SW, GAP = 36, 8.5, 5.5
_, width = wordmark(0, 0, XH, SW, GAP)
left = (320 - (mw + 20 + width)) / 2
mcx = left + mw / 2
mh = mw * 0.94
mtop = 90 - mh / 2
mbody, mscreen, mbox = mark(mcx, mtop, mw)
wx = mcx + mw / 2 + 20
baseline = mtop + mh - 1
paths, right = wordmark(wx, baseline, XH, SW, GAP)
banner = x_fill(rrect(0, 0, 320, 180, 0.01), "@color/stova_ink")
banner += x_radial(mcx, mtop + 44, 120, (0, 0, 320, 180))
banner += x_fill(mbody, "@color/stova_lime") + x_gradient(mscreen, mbox)
for p in paths: banner += x_stroke(p, "@color/stova_wordmark", SW)
(RES / "drawable" / "tv_banner.xml").write_text(
    vector(320, 180, banner, "320x180 TV banner: the mark on a faint hearth glow, and the stova wordmark."), encoding="utf-8")
c = Canvas(320, 180, 4, INK)
c.glow(mcx, mtop + 44, 120, EMBER, 0.16)
c.fill(mbody, SAGE); c.fill_gradient(mscreen, mbox, GLOW_TOP, GLOW_BOTTOM)
for p in paths: c.stroke(p, TEXT, SW)
c.save(PREVIEW / "banner.png", (960, 540))
print("wordmark right edge", round(right, 1), "of 320")
