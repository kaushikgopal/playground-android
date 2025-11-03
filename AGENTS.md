# Agent Instructions

## How to Use This Document

This is the **primary entry point** for agents working on this project. It provides sufficient context for most decisions.

**When you need more detail:**
- Module structure, navigation, or DI patterns → @ARCHITECTURE.md
- Implementing/debugging USF ViewModels → @ARCHITECTURE-USF.md (comprehensive 3600+ line guide)
  - New screen? → "USF Quickstart Guide" section
  - Complex features? → "USF Plugin Architecture" section
  - Debugging? → "USF Troubleshooting Guide" section
- Testing patterns and utilities → @TESTING.md

## Project Overview
playground-android is a modern Android development playground showcasing best-in-class architectural patterns and practices. It serves as a reference implementation for building maintainable, testable Android applications using contemporary tooling and techniques.

**Technology stack**: Kotlin-first with Jetpack Compose, Clean Architecture, Unidirectional State Flow (USF) ViewModels, and compile-time DI via kotlin-inject + Anvil. Modular by design: features ship as isolated modules, domain logic stays UI-agnostic, and shared utilities live in `:common`.

## Important Commands
- `make` — Default build target (runs full build including tests)
- `make build-debug` — Assemble debug build without lint
- `make clean` — Remove build outputs and caches
- `make tests` — Execute all unit tests
- `make lint` — Run Android/Kotlin lint checks
- `make ktfmt` — Format staged Kotlin files
- `make ktfmt-all` — Format every Kotlin file

## Core Principles

### 1. Architecture First
- Clean separation of concerns with feature modules, domain logic, and shared utilities
- Testability as a first-class concern; every component should be easily testable in isolation
- Type-safe dependency injection with compile-time validation

### 2. Modern Development Practices
- Kotlin-first with coroutines for concurrency
- Jetpack Compose for declarative UI with minimal boilerplate
- Unidirectional data flow (USF) for predictable state management
- Immutable data structures to prevent bugs

### 3. Performance & Responsiveness
- Never block the UI; all long-running work happens on background threads
- Efficient state updates that minimize recomposition
- Proper lifecycle management to prevent leaks

### 4. Developer Experience
- Clear patterns that scale from simple to complex features
- Comprehensive documentation with practical examples
- Debugging-friendly architecture with observable state flows

## Key Architectural Features

### Core Components
- **USF ViewModels**: Unidirectional State Flow pattern for predictable state management
- **Navigation 3**: Type-safe, developer-owned back stack navigation
- **Modular Architecture**: Features as isolated modules with clear boundaries
- **Compile-time DI**: kotlin-inject + Anvil for fast, type-safe dependency injection

### Demonstrated Patterns
- Simple CRUD screens with loading, error, and success states
- Complex multi-step workflows with plugin composition
- Form validation and debounced input handling
- Async operations with proper cancellation
- Lifecycle-aware resource management
- Comprehensive testing patterns for all scenarios

### Areas of Focus
- Clean architecture with separation of UI, domain, and data layers
- Testable components with clear contracts
- Performance optimization techniques
- Error handling patterns
- State management best practices

## UI & Compose Patterns

### Compose Best Practices
- Thin composables that delegate logic to ViewModels
- State hoisting for reusable components
- Stable callbacks using `inputEventCallback` for reference stability
- Preview functions for rapid UI iteration

### State Management in UI
- Collect state with `collectAsState()` for reactive updates
- Handle effects with `LaunchedEffect(viewModel)` for one-time actions
- Avoid business logic in composables; keep them presentation-only

### Navigation
- Type-safe routes with serializable data classes
- Clear entry points via `EntryProviderInstaller`
- Back stack management through `Navigator` singleton

### Spacing & Layout
- **ALWAYS use AppSpacing values** from `domain.ui.AppSpacing` for all padding, spacing, and margins
- Available values: `dp4`, `dp8`, `dp12`, `dp16`, `dp20`, `dp24`, `dp32`, `dp64`
- **Never use hardcoded `.dp` values** (e.g., `16.dp`) - always use the predefined spacing constants
- This ensures consistent spacing across the entire app

## Architecture

### Module Layout
- `:app` — Entry point; wires dependency graph, root composables, navigation display
- `:features:*` — Independent feature modules that demonstrate different patterns and use cases
- `:domain:*` — Shared business logic and domain models
- `:common:*` — Cross-cutting utilities (logging, networking, USF core, UI components)
- `:build-logic` — Gradle convention plugins

