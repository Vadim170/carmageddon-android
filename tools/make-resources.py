"""Make the app's game-derived Android resources from game-data/.

Nothing drawn from the game is kept in the repository: the first-run unpacking
screens (the games' LOADSCRN.PIX) and the launcher icons are rendered from the
player's own data at build time.

Usage: make-resources.py <game-data dir> <output res dir>
"""
import os
import struct
import sys
import zlib


def chunks(data):
    off = 0
    while off + 8 <= len(data):
        cid, length = struct.unpack('>II', data[off:off + 8])
        yield cid, data[off + 8:off + 8 + length]
        off += 8 + length


def read_pixmap(path):
    """First pixelmap of a BRender .PIX/.PAL file: (width, height, row_bytes, bytes_per_pixel, pixels)."""
    header = None
    for cid, body in chunks(open(path, 'rb').read()):
        if cid == 0x3d:
            _, row_bytes, w, h = struct.unpack('>BHHH', body[:7])
            header = (w, h, row_bytes)
        elif cid == 0x03:
            _, row_bytes, w, h = struct.unpack('>BHHH', body[:7])
            header = (w, h, row_bytes)
        elif cid == 0x21 and header is not None:
            _, bpp = struct.unpack('>II', body[:8])
            return header + (bpp, body[8:])
    raise ValueError('no pixelmap in ' + path)


def read_palette(path):
    _, _, _, _, px = read_pixmap(path)
    # RGBX entries are stored as X, R, G, B
    return [(px[i * 4 + 1], px[i * 4 + 2], px[i * 4 + 3]) for i in range(256)]


def write_png(path, w, h, rgba):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    raw = b''.join(b'\0' + rgba[y * w * 4:(y + 1) * w * 4] for y in range(h))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)))
        f.write(chunk(b'IDAT', zlib.compress(raw, 9)))
        f.write(chunk(b'IEND', b''))


def loading_screen(game_dir):
    """The game's 320x200 loading screen as RGBA."""
    data = os.path.join(game_dir, 'DATA')
    pal = read_palette(os.path.join(data, 'REG', 'PALETTES', 'DRRENDER.PAL'))
    w, h, row_bytes, _, px = read_pixmap(os.path.join(data, 'PIXELMAP', 'LOADSCRN.PIX'))
    rgba = bytearray()
    for y in range(h):
        for x in range(w):
            rgba += bytes(pal[px[y * row_bytes + x]]) + b'\xff'
    return w, h, bytes(rgba)


def icon(screen, size=192):
    """Loading screen centred on a black square, nearest-neighbour."""
    w, h, rgba = screen
    out = bytearray(b'\0\0\0\xff' * size * size)
    top = (size - size * h // w) // 2
    for y in range(size):
        sy = (y - top) * w // size
        if not 0 <= sy < h:
            continue
        for x in range(size):
            sx = x * w // size
            out[(y * size + x) * 4:(y * size + x) * 4 + 4] = rgba[(sy * w + sx) * 4:(sy * w + sx) * 4 + 4]
    return size, size, bytes(out)


def main(game_data, res):
    for game_id, icon_name in (('c1', 'ic_launcher'), ('sp', 'ic_launcher_splat')):
        screen = loading_screen(os.path.join(game_data, game_id))
        write_png(os.path.join(res, 'drawable-nodpi', 'loading_%s.png' % game_id), *screen)
        write_png(os.path.join(res, 'mipmap-xxxhdpi', icon_name + '.png'), *icon(screen))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
