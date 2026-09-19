# CLAUDE.md

## Project: OAS Buddy

An OpenAPI Specification (OAS) editor.

### Vision
- Edit OpenAPI Specs stored on disk locally, and (in a later phase) collaboratively on a server with colleagues.
- Support all versions of OAS (2.0/Swagger, 3.0, 3.1, ...) — MVP targets OAS 3.0 only; architecture designed to extend.
- Distribution: standalone desktop app first. An IntelliJ plugin is a likely later front-end reusing the same core engine.
- Programming language: Java (baseline: Java 25 LTS).

### MVP scope decisions
- UI: structured/form-based editor (no raw-text editing in MVP).
- OAS version: 3.0 only for MVP.
- Editable sections: Info, Servers, Paths/Operations, Components/Schemas — "everything at once" rather than a narrower slice.
- File formats: both YAML and JSON.
- Out of scope for MVP: OAS 3.1/2.0 support, server/collaboration backend, IntelliJ plugin, YAML comment preservation, external $ref resolution, raw-text/source view.

### Architecture
- Maven groupId and Java base package: `no.maddin.oasbuddy` (e.g. `no.maddin.oasbuddy.core.document`, `no.maddin.oasbuddy.desktop`). Artifact ids stay `oas-buddy` / `oas-buddy-core` / `oas-buddy-desktop`.
- Maven multi-module project.
  - `oas-buddy-core`: no UI deps. Document model, file I/O, validation. Reusable by future server/plugin front-ends.
  - `oas-buddy-desktop`: JavaFX app. Structured editor UI, depends only on oas-buddy-core's public API.
- Document model: Jackson-based ordered tree (ObjectNode/ArrayNode, LinkedHashMap-backed) as the single source of truth — preserves key order for git-friendly diffs. Loaded via jackson-dataformat-yaml / jackson-databind.
- Typed facade on top of the tree (OasDocument -> Info, Servers, PathItem, Operation, Schema, ...) for the UI to bind to. Reads/writes through to the same tree nodes (no separate DTO sync). MVP facade covers OAS 3.0 only.
- Validation: swagger-parser / swagger-core used only as a validation engine (spec-compliance errors/warnings), never as the editable model. This was a deliberate choice over using swagger-parser's own POJO model as the source of truth, because that model doesn't reliably preserve key order/round-trip fidelity and has only partial OAS 3.1 support — both matter for a git-collaborated editor.
- Also considered and rejected: KaiZen OpenAPI Parser (round-trip-preserving, editor-focused design, but unmaintained since 2019, OAS 3.0 only, EPL-1.0). openapi-processor/openapi-parser (actively maintained, supports 3.0/3.1/3.2, Apache 2.0, but not documented as round-trip/write-back oriented — a general parsing/validation library, not an editing model).

