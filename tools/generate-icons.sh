#!/usr/bin/env bash
#
# Regenerates every icon artifact from docs/branding/oas-buddy-icon.svg.
#
# The SVG is the only file anyone edits by hand. Everything this script writes is committed so
# that CI never needs image tooling, but nothing here should ever be edited directly -- rerun the
# script instead.
#
#   Requires: librsvg2-bin (rsvg-convert), imagemagick (magick), icnsutils (png2icns)
#   Usage:    tools/generate-icons.sh

set -euo pipefail

cd "$(dirname "$0")/.."

SOURCE="docs/branding/oas-buddy-icon.svg"
RESOURCES="oas-buddy-desktop/src/main/resources/no/maddin/oasbuddy/desktop"
ICONS="oas-buddy-desktop/src/main/icons"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

for tool in rsvg-convert magick png2icns; do
    command -v "$tool" >/dev/null || {
        echo "error: $tool not found. Install: sudo apt install librsvg2-bin imagemagick icnsutils" >&2
        exit 1
    }
done

render() { rsvg-convert -w "$1" -h "$1" -o "$2" "$SOURCE"; }

echo "==> $SOURCE"

# 1. The SVG itself, onto the classpath. AppIcon parses this at runtime to draw the window icon,
#    and the Linux packages install it into the hicolor theme as a true scalable icon.
mkdir -p "$RESOURCES"
cp "$SOURCE" "$RESOURCES/oas-buddy-icon.svg"
echo "    -> $RESOURCES/oas-buddy-icon.svg"

# 2. The SVG again, beside the jpackage inputs. The Linux packages ship this file and install it
#    into /usr/share/icons/hicolor/scalable/apps -- the one platform that takes a real vector icon.
mkdir -p "$ICONS"
cp "$SOURCE" "$ICONS/oas-buddy.svg"
echo "    -> $ICONS/oas-buddy.svg"

# 3. Raster sizes. These exist only because jpackage cannot accept an SVG.
for size in 16 32 48 64 128 256 512 1024; do
    render "$size" "$STAGE/icon-$size.png"
done

# Linux: jpackage takes a single PNG.
cp "$STAGE/icon-1024.png" "$ICONS/oas-buddy.png"
echo "    -> $ICONS/oas-buddy.png"

# Windows: one .ico carrying every size the shell picks between.
magick "$STAGE/icon-16.png" "$STAGE/icon-32.png" "$STAGE/icon-48.png" \
       "$STAGE/icon-64.png" "$STAGE/icon-128.png" "$STAGE/icon-256.png" \
       "$ICONS/oas-buddy.ico"
echo "    -> $ICONS/oas-buddy.ico"

# macOS: unlike Linux/Windows, a Big Sur icon is expected to sit at ~80% of its canvas with
# transparent margin around it -- the rounded-badge shape is the system's job to imply, not ours
# to fill. Without this the icon looks oversized next to every other one in the Dock.
for size in 16 32 128 256 512 1024; do
    magick "$STAGE/icon-$size.png" -resize 80% -background none \
           -gravity center -extent "${size}x${size}" "$STAGE/mac-$size.png"
done
png2icns "$ICONS/oas-buddy.icns" \
    "$STAGE/mac-16.png" "$STAGE/mac-32.png" "$STAGE/mac-128.png" \
    "$STAGE/mac-256.png" "$STAGE/mac-512.png" "$STAGE/mac-1024.png" >/dev/null
echo "    -> $ICONS/oas-buddy.icns"

# 4. A raster for documentation that cannot embed SVG.
cp "$STAGE/icon-512.png" "docs/branding/oas-buddy-icon-512.png"
echo "    -> docs/branding/oas-buddy-icon-512.png"

echo "==> done"
