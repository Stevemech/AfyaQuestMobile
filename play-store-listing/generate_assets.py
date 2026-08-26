#!/usr/bin/env python3
"""
Generates the Google Play store listing graphics for AfyaQuest.

Outputs into ./graphics:
  app-icon-512.png              512x512   Play app icon
  feature-graphic-1024x500.png  1024x500  Play feature graphic
  screenshots/NN-*.png          1080x1920 phone screenshots (9:16 exactly)

Source screenshots are the real in-app captures kept in the afyasite repo.
They are 838x1866 (~9:20), which is outside the 16:9..9:16 range Play accepts,
so each one is placed on a branded 1080x1920 canvas rather than stretched --
that fixes the aspect ratio without distorting the UI or faking app state.

Run:  <afyasite>/website/.venv-crop/bin/python generate_assets.py
"""

from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

HERE = Path(__file__).resolve().parent
SITE = Path("/Users/steve/Documents/GitHub/afyasite/website")
FONTS = Path(
    "/private/tmp/claude-501/-Users-steve-Documents-GitHub/"
    "f7b25a6a-7566-44a0-b078-f325e7425dfd/scratchpad/fonts"
)
SHOTS = SITE / "public" / "walkthrough"
OUT = HERE / "graphics"

# Brand palette, lifted from website/tailwind.config.js
TEAL_DARK = (37, 77, 77)
TEAL = (67, 136, 148)
TEAL_LIGHT = (178, 232, 241)
GOLD = (239, 160, 63)
WHITE = (255, 255, 255)


def font(name, size):
    return ImageFont.truetype(str(FONTS / name), size)


def vertical_gradient(size, top, bottom):
    """Linear top-to-bottom gradient, built at 1px wide then stretched."""
    w, h = size
    strip = Image.new("RGB", (1, h))
    px = strip.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        px[0, y] = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return strip.resize((w, h), Image.BICUBIC)


def outline_circle(img, center, radius, color, width=2, opacity=38):
    """The thin decorative rings used across the marketing site."""
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = center
    d.ellipse(
        [cx - radius, cy - radius, cx + radius, cy + radius],
        outline=color + (opacity,),
        width=width,
    )
    img.alpha_composite(layer)


def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius, fill=255)
    return mask


def wrap(draw, text, fnt, max_width):
    lines, words = [], text.split()
    line = ""
    for word in words:
        trial = f"{line} {word}".strip()
        if draw.textlength(trial, font=fnt) <= max_width or not line:
            line = trial
        else:
            lines.append(line)
            line = word
    if line:
        lines.append(line)
    return lines


def wrap_balanced(draw, text, fnt, max_width):
    """
    Greedy wrapping leaves orphans ("...built for / CHWs"). When the text needs
    exactly two lines, pick the split that makes them closest in length instead.
    """
    lines = wrap(draw, text, fnt, max_width)
    if len(lines) != 2:
        return lines
    words = text.split()
    best = None
    for i in range(1, len(words)):
        a, b = " ".join(words[:i]), " ".join(words[i:])
        wa, wb = draw.textlength(a, font=fnt), draw.textlength(b, font=fnt)
        if wa <= max_width and wb <= max_width and (best is None or max(wa, wb) < best[0]):
            best = (max(wa, wb), [a, b])
    return best[1] if best else lines


def draw_centered(draw, lines, fnt, top, width, fill, line_height):
    y = top
    for line in lines:
        w = draw.textlength(line, font=fnt)
        draw.text(((width - w) / 2, y), line, font=fnt, fill=fill)
        y += line_height
    return y


def load_logo():
    """The brand mark: a white elephant on a teal disc, on a white field."""
    return Image.open(SITE / "logo.png").convert("RGBA")


def elephant_on_transparent(px=1200):
    """
    Isolates the white elephant so it can sit on any background.

    The source art is a white elephant knocked out of a teal disc, and the disc
    itself sits on a white field. Whiteness alone therefore selects the elephant
    *and* the surrounding field, so it has to be intersected with the disc.
    """
    logo = load_logo().convert("RGB").resize((px, px), Image.LANCZOS)
    gray = logo.convert("L")

    # Only the disc is dark, so its bounding box locates the disc; the elephant
    # is enclosed by it and cannot extend the box.
    disc = gray.point(lambda v: 255 if v < 150 else 0)
    left, top, right, bottom = disc.getbbox()
    cx, cy = (left + right) / 2, (top + bottom) / 2
    # Shrink slightly to drop the disc's antialiased rim.
    radius = min(right - left, bottom - top) / 2 - px * 0.004

    inside = Image.new("L", logo.size, 0)
    ImageDraw.Draw(inside).ellipse(
        [cx - radius, cy - radius, cx + radius, cy + radius], fill=255
    )

    # Ramp teal->white into 0->255 so the elephant keeps its antialiased edges.
    whiteness = gray.point(lambda v: 0 if v < 110 else min(255, int((v - 110) * 255 / 110)))

    out = Image.new("RGBA", logo.size, WHITE + (0,))
    out.putalpha(ImageChops.multiply(whiteness, inside))
    return out


