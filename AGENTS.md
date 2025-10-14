# Agent Instructions

## Project Overview
- Playground Android is an exploration ground for Kotlin-first Android development with Jetpack Compose, Clean Architecture, and kotlin-inject + Anvil for DI.
- The template favors modularity: features ship as isolated modules, domain logic stays UI-agnostic, and shared utilities live in `:common`.
- Aim for maintainable, testable code; surface risky assumptions early, and challenge weak requirements instead of accepting them.

## Architecture
- Core patterns: Unidirectional State Flow (USF) ViewModels, optional USF plugins for complex compositions, developer-owned Navigation 3 back stack, and compile-time DI with kotlin-inject + Anvil.
- Module responsibilities: `:app` orchestrates composition, `:features:*` own screens + navigation, `:domain:*` holds business logic, `:common:*` provides shared tooling, `:build-logic` enforces Gradle conventions.
- See `ARCHITECTURE.md` for detailed guidance, including plugin registration, DI scopes, and navigation wiring.
- See `ARCHITECTURE-USF.md` for the complete USF library (architecture deep dive, plugins, quickstart, testing, troubleshooting).

## Testing
- Unit tests rely on JUnit 5, `kotlinx.coroutines.test`, MockK, AssertJ, and Turbine; all public logic must have coverage.
- Test USF pipelines by driving events, capturing state/effects, and controlling virtual time; isolate plugin behavior when used.
- Run `make tests` for the suite or `make build` for full verification. Additional patterns and utilities are documented in `TESTING.md`.

## Important Commands
- `make build-debug` — Assemble a debug build without lint.
- `make build` — Full build including tests.
- `make clean` — Remove build outputs and caches.
- `make tests` — Execute all unit tests.
- `make lint` — Run Android/Kotlin lint checks.
- `make ktfmt` — Format staged Kotlin files.
- `make ktfmt-all` — Format every Kotlin file.

## Coding Conventions
- Optimize for legibility: prefer clear `if/else`, single-responsibility functions (<20 lines), and immutable data structures when possible.
- Enforce Kotlin style rules: PascalCase types, camelCase members, no star imports, always add trailing commas in multiline literals.
- Avoid Kotlin `object` except for true constants; rely on constructor injection with scoped classes (`@SingleIn`, `@ContributesBinding`, `@ContributesSubcomponent`).
- UI code is Compose-only; keep composables thin and delegate logic to USF ViewModels and domain layers.
- Logging: never include sensitive data; prefix messages with data marks (`"[FFF][CC] message"`).
- Concurrency: never use `GlobalScope` or raw threads; inject coroutine scopes, prefer suspend functions, and clean up resources via USF lifecycle hooks.
- Naming: skip generic suffixes like Manager/Helper/Util—choose domain terms that reveal intent.
- Comments document *why* decisions were made (space-shuttle style). Do not leave TODOs without owner/context.
- Instruction priority: user requests → project rules → professional standards → this document. Default to precision over politeness and surface flawed assumptions quickly.