### Tech stack
- Language/runtime: Java 25 (LTS)
- Build tool: Maven
- UI framework: JavaFX, themed with [AtlantaFX](https://github.com/mkpaz/atlantafx) (Primer Light/Dark, switchable via View menu) instead of the default JavaFX/Modena look. Shared pane-building helpers live in `no.maddin.oasbuddy.desktop.pane.FormFields` (`root()`, `grid()`, `heading()`, `column()`, `columnHeading()`, `headerWithDelete()`) — every editor pane uses them for consistent spacing/typography rather than hand-rolling layout per pane.
- Theme choice (light/dark) persists across runs via `java.util.prefs.Preferences` (`MainApp.PREFS`, key `"theme"`) — plain JDK API, no extra dependency. If more settings accumulate later (window size, recent files), reconsider in favor of a JSON settings file for structured data; `Preferences` was chosen for now because theme is the only persisted setting.
- OAS document tree: Jackson (jackson-databind, jackson-dataformat-yaml)
- OAS validation: swagger-parser v3 / swagger-core
- Testing: JUnit 5 (core), TestFX (desktop, minimal for MVP)
- License: Apache 2.0

### CI
- GitHub Actions (`.github/workflows/ci.yml`): builds and runs the full test suite (Java 25, `mvn test`) on push to `master` and on pull requests. The desktop module's TestFX tests need a display, so the job runs under Xvfb (`xvfb-run -a mvn -B test`).
- GitHub Actions (`.github/workflows/package.yml`): builds native installers (.deb/.rpm/.pkg/.exe) via `jpackage` across an ubuntu/macos/windows matrix. Triggered manually or on a `v*` tag push, not on every push/PR — jpackage needs OS-native tools (dpkg-deb, rpmbuild, Xcode command line tools, WiX Toolset) that only exist on their respective OS, so this can't run as a single job. Installers are always uploaded as workflow-run artifacts; on a tag push they're additionally attached to the matching GitHub Release via `softprops/action-gh-release` (creating the release if it doesn't exist yet), using the version extracted from the tag name (see Versioning below).
- GitHub Actions (`.github/workflows/release.yml`): manual (`workflow_dispatch`, takes a `version` input) — creates and pushes an annotated `vX.Y.Z` tag, then explicitly triggers `package.yml` via `gh workflow run package.yml --ref vX.Y.Z`. No commits, no POM changes. The explicit trigger is required: a tag pushed with the default `GITHUB_TOKEN` does not fire other workflows' `push` triggers (GitHub's anti-recursion guard), so relying on `package.yml`'s `push: tags: ['v*']` trigger alone doesn't work when the tag came from a workflow. That trigger still fires normally for a tag pushed by a human with their own credentials outside Actions.

### Versioning
- **The release version lives in the `Release` workflow, not in the POMs.** The version numbers checked into `pom.xml` are placeholders and carry no meaning: `<revision>` (and `jpackage.appVersion`) are only defaults for ordinary local/CI builds, and every released artifact gets its version from the workflow input instead. Don't bump them to "prepare a release", and don't read them as "the current version" — `git tag` and the GitHub Releases page are the source of truth for what has shipped.
- CI-friendly versions: the POMs never hold a real version. Root `pom.xml` declares `<version>${revision}</version>` with a `-SNAPSHOT` placeholder as the default; both child modules' `<parent><version>` also reference `${revision}`. A release build overrides it with `-Drevision=X.Y.Z` — nothing in git ever needs a version-bump commit.
- Rejected: `maven-release-plugin` — it commits and pushes directly to the branch twice (version bump, next-`-SNAPSHOT` bump) and tags itself, which fights GitHub's branch-protection/PR-based workflow more than it helps, and was overkill for this project's needs.
- To cut a release: run the `Release` workflow with a version (e.g. `1.2.0`) → it tags `v1.2.0` → `package.yml` picks that up, builds installers with `-Drevision=1.2.0 -Djpackage.appVersion=1.2.0`, and attaches them to the GitHub Release for that tag.

### Packaging (jpackage)
- `oas-buddy-desktop/pom.xml` has three OS-gated Maven profiles (`jpackage-linux` → DEB + RPM + a portable APP_IMAGE, `jpackage-mac` → PKG, `jpackage-windows` → EXE), each producing a self-contained bundle (own JVM, no separate Java install needed on the target machine) via the `org.panteleyev:jpackage-maven-plugin`.
- The Linux APP_IMAGE isn't an installer — it's a plain directory (own JVM + native launcher script) meant for direct download/manual install, and as the fetch target for a future AUR `-bin` package (Arch/CachyOS doesn't understand `.deb`/`.rpm`; real AUR packaging is deferred). `package.yml` tars it into `oas-buddy-linux-x86_64.tar.gz` and deletes the raw directory before the artifact-upload/release-attach steps, since those glob `target/dist/*` and can't handle a raw directory as an asset.
- Opt-in only: profiles activate on `-Djpackage` (combined with an OS check), so a plain `mvn package` is unaffected. Run `mvn -Djpackage package` on the target OS — the matching profile is picked automatically. Output lands in `oas-buddy-desktop/target/dist/`.
- Requires `dpkg-deb` (Linux/DEB), `rpmbuild` (Linux/RPM), Xcode command line tools (macOS/PKG), or WiX Toolset (Windows/EXE) to be installed locally — all preinstalled on GitHub's hosted runners except `rpmbuild` on Ubuntu, which the `package.yml` workflow installs explicitly.
- `jpackage --app-version` rejects a `-SNAPSHOT` suffix, so the desktop module tracks a separate `jpackage.appVersion` property instead of reusing `project.version`/`revision` directly. Its checked-in value is a placeholder like every other version in the POMs — a release build sets both it and `revision` explicitly from the workflow input (see Versioning above).
- The one case where a placeholder version still bites: macOS PKG rejects a major version of `0`, so a local `mvn -Djpackage package` on a Mac fails unless `-Djpackage.appVersion=` is passed with a `1.x`-or-later value. CI releases always pass it explicitly and are unaffected.

### Branding / app icon
- `docs/branding/oas-buddy-icon.svg` is the **single hand-edited source** for the icon (a `{` brace beside an outline tree — spec + structured editor; deliberately not derived from the OpenAPI Initiative's trademarked logo). Palette: badge `#0F243F`, mark `#2DD4BF`, tree `#7DD3FC`. Everything else is generated by `tools/generate-icons.sh` and committed, so CI never needs image tooling. See `docs/branding/README.md`.
- The in-app icon is **rendered as live vector, not loaded from a bitmap**: `desktop.AppIcon` parses that SVG with the JDK's built-in DOM parser (no new dependency) into a JavaFX scene graph and `snapshot()`s it at each size `MainApp` asks for.
- **Gotcha, learned the hard way:** `AppIcon.image()` re-encodes each snapshot to PNG and decodes it again before returning. This looks redundant but is load-bearing — the GTK glass backend does *not* forward a `snapshot()`-produced `WritableImage` to the window manager, so putting raw snapshots in `Stage.getIcons()` leaves X11's `_NET_WM_ICON` unset and the window/taskbar silently keeps the default Java icon, even though `getIcons()` looks perfectly populated. A decoded image propagates; a snapshot doesn't. Confirmed by running two otherwise-identical stages side by side and comparing `xprop -id <win> _NET_WM_ICON`. `AppIconRenderTest.imagesAreDecodedNotRawSnapshots` guards it, since no pixel assertion can catch this. To check by hand: `xprop -root _NET_CLIENT_LIST`, then `xprop -id <id> _NET_WM_ICON`. JavaFX's `Image` can't read SVG, and the obvious alternative — copying the path data into Java constants — would have created a second copy of the geometry that silently drifts from the first. The parser deliberately handles only `rect`/`path`/`circle`/`g`; `AppIconResourceTest` asserts the SVG stays inside that subset, since anything else is ignored rather than reported.
- Rasters exist **only** because `jpackage --icon` rejects SVG and needs a different format per OS (PNG/ICO/ICNS). The format is selected by one `jpackage.icon` property that each `jpackage-*` profile overrides, referenced once in the shared `pluginManagement` config.
- Linux gets a genuinely scalable icon: the SVG is copied into the jpackage input (so it ships inside the package) and `src/main/jpackage/linux/OAS Buddy.desktop` — supplied via `--resource-dir` — points `Icon=` at its absolute installed path. An absolute path was chosen over installing into the hicolor theme under a theme name, because a theme name would require vendoring jpackage's `postinst`/`postrm` *and* its RPM `.spec` template just to copy one file; the absolute path covers DEB and RPM identically with nothing to keep in sync against the JDK. Resource filenames were discovered with `jpackage --verbose`, which logs each template it uses — don't guess them.
- macOS icons are expected to occupy ~80% of their canvas with transparent margin (the system implies the badge shape), unlike Linux/Windows full-bleed, so the generator pads the `.icns` sources. `png2icns` (icnsutils 0.8.1) tops out at the 512px `ic09` type and skips 1024px `ic10`.

### Removing things from the document
- Every removal (schema, path, operation) goes through a confirmation: `no.maddin.oasbuddy.desktop.pane.RemovalConfirmation` is a one-method interface (`confirm(question, details)`) with a `dialog()` implementation for the running app, so the removal flows are testable without driving a modal `Alert`. `SchemaRemoval` and `PathRemoval` compose the `details` text and apply the change; the panes only *report* a removal request via a callback and never mutate the document themselves.
- Removing a schema first scans the whole raw tree for `$ref`s to it (`core.model.SchemaReferences`) and lists them in the dialog; the refs are left dangling for the validation panel to report rather than being rewritten. Removing a path lists the operations that go with it. Nothing can `$ref` a path or an operation, so those need no scan.
- After any removal `MainApp.refreshAndSelect(labels...)` rebuilds the outline and selects a surviving node by label, so the editor is never left showing something that was just deleted.

### Error handling principles
- Malformed YAML/JSON on open: show error with line/column, never crash.
- Spec validation errors/warnings: shown in a non-blocking panel; editing and saving an invalid/WIP spec is always allowed.
- File I/O errors: dialog with retry.

### Roadmap beyond MVP (directional, not committed)
1. OAS 3.1 support (extend typed facade)
2. Swagger 2.0 support
3. YAML comment preservation on round-trip
4. Server backend + real-time collaboration with colleagues
5. IntelliJ plugin front-end reusing oas-buddy-core
6. External $ref resolution, raw-text/source split view