def build_app_icon():
    """
    512x512, filled edge to edge.

    Play masks the icon with rounded corners, so a circle-on-white source would
    render as a small disc floating in white. Filling the square with the disc's
    own teal and scaling up past the edges keeps the mark full-bleed under any mask.
    """
    size = 512
    logo = load_logo()
    # Oversize so the teal disc bleeds past all four corners, then centre-crop.
    scale = int(size * 1.42)
    big = logo.resize((scale, scale), Image.LANCZOS)
    off = (scale - size) // 2

    icon = Image.new("RGB", (size, size), TEAL_DARK)
    icon.paste(big.convert("RGB"), (-off, -off), big)
    path = OUT / "app-icon-512.png"
    icon.save(path, "PNG")
    return path


def build_feature_graphic():
    """1024x500. Play may overlay a play button dead centre, so keep the middle calm."""
    w, h = 1024, 500
    img = vertical_gradient((w, h), TEAL_DARK, TEAL).convert("RGBA")
    outline_circle(img, (880, 90), 190, WHITE, 2, 45)
    outline_circle(img, (120, 430), 150, WHITE, 2, 40)
    outline_circle(img, (935, 385), 90, TEAL_LIGHT, 2, 55)

    mark = elephant_on_transparent(1200)
    # Crop to the elephant's bounding box so it can be sized independently of the disc.
    bbox = mark.getbbox()
    mark = mark.crop(bbox)
    target_h = 250
    mark = mark.resize(
        (round(mark.width * target_h / mark.height), target_h), Image.LANCZOS
    )
    img.alpha_composite(mark, (86, (h - target_h) // 2))

    d = ImageDraw.Draw(img)
    text_x = 86 + mark.width + 66
    d.text((text_x, 168), "AfyaQuest", font=font("bricolage-800.ttf", 92), fill=WHITE)
    d.text(
        (text_x, 278),
        "Gamified training and daily tools",
        font=font("figtree-600.ttf", 33),
        fill=TEAL_LIGHT,
    )
    d.text(
        (text_x, 320),
        "for community health workers",
        font=font("figtree-600.ttf", 33),
        fill=TEAL_LIGHT,
    )
    d.rounded_rectangle([text_x, 378, text_x + 118, 384], 3, fill=GOLD)

    path = OUT / "feature-graphic-1024x500.png"
    img.convert("RGB").save(path, "PNG")
    return path


SCREENS = [
    ("dashboard.png", "Your whole day, in one place",
     "Clock in, daily questions, your itinerary, and the end-of-day report."),
    ("learning modules.png", "36 video lessons in 6 modules",
     "Childhood illness, chronic disease, maternal health, first aid."),
    ("inside of a learning module, lessons.png", "Watch a video, then take the quiz",
     "Short lessons you can finish between visits."),
    ("show nearby hospitals.png", "Find the nearest health facility",
     "Hospitals and clinics nearby, with services and distance."),
    ("profile.png", "Level up as you learn",
     "XP, streaks, levels, and achievements track your progress."),
]


def build_screenshot(src_name, headline, sub, index):
    W, H = 1080, 1920
    img = vertical_gradient((W, H), TEAL_DARK, TEAL).convert("RGBA")
    outline_circle(img, (940, 250), 240, WHITE, 2, 40)
    outline_circle(img, (110, 640), 170, WHITE, 2, 32)

    d = ImageDraw.Draw(img)
    head_font = font("bricolage-700.ttf", 62)
    sub_font = font("figtree-400.ttf", 34)

    head_lines = wrap_balanced(d, headline, head_font, W - 150)
    y = draw_centered(d, head_lines, head_font, 96, W, WHITE, 76)
    sub_lines = wrap_balanced(d, sub, sub_font, W - 190)
    y = draw_centered(d, sub_lines, sub_font, y + 14, W, TEAL_LIGHT, 46)

    shot = Image.open(SHOTS / src_name).convert("RGB")
    top = int(y) + 52
    avail_h = H - top - 76
    scale = avail_h / shot.height
    new = (round(shot.width * scale), avail_h)
    shot = shot.resize(new, Image.LANCZOS)

    radius = 34
    shot.putalpha(rounded_mask(new, radius))
    x = (W - new[0]) // 2

    shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [x, top + 14, x + new[0], top + new[1] + 14], radius, fill=(0, 0, 0, 110)
    )
    img.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(26)))
    img.alpha_composite(shot, (x, top))

    path = OUT / "screenshots" / f"{index:02d}-{Path(src_name).stem.split(',')[0].replace(' ', '-')}.png"
    img.convert("RGB").save(path, "PNG")
    return path


def main():
    (OUT / "screenshots").mkdir(parents=True, exist_ok=True)
    made = [build_app_icon(), build_feature_graphic()]
    for i, (src, head, sub) in enumerate(SCREENS, start=1):
        made.append(build_screenshot(src, head, sub, i))
    for p in made:
        with Image.open(p) as im:
            print(f"{p.relative_to(HERE)}  {im.width}x{im.height}  {p.stat().st_size/1024:.0f} KB")


if __name__ == "__main__":
    main()