### Unidirectional State Flow (USF)
- Every screen uses `UsfViewModel<Event, UiState, Effect>` with immutable state and one-off effects
- Events process on the main thread (`Dispatchers.Main.immediate`); keep handlers fast and use `offload { }` for heavy work
- Events flow through `process` inside `ResultScope`; state updates via `updateState { … }`, side effects via `emitEffect`
- Model callbacks as part of state to keep composables dumb:
  - Use `inputEventCallback` for simple events: `onAction = inputEventCallback(Event.Action)`
  - Use `inputEventCallback` for parameterized callbacks: `onTextChanged = inputEventCallback(Event::TextChanged)`
  - These helpers ensure **reference stability** for Compose, preventing unnecessary recompositions
  - Callbacks are created once and preserved via `.copy()`, ensuring stable references across state updates
- Debug builds enable StrictMode (see `StrictModeInitializer`) to flag blocking work on main; fix violations by moving logic into `offload { }` or background coroutines

**USF Documentation:**
- **@USF.md** — Core patterns for 90% of cases (basic ViewModels, UI integration, testing)
- **@USF-QUICKSTART.md** — Complete tutorial if new to USF
- **@USF-PLUGINS.md** — Plugin composition for complex features (read only when needed)
- **@USF-TESTING.md** — Comprehensive testing patterns (read only when writing tests)
- **@USF-TROUBLESHOOTING.md** — Debugging guide (read only when stuck)

**Agent instructions:** Read @USF.md for basic patterns. Read specialized guides only when the task requires them. Don't read all files upfront—pull specific guides as needed.

### Navigation 3 Back Stack
- Developer-owned: singleton `Navigator` keeps `SnapshotStateList<NavRoute>`
- Routes are serializable, compile-time safe objects
- Features contribute `EntryProviderInstaller` to map routes to composables
- `navigator.goTo(route)` / `navigator.goBack()` / `navigator.clearAndGoTo(route)`

### Dependency Injection (kotlin-inject + Anvil)
- `@Inject` constructors, `@SingleIn(scope)` for lifetime
- `@ContributesBinding(scope)` for interface implementations
- Feature-scoped subcomponents: `@ContributesSubcomponent(FeatureScope::class)`
- Prefer constructor injection; use `Lazy<T>` for deferred work

## Testing
- JUnit 5, `kotlinx.coroutines.test`, MockK, AssertJ, Turbine
- Test USF pipelines by driving events, capturing state/effects, controlling virtual time
- All public logic must have unit test coverage
- Run `make tests` or `make build` for full verification
- See `TESTING.md` for patterns and utilities


## Coding Conventions

### General
- Optimize for legibility: clear `if/else`, single-responsibility functions (<20 lines), immutable data
- PascalCase types, camelCase members, no star imports, trailing commas in multiline literals
- Avoid Kotlin `object` except for true constants; rely on scoped DI classes
- UI is Compose-only; keep composables thin, delegate logic to ViewModels and domain layers
- **Spacing**: ALWAYS use `AppSpacing` constants (dp4, dp8, dp16, etc.) - never hardcode `.dp` values

### Performance & Best Practices
- **Performance**: never block the UI; prefer incremental loading and optimistic updates
- **Error Handling**: explicit error states in UI, never silently catch-and-ignore exceptions
- **State Management**: immutable state updates, clear event/state/effect boundaries
- **Resource Management**: proper cleanup in lifecycle hooks, cancel coroutines when no longer needed

### Logging, Concurrency, Naming
- Logging: never include sensitive data; use clear prefixes for module identification (`"[MOD] message"`)
- Concurrency: no `GlobalScope` or raw threads; inject scopes, use suspend functions, clean up via USF lifecycle
- Naming: skip generic suffixes (Manager/Helper/Util); choose descriptive, domain-specific terms (e.g., `DataValidator`, `UserRepository`)

### Comments & Instruction Priority
- Comments document *why* decisions were made (space-shuttle style); no TODOs without owner/context
- Instruction priority: **user requests → project rules → professional standards → this document**
- Default to precision over politeness; surface flawed assumptions immediately

## Additional Resources

- @ARCHITECTURE.md — Detailed module responsibilities, DI scopes, navigation wiring, UI composition patterns
- @USF.md — Core USF patterns for daily development (basic ViewModels, UI integration, testing essentials)
- @USF-QUICKSTART.md — Complete USF tutorial from scratch
- @USF-PLUGINS.md — Advanced plugin composition for complex features
- @USF-TESTING.md — Comprehensive testing patterns and utilities
- @USF-TROUBLESHOOTING.md — Debugging guide for common USF issues
- @TESTING.md — Test patterns, utilities, and conventions for JUnit 5 + Coroutines

# ExecPlans
When writing complex features or significant refactors, use an ExecPlan (as described in @.ai/plans/PLANS.md or @~/.ai/plans/PLANS.md) from design to implementation. Write new plans to the @.ai/plans directory. Place any temporary research, clones etc., in the .gitignored subdirectory @.ai/plans/tmp
