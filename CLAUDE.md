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
- UI framework: JavaFX, themed with [AtlantaFX](https://github.com/mkpaz/atlantafx) (Primer Light/Dark, switchable via View menu) instead of the default JavaFX/Modena look. Shared pane-building helpers live in `no.maddin.oasbuddy.desktop.pane.FormFields` (`root()`, `grid()`, `heading()`) — every editor pane uses them for consistent spacing/typography rather than hand-rolling layout per pane.
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
- CI-friendly versions: the POMs never hold a real version. Root `pom.xml` declares `<version>${revision}</version>` with `<revision>0.1.0-SNAPSHOT</revision>` as the default (used by every ordinary local/CI build); both child modules' `<parent><version>` also reference `${revision}`. A release build overrides it with `-Drevision=X.Y.Z` — nothing in git ever needs a version-bump commit.
- Rejected: `maven-release-plugin` — it commits and pushes directly to the branch twice (version bump, next-`-SNAPSHOT` bump) and tags itself, which fights GitHub's branch-protection/PR-based workflow more than it helps, and was overkill for this project's needs.
- To cut a release: run the `Release` workflow with a version (e.g. `1.2.0`) → it tags `v1.2.0` → `package.yml` picks that up, builds installers with `-Drevision=1.2.0 -Djpackage.appVersion=1.2.0`, and attaches them to the GitHub Release for that tag.

### Packaging (jpackage)
- `oas-buddy-desktop/pom.xml` has three OS-gated Maven profiles (`jpackage-linux` → DEB + RPM, `jpackage-mac` → PKG, `jpackage-windows` → EXE), each producing a self-contained native installer (bundled JVM, no separate Java install needed on the target machine) via the `org.panteleyev:jpackage-maven-plugin`.
- Opt-in only: profiles activate on `-Djpackage` (combined with an OS check), so a plain `mvn package` is unaffected. Run `mvn -Djpackage package` on the target OS — the matching profile is picked automatically. Output lands in `oas-buddy-desktop/target/dist/`.
- Requires `dpkg-deb` (Linux/DEB), `rpmbuild` (Linux/RPM), Xcode command line tools (macOS/PKG), or WiX Toolset (Windows/EXE) to be installed locally — all preinstalled on GitHub's hosted runners except `rpmbuild` on Ubuntu, which the `package.yml` workflow installs explicitly.
- `jpackage --app-version` rejects a `-SNAPSHOT` suffix, so the desktop module tracks a separate `jpackage.appVersion` property (default `0.1.0`) instead of reusing `project.version`/`revision` directly — a release build sets both to the same value explicitly (see Versioning above).

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
