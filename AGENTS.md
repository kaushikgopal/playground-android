# Agent Instructions

## How to Use This Document

This is the **primary entry point** for agents working on this project. It
provides sufficient context for most decisions.

**When you need more detail:**

- USF ViewModels → See `common/usf/USF.md` and related docs in that module
- Navigation patterns → See `common/navigation/NAVIGATION.md`
- Testing patterns and utilities → See @TESTING.md

## USF Documentation (in `common/usf/`)

- **USF.md** — Core patterns for 90% of cases (basic ViewModels, UI integration,
  testing)
- **USF-QUICKSTART.md** — Complete tutorial if new to USF
- **USF-PLUGINS.md** — Plugin composition for complex features (read only when
  needed)
- **USF-TESTING.md** — Comprehensive testing patterns (read only when writing
  tests)
- **USF-TROUBLESHOOTING.md** — Debugging guide (read only when stuck)

**Agent instructions:** Read `common/usf/USF.md` for basic patterns. Read
specialized guides only when the task requires them. Don't read all files
upfront—pull specific guides as needed.

## Project Overview

playground-android is a modern Android development playground showcasing
best-in-class architectural patterns and practices. It serves as a reference
implementation for building maintainable, testable Android applications using
contemporary tooling and techniques.

**Technology stack**: Kotlin-first with Jetpack Compose, Clean Architecture,
Unidirectional State Flow (USF) ViewModels, and compile-time DI via Metro
(compiler plugin + graph extensions). Modular by design: features ship as
isolated modules, domain logic stays UI-agnostic, and shared utilities live in
`:common`.

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

- Clean separation of concerns with feature modules, domain logic, and shared
  utilities
- Testability as a first-class concern; every component should be easily
  testable in isolation
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

# Architecture

## Module Layout

- `:app` — Entry point; wires dependency graph, root composables, navigation
  display
- `:features:*` — Independent feature modules that expose navigation routes, DI
  components, and USF-driven ViewModels for their screens
- `:domain:*` — Shared business logic and domain models; no Android UI
  dependencies
- `:common:*` — Cross-cutting utilities (logging, networking, USF core, UI
  components, lint rules) that features and domains depend on
- `:build-logic` — Gradle convention plugins that enforce consistent
  configuration across modules

## Unidirectional State Flow (USF)

- Every screen uses `UsfViewModel<Event, UiState, Effect>` with immutable state
  and one-off effects
- Events process on the main thread (`Dispatchers.Main.immediate`); keep
  handlers fast and use `offload { }` for heavy work
- Events flow through `process` inside `ResultScope`; state updates via
  `updateState { … }`, side effects via `emitEffect`
- Model callbacks as part of state to keep composables dumb:
  - Use `inputEventCallback` for simple events:
    `onAction = inputEventCallback(Event.Action)`
  - Use `inputEventCallback` for parameterized callbacks:
    `onTextChanged = inputEventCallback(Event::TextChanged)`
  - These helpers ensure **reference stability** for Compose, preventing
    unnecessary recompositions
  - Callbacks are created once and preserved via `.copy()`, ensuring stable
    references across state updates
- Debug builds enable StrictMode (see `StrictModeInitializer`) to flag blocking
  work on main; fix violations by moving logic into `offload { }` or background
  coroutines
- Naming convention: abbreviate long feature names (e.g., `ULEvent`,
  `ULUiState`, `ULEffect` for `UserListScreen`)

### Plugins for Complex Screens

- Compose ViewModels from reusable `UsfPluginInterface` implementations when
  multiple concerns (search, pagination, filtering) must coexist
- Register plugins via
  `register(plugin = …, mapEvent = …, applyState = …, mapEffect = …, transformEffect = …)`
  to translate between ViewModel and plugin types
- Plugins can hold internal state and react to subscription lifecycle
  (`onSubscribed`, `onUnsubscribed`) for resource management
- Example use cases: search plugin with debouncing, pagination plugin for lists,
  form validation plugin, sync plugin for background operations
- Prefer plugins when logic is reusable or can be tested independently; skip
  them for simple, single-responsibility screens
- See `common/usf/USF-PLUGINS.md` for detailed plugin implementation patterns

## Navigation 3 Back Stack

- Developer-owned: singleton `Navigator` (DI scoped to `AppScope`) keeps a
  `SnapshotStateList<NavRoute>` that represents the entire back stack
- Routes are serializable objects implementing `NavRoute`, compile-time safe,
  making arguments part of the type system
- Features contribute `EntryProviderInstaller` to map routes to composables;
  `NavDisplay` stitches everything together at runtime
