"""Convert a raw MODE1/2352 CD image (the GOG .GOG files) to a 2048-byte-sector ISO."""
import sys


def main(src, dst):
    with open(src, 'rb') as f, open(dst, 'wb') as o:
        while True:
            sector = f.read(2352)
            if len(sector) < 2352:
                break
            o.write(sector[16:16 + 2048])


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
