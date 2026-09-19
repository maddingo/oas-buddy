# OAS Buddy branding

<img src="oas-buddy-icon.svg" width="96" alt="OAS Buddy icon">

## The mark

A `{` brace beside a descending outline tree: the brace is the spec (JSON/YAML), the tree is the
structured editor that OAS Buddy actually is. It is deliberately not derived from the OpenAPI
Initiative's logo, which is trademarked.

| Role | Colour |
|---|---|
| Badge | `#0F243F` deep navy |
| Mark (brace, root node) | `#2DD4BF` teal |
| Tree branches and child nodes | `#7DD3FC` sky |

The badge is self-contained and holds contrast on both light and dark backgrounds, so there is no
separate dark-mode variant to keep in sync.

## Source of truth

`oas-buddy-icon.svg` is the **only** file anyone edits by hand. Everything else listed below is
generated. To change the icon, edit that file and run:

```
tools/generate-icons.sh
```

Requires `librsvg2-bin` (`rsvg-convert`), `imagemagick` and `icnsutils` (`png2icns`):

```
sudo apt install librsvg2-bin imagemagick icnsutils
```

### Generated files

| Path | Consumer |
|---|---|
| `docs/branding/oas-buddy-icon-512.png` | docs that cannot embed SVG |
| `oas-buddy-desktop/src/main/resources/no/maddin/oasbuddy/desktop/oas-buddy-icon.svg` | `AppIcon`, at runtime |
| `oas-buddy-desktop/src/main/icons/oas-buddy.svg` | shipped in the Linux packages, referenced by the `.desktop` file |
| `oas-buddy-desktop/src/main/icons/oas-buddy.png` | `jpackage --icon` on Linux |
| `oas-buddy-desktop/src/main/icons/oas-buddy.ico` | `jpackage --icon` on Windows |
| `oas-buddy-desktop/src/main/icons/oas-buddy.icns` | `jpackage --icon` on macOS |

They are committed so that CI never needs image tooling installed.

## Authoring constraint

Keep the SVG to `rect`, `path`, `circle` and `g` (for attribute inheritance), with plain solid
fills and strokes. `AppIcon` parses this file at runtime with a deliberately small parser, and
anything outside that subset is silently ignored rather than reported. `AppIconResourceTest`
asserts the constraint still holds.

Corollary: the `viewBox` must stay `0 0 256 256`, which is the canvas `AppIcon` scales against.

## Notes

- **macOS padding.** Unlike Linux and Windows, a macOS icon is expected to occupy ~80% of its
  canvas with transparent margin — the system implies the rounded-badge shape itself. The
  generator pads the `.icns` sources accordingly, so the icon doesn't look oversized in the Dock.
- **`icns` largest size.** `png2icns` from icnsutils 0.8.1 writes up to `ic09` (512px) and skips
  the 1024px `ic10` type. Harmless for Dock and Finder use; only the very largest Finder preview
  is upscaled.
- **Snapshots must be re-decoded.** `AppIcon.image()` encodes each rendered snapshot to PNG and
  decodes it again before handing it over. That round-trip is not redundant: JavaFX's GTK backend
  will not forward a `snapshot()`-produced `WritableImage` to the window manager, so raw snapshots
  in `Stage.getIcons()` leave the window with the default Java icon while `getIcons()` still looks
  correct. Verify with `xprop -id <window> _NET_WM_ICON` — absent means broken.
- **Why rasters exist at all.** JavaFX cannot load SVG, and `jpackage --icon` rejects it. The
  first is sidestepped by rendering the vector through the scene graph (see `AppIcon`); the
  second is not sidesteppable, which is why the generator exists.
