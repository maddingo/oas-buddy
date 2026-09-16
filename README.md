# OAS Buddy

[![CI](https://github.com/maddingo/oas-buddy/actions/workflows/ci.yml/badge.svg)](https://github.com/maddingo/oas-buddy/actions/workflows/ci.yml)

A desktop editor for OpenAPI Specification (OAS) documents.

See [CLAUDE.md](CLAUDE.md) for the full project vision, MVP scope, and architecture decisions.

## Modules

- `oas-buddy-core` — document model, file I/O, and validation. No UI dependencies.
- `oas-buddy-desktop` — JavaFX desktop application.

## Requirements

- Java 25 and Maven, pinned via [mise](https://mise.jdx.dev/) (`mise install`).

## Build and run

```
mvn test                        # run all tests
cd oas-buddy-desktop
mvn javafx:run                  # run the desktop app
```

The `javafx:run` plugin prefix only resolves correctly when Maven is invoked from inside
`oas-buddy-desktop` itself; running it from the repo root via `-pl oas-buddy-desktop -am` fails
with "No plugin found for prefix 'javafx'".
