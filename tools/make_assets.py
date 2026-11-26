#!/usr/bin/env python3
"""Generates the retro sprite sheet and launcher icon.

This script is the source of truth for the sprite layout. The same rectangles are mirrored in
app/src/main/java/com/icarus/pixelinvaders/render/Sprites.kt and checked by SpriteSheetTest.

Sprites are drawn white-on-transparent; the renderer tints them per theme.

Usage: python3 tools/make_assets.py   (needs Pillow)
"""
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHEET_PATH = os.path.join(ROOT, "app/src/main/res/drawable-nodpi/spritesheet.png")
ICON_PATH = os.path.join(ROOT, "app/src/main/res/drawable/ic_launcher.xml")
SHEET_W, SHEET_H = 160, 32

# --- Alien designs (all 11x8, two animation frames each) --------------------------------------

JELLY = [  # top row, 30 pts
    [
        "....###....",
        "..#######..",
        ".#########.",
        ".##.###.##.",
        ".#########.",
        "...#...#...",
        "..#.###.#..",
        ".#.......#.",
    ],
    [
        "....###....",
        "..#######..",
        ".#########.",
        ".##.###.##.",
        ".#########.",
        "..#.....#..",
        "...#.#.#...",
        "..#.....#..",
    ],
]

BEETLE = [  # middle rows, 20 pts
    [
        "..#.....#..",
        "...#...#...",
        "..#######..",
        ".##.###.##.",
        "###########",
        "#.#######.#",
        "#.#.....#.#",
        "...##.##...",
    ],
    [
        "..#.....#..",
        "#..#...#..#",
        "#.#######.#",
        "###.###.###",
        "###########",
        ".#########.",
        "..#.....#..",
        ".#.......#.",
    ],
]

SKULL = [  # bottom row, 10 pts
    [
        "...#####...",
        ".#########.",
        "###########",
        "##..###..##",
        "###########",
        "..##...##..",
        ".##.###.##.",
        "##.......##",
    ],
    [
        "...#####...",
        ".#########.",
        "###########",
        "##..###..##",
        "###########",
        ".###...###.",
        "..#.###.#..",
        ".#.......#.",
    ],
]

ALIEN_BOOM = [
    "#...#.#...#",
    ".#..#.#..#.",
    "..#.....#..",
    "##.......##",
    "..#.....#..",
    ".#..#.#..#.",
    "#...#.#...#",
    "....#.#....",
]

UFO = [
    ".....######.....",
    "...##########...",
    "..############..",
    ".##.##.##.##.##.",
    "################",
    "..###..##..###..",
    "...#........#...",
]

SHIP = [
    "......#......",
    ".....###.....",
    ".....###.....",
    ".###########.",
    "#############",
    "#############",
    "#############",
    "#############",
]

BULLET = ["#", "#", "#", "#", "#"]

BOMB = [
    [".#.", "#..", ".#.", "..#", ".#.", "#.."],
    [".#.", "..#", ".#.", "#..", ".#.", "..#"],
]

# --- Tiny 3x5 font ------------------------------------------------------------------------------

FONT_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-:!"
FONT_Y, FONT_PITCH = 22, 4
FONT = {
    "0": ["###", "#.#", "#.#", "#.#", "###"],
    "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["###", "..#", "###", "#..", "###"],
    "3": ["###", "..#", "###", "..#", "###"],
    "4": ["#.#", "#.#", "###", "..#", "..#"],
    "5": ["###", "#..", "###", "..#", "###"],
    "6": ["###", "#..", "###", "#.#", "###"],
    "7": ["###", "..#", "..#", "..#", "..#"],
    "8": ["###", "#.#", "###", "#.#", "###"],
    "9": ["###", "#.#", "###", "..#", "###"],
    "A": ["###", "#.#", "###", "#.#", "#.#"],
    "B": ["##.", "#.#", "##.", "#.#", "##."],
    "C": ["###", "#..", "#..", "#..", "###"],
    "D": ["##.", "#.#", "#.#", "#.#", "##."],
    "E": ["###", "#..", "##.", "#..", "###"],
    "F": ["###", "#..", "##.", "#..", "#.."],
    "G": ["###", "#..", "#.#", "#.#", "###"],
    "H": ["#.#", "#.#", "###", "#.#", "#.#"],
    "I": ["###", ".#.", ".#.", ".#.", "###"],
    "J": ["..#", "..#", "..#", "#.#", "###"],
    "K": ["#.#", "#.#", "##.", "#.#", "#.#"],
    "L": ["#..", "#..", "#..", "#..", "###"],
    "M": ["#.#", "###", "###", "#.#", "#.#"],
    "N": ["###", "#.#", "#.#", "#.#", "#.#"],
    "O": ["###", "#.#", "#.#", "#.#", "###"],
    "P": ["###", "#.#", "###", "#..", "#.."],
    "Q": ["###", "#.#", "#.#", "###", "..#"],
    "R": ["###", "#.#", "##.", "#.#", "#.#"],
    "S": ["###", "#..", "###", "..#", "###"],
    "T": ["###", ".#.", ".#.", ".#.", ".#."],
    "U": ["#.#", "#.#", "#.#", "#.#", "###"],
    "V": ["#.#", "#.#", "#.#", "#.#", ".#."],
    "W": ["#.#", "#.#", "###", "###", "#.#"],
    "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "Y": ["#.#", "#.#", ".#.", ".#.", ".#."],
    "Z": ["###", "..#", ".#.", "#..", "###"],
    "-": ["...", "...", "###", "...", "..."],
    ":": ["...", ".#.", "...", ".#.", "..."],
    "!": [".#.", ".#.", ".#.", "...", ".#."],
}


