#!/usr/bin/env bash
# Builds game-data/ from your own GOG "Carmageddon Max Pack" installer.
#
# The GOG release keeps part of the game (key maps, all cutscenes) only on the CD
# images it mounts in DOSBox, so the data for each game is the CD's DATA folder
# with the installed DATA and MUSIC folders on top, the way the original installer
# laid it out.
#
#   game-data/c1/{DATA,MUSIC}   Carmageddon
#   game-data/sp/{DATA,MUSIC}   Carmageddon Splat Pack
#
# Requires: innoextract, python3, bsdtar (macOS: built in; Debian/Ubuntu: libarchive-tools)
#
# Usage: tools/prepare-data.sh path/to/setup_carmageddon_max_pack_*.exe
set -euo pipefail

if [ $# -ne 1 ] || [ ! -f "$1" ]; then
    echo "usage: $0 path/to/setup_carmageddon_max_pack_*.exe" >&2
    exit 2
fi
for tool in innoextract python3 bsdtar; do
    command -v "$tool" >/dev/null || { echo "$tool is required" >&2; exit 1; }
done

INSTALLER="$1"
REPO="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$REPO/game-data"
WORK="$OUT/.work"

rm -rf "$OUT"
mkdir -p "$WORK"
trap 'rm -rf "$WORK"' EXIT

echo "Unpacking installer..."
innoextract -s -d "$WORK/installer" "$INSTALLER"

# <installed folder> <CD image> <game id>
for game in "CARMA GAME.GOG c1" "CARSPLAT SPLAT.GOG sp"; do
    set -- $game
    src="$WORK/installer/$1"
    dst="$OUT/$3"
    echo "Preparing $3 from $1..."

    python3 -I "$REPO/tools/bin2iso.py" "$src/$2" "$WORK/$3.iso"
    mkdir -p "$dst"
    bsdtar -xf "$WORK/$3.iso" -C "$dst" DATA
    rm -f "$WORK/$3.iso"
    chmod -R u+w "$dst"

    # installed files win over the CD ones
    cp -R "$src/DATA" "$src/MUSIC" "$dst/"
    chmod -R u+w "$dst"

    # every CD file is present, so the game must not go looking for a CD drive
    find "$dst/DATA" -maxdepth 1 -iname 'paths.txt' -delete
done

du -sh "$OUT"/c1 "$OUT"/sp
echo "Done: $OUT"
