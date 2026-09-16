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
- UI framework: JavaFX
- OAS document tree: Jackson (jackson-databind, jackson-dataformat-yaml)
- OAS validation: swagger-parser v3 / swagger-core
- Testing: JUnit 5 (core), TestFX (desktop, minimal for MVP)
- License: Apache 2.0

### CI
- GitHub Actions (`.github/workflows/ci.yml`): builds and runs the full test suite (Java 25, `mvn test`) on push to `master` and on pull requests. The desktop module's TestFX tests need a display, so the job runs under Xvfb (`xvfb-run -a mvn -B test`).

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
