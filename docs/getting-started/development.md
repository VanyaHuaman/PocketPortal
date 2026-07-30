# Development setup

PocketPortal is a Kotlin modular monolith with strict dependency direction.

## Verify changes

```bash
./scripts/verify.sh
```

Tests are part of each feature's definition of done. Domain and application tests must not require Ktor, SQLite, ADB, or a physical device. Hardware tests will remain explicitly opt-in.

## Modules

| Module | Responsibility |
| --- | --- |
| `domain` | Framework-free business models and rules |
| `application` | Use cases and external-system ports |
| `infrastructure` | ADB, process, time, and future database adapters |
| `web` | Ktor routes and transport models |
| `app` | Configuration and dependency composition |

Dependencies point inward. Domain logic does not belong in Ktor routes, database queries, process adapters, or frontend components.

## Documentation

Build the documentation with:

```bash
./scripts/docs.sh build
```

Preview it locally with:

```bash
./scripts/docs.sh serve
```