- Example routes: `HomeRoute`, `DetailRoute(itemId: String)`, `SettingsRoute`
- Common operations:
  - `navigator.goTo(DetailRoute(itemId))` pushes a detail screen
  - `navigator.goBack()` pops, returning `false` at root (allow the host to
    finish the activity)
  - `navigator.clearAndGoTo(HomeRoute)` replaces the stack (useful for
    logout/reset flows)
- Write helper extensions (e.g., `Navigator.popToHome`) when custom stack
  manipulation is needed
- See `common/navigation/NAVIGATION.md` for detailed patterns and examples

## Dependency Injection (Metro)

- Scopes live next to their modules (`AppScope`, `LandingScope`,
  `SettingsScope`) and are plain Metro `@Scope` annotations. Annotate long-lived
  services (e.g., `Navigator`, `NetworkApi`, loggers) with `@AppScope`.
- `AppGraph` is the root `@DependencyGraph(AppScope::class)` and extends every
  feature graph factory. It provides qualified values with
  `@Provides @Named("…")` functions and exposes Metro-generated
  `Lazy<Set<EntryProviderInstaller>>` to bootstrap Navigation 3.
- Feature modules use `@GraphExtension(<FeatureScope>::class)` +
  `@ContributesTo(AppScope::class)` factories to contribute their bindings. Each
  extension can define scoped `@Provides` functions (e.g., coroutine scopes,
  screen factories) and add Nav3 installers via `@Provides @IntoSet` functions.
- Constructor injection remains the default. Swap any legacy `Lazy<T>` injection
  sites with Metro `Provider<T>` when a dependency must be resolved lazily.
- Graph instances are created through
  `createGraphFactory<AppGraph.Factory>().create(app)`; `AppImpl` caches that
  instance and everything else reads from it.

## UI Composition

- Apply Jetpack Compose exclusively; no XML fragments
- Keep screens thin by pulling business logic into ViewModels and domain layers,
  leaving composables to render state and delegate events
- Example: `ItemListScreen` observes `ItemListUiState`, renders list items, and
  delegates tap events to
  `onItemClicked = { viewModel.input(ILEvent.ItemClicked(it)) }`
- Typography and spacing follow consistent design principles: use `AppSpacing`
  constants (dp4, dp8, dp16, etc.) for all padding/margins, maintain clear
  visual hierarchy, optimize for readability
- Wrap navigation and DI plumbing inside feature modules so the `:app` module
  only orchestrates high-level wiring

## Testing

- JUnit 5, `kotlinx.coroutines.test`, MockK, AssertJ, Turbine
- Test USF pipelines by driving events, capturing state/effects, controlling
  virtual time
- All public logic must have unit test coverage
- Run `make tests` or `make build` for full verification
- See `TESTING.md` for patterns and utilities

# Instructions

## General

- Optimize for legibility: clear `if/else`, single-responsibility functions (<20
  lines), immutable data
- PascalCase types, camelCase members, no star imports, trailing commas in
  multiline literals
- Avoid Kotlin `object` except for true constants; rely on scoped DI classes
- UI is Compose-only; keep composables thin, delegate logic to ViewModels and
  domain layers
- **Spacing**: ALWAYS use `AppSpacing` constants (dp4, dp8, dp16, etc.) - never
  hardcode `.dp` values

## Performance & Best Practices

- **Performance**: never block the UI; prefer incremental loading and optimistic
  updates
- **Error Handling**: explicit error states in UI, never silently
  catch-and-ignore exceptions
- **State Management**: immutable state updates, clear event/state/effect
  boundaries
- **Resource Management**: proper cleanup in lifecycle hooks, cancel coroutines
  when no longer needed

## Logging, Concurrency, Naming

- Logging: never include sensitive data; use clear prefixes for module
  identification (`"[MOD] message"`)
- Concurrency: no `GlobalScope` or raw threads; inject scopes, use suspend
  functions, clean up via USF lifecycle
- Naming: skip generic suffixes (Manager/Helper/Util); choose descriptive,
  domain-specific terms (e.g., `DataValidator`, `UserRepository`)

## Comments & Instruction Priority

- Comments document _why_ decisions were made (space-shuttle style); no TODOs
  without owner/context
- Instruction priority: **user requests → project rules → professional standards
  → this document**
- Default to precision over politeness; surface flawed assumptions immediately

# ExecPlans

When writing complex features or significant refactors, use an ExecPlan (as
described in @.ai/plans/PLANS.md or @~/.ai/plans/PLANS.md) from design to
implementation. Write new plans to the @.ai/plans directory. Place any temporary
research, clones etc., in the .gitignored subdirectory @.ai/plans/tmp
