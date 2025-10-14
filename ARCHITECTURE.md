# Architecture

## Module Layout
- `:app` — Entry point; wires dependency graph, sets up root composables and navigation display.
- `:features:*` — Independent feature modules that expose navigation routes, DI components, and USF-driven ViewModels for their screens.
- `:domain:*` — Shared business logic and domain models; no Android UI dependencies.
- `:common:*` — Cross-cutting utilities (logging, networking, USF core, lint rules) that features and domains depend on.
- `:build-logic` — Gradle convention plugins that enforce consistent configuration across modules.

## Unidirectional State Flow (USF)
- Every ViewModel subclasses `UsfViewModel<Event, UiState, Effect>` and owns a single immutable `UiState` plus an effects stream for one-off actions.
- Events flow through `process` inside a `ResultScope`, where state transitions use `updateState { … }` and one-time work uses `emitEffect`.
- Provide a dedicated `CoroutineScope` per ViewModel (Scoped via DI) so the pipeline lifecycle can pause when the UI detaches and resume when collectors return.
- Model callbacks as part of the state (`UiState(onAction = { input(Event.Action) })`) to keep composables dumb.
- Naming convention: abbreviate long feature names (e.g., `SAEvent`, `SAUiState`, `SAEffect` for `SettingsAScreen`).
- For the full USF deep dive (architecture, plugins, quickstart, tests, troubleshooting), see `ARCHITECTURE-USF.md`.

### Plugins for Complex Screens
- Compose ViewModels from reusable `UsfPluginInterface` implementations when multiple concerns (search, pagination, filtering) must coexist.
- Register plugins via `register(plugin = …, mapEvent = …, applyState = …, mapEffect = …, transformEffect = …)` to translate between ViewModel and plugin types.
- Plugins can hold internal state and react to subscription lifecycle (`onSubscribed`, `onUnsubscribed`) for resource management.
- Prefer plugins when logic is reusable or can be tested independently; skip them for simple, single-responsibility screens.

## Navigation 3 Back Stack
- Navigation is developer-owned: a singleton `Navigator` (DI scoped to `AppScope`) keeps a `SnapshotStateList<NavRoute>` that represents the entire back stack.
- Routes are serializable objects implementing `NavRoute`, giving compile-time safety and making arguments part of the type system.
- Each feature contributes an `EntryProviderInstaller` that maps its routes to composable screen content; `NavDisplay` stitches everything together at runtime.
- Common operations:
  - `navigator.goTo(route)` pushes a new destination.
  - `navigator.goBack()` pops, returning `false` at root (allow the host to finish the activity).
  - `navigator.clearAndGoTo(route)` replaces the stack (useful for tabs/auth resets).
- Write helper extensions (e.g., `Navigator.popTo`) when custom stack manipulation is needed.

## Dependency Injection (kotlin-inject + Anvil)
- Annotate injectable classes with `@Inject` constructors and mark lifetime with `@SingleIn(scope)`.
- Use `@ContributesBinding(scope)` to bind implementations to interfaces without manual modules.
- Define feature-scoped subcomponents via `@ContributesSubcomponent(FeatureScope::class)`; expose lazy screen providers to defer composition work.
- The app component (`@MergeComponent(AppScope::class)`) aggregates bindings and exposes factories for feature components alongside shared services such as the `Navigator`.
- Prefer constructor parameters over optional setters; inject `Lazy<T>` when work should defer until first use.

## UI Composition
- Apply Jetpack Compose exclusively; no XML fragments.
- Keep screens thin by pulling business logic into ViewModels and domain layers, leaving composables to render state and delegate events.
- Wrap navigation and DI plumbing inside feature modules so the `:app` module only orchestrates high-level wiring.