def burst(w, h, seed, density):
    """Deterministic scattered-pixel blast, densest in the middle."""
    rng = random.Random(seed)
    cx, cy = (w - 1) / 2, (h - 1) / 2
    art = []
    for r in range(h):
        row = ""
        for c in range(w):
            d = (((c - cx) / (w / 2)) ** 2 + ((r - cy) / (h / 2)) ** 2) ** 0.5
            row += "#" if d <= 1.0 and rng.random() < density * (1.2 - d * 0.6) else "."
        art.append(row)
    return art


SHIP_BOOM = [burst(13, 8, 11, 0.85), burst(13, 8, 23, 0.6)]
UFO_BOOM = burst(16, 8, 5, 0.8)

# --- Sheet layout (x, y) -----------------------------------------------------------------------

PLACEMENTS = [
    (JELLY[0], 0, 0), (JELLY[1], 12, 0),
    (BEETLE[0], 24, 0), (BEETLE[1], 36, 0),
    (SKULL[0], 48, 0), (SKULL[1], 60, 0),
    (ALIEN_BOOM, 72, 0),
    (UFO, 84, 0),
    (UFO_BOOM, 102, 0),
    (SHIP, 0, 10), (SHIP_BOOM[0], 14, 10), (SHIP_BOOM[1], 28, 10),
    (BULLET, 42, 10),
    (BOMB[0], 44, 10), (BOMB[1], 48, 10),
]


def check(art):
    widths = {len(row) for row in art}
    assert len(widths) == 1, f"ragged sprite: {art}"


def blit(img, art, x, y):
    check(art)
    for r, row in enumerate(art):
        for c, ch in enumerate(row):
            if ch == "#":
                img.putpixel((x + c, y + r), (255, 255, 255, 255))


def build_sheet():
    img = Image.new("RGBA", (SHEET_W, SHEET_H), (0, 0, 0, 0))
    for art, x, y in PLACEMENTS:
        blit(img, art, x, y)
    for i, ch in enumerate(FONT_CHARS):
        blit(img, FONT[ch], i * FONT_PITCH, FONT_Y)
    return img


def build_icon():
    """Adaptive-style vector: dark tile with the beetle alien."""
    art = BEETLE[0]
    scale, ox, oy = 6, 21, 30
    path = []
    for r, row in enumerate(art):
        for c, ch in enumerate(row):
            if ch == "#":
                path.append(f"M{ox + c * scale},{oy + r * scale}h{scale}v{scale}h-{scale}z")
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        '    <path\n        android:fillColor="#050A06"\n        android:pathData="M0,0h108v108h-108z" />\n'
        '    <path\n        android:fillColor="#6BE36B"\n'
        f'        android:pathData="{"".join(path)}" />\n'
        "</vector>\n"
    )


if __name__ == "__main__":
    os.makedirs(os.path.dirname(SHEET_PATH), exist_ok=True)
    build_sheet().save(SHEET_PATH)
    with open(ICON_PATH, "w") as f:
        f.write(build_icon())
    print("wrote", SHEET_PATH)
    print("wrote", ICON_PATH)
