#!/usr/bin/env python3
"""Generate the PLACEHOLDER Observer texture for Eidolon Drift (M2).

License-clean: a procedurally-filled 64x64 player-format skin (no downloaded art), so it
ships freely under the mod's MIT license. The Observer reads as a near-black, faintly
uneven human silhouette — "barely visible" (GDD §8). Final art replaces it in M8.

    python tools/gen_observer_texture.py

Uses only the Python stdlib (zlib + struct) — no Pillow/numpy needed.
"""
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets",
                   "eidolon_drift", "textures", "entity", "observer.png")

W = H = 64


def _pixel(x, y):
    """Near-black with a faint top-to-bottom gradient and a touch of cold blue."""
    # Slightly darker toward the bottom; a barely-there blue cast so it isn't pure #000.
    base = 14 - int(6 * (y / H))          # 14 → 8
    base = max(6, base)
    r = base
    g = base
    b = base + 4                          # cold tint
    return (r, g, b, 255)


def _png(width, height, rows):
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = bytearray()
    for row in rows:
        raw.append(0)                     # filter type 0 (None)
        raw.extend(row)
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)  # 8-bit RGBA
    return (sig
            + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))


def main():
    rows = []
    for y in range(H):
        row = bytearray()
        for x in range(W):
            row.extend(_pixel(x, y))
        rows.append(row)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "wb") as f:
        f.write(_png(W, H, rows))
    print(f"  wrote {os.path.relpath(OUT, ROOT)}  ({os.path.getsize(OUT)} bytes)")


if __name__ == "__main__":
    main()
