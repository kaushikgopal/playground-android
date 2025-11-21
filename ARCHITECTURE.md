# Architecture

> **For project overview and core principles**, see @AGENTS.md
> **For USF core patterns**, see @USF.md

This document provides detailed architectural patterns for module structure, navigation, dependency injection, and UI composition.

## Module Layout
- `:app` — Entry point; wires dependency graph, sets up root composables and navigation display.
- `:features:*` — Independent feature modules that expose navigation routes, DI components, and USF-driven ViewModels for their screens.
- `:domain:*` — Shared business logic and domain models; no Android UI dependencies.
- `:common:*` — Cross-cutting utilities (logging, networking, USF core, UI components, lint rules) that features and domains depend on.
- `:build-logic` — Gradle convention plugins that enforce consistent configuration across modules.

## Unidirectional State Flow (USF)
- Every ViewModel subclasses `UsfViewModel<Event, UiState, Effect>` and owns a single immutable `UiState` plus an effects stream for one-off actions.
- Events flow through `process` inside a `ResultScope`, where state transitions use `updateState { … }` and one-time work uses `emitEffect`.
- Provide a dedicated `CoroutineScope` per ViewModel (Scoped via DI) so the pipeline lifecycle can pause when the UI detaches and resume when collectors return.
- Model callbacks as part of the state using `inputEventCallback` to ensure reference stability for Compose (`UiState(onAction = inputEventCallback(Event.Action))`).
- Naming convention: abbreviate long feature names (e.g., `ULEvent`, `ULUiState`, `ULEffect` for `UserListScreen`).
- Example: `DataViewModel` processes events like `LoadData`, `RefreshClicked`, emits effects like `ShowToast`, and updates state like `UiState(items = …, isLoading = …)`.

**USF Resources:**
- @USF.md — Core patterns for daily development (basic ViewModels, UI integration, testing)
- @USF-QUICKSTART.md — Complete tutorial from scratch
- @USF-PLUGINS.md — Advanced plugin composition for complex features
- @USF-TESTING.md — Comprehensive testing patterns
- @USF-TROUBLESHOOTING.md — Debugging guide

### Plugins for Complex Screens
- Compose ViewModels from reusable `UsfPluginInterface` implementations when multiple concerns (search, pagination, filtering) must coexist.
- Register plugins via `register(plugin = …, mapEvent = …, applyState = …, mapEffect = …, transformEffect = …)` to translate between ViewModel and plugin types.
- Plugins can hold internal state and react to subscription lifecycle (`onSubscribed`, `onUnsubscribed`) for resource management.
- Example use cases: search plugin with debouncing, pagination plugin for lists, form validation plugin, sync plugin for background operations.
- Prefer plugins when logic is reusable or can be tested independently; skip them for simple, single-responsibility screens.
- See @USF-PLUGINS.md for detailed plugin implementation patterns.

## Navigation 3 Back Stack
- Navigation is developer-owned: a singleton `Navigator` (DI scoped to `AppScope`) keeps a `SnapshotStateList<NavRoute>` that represents the entire back stack.
- Routes are serializable objects implementing `NavRoute`, giving compile-time safety and making arguments part of the type system.
- Each feature contributes an `EntryProviderInstaller` that maps its routes to composable screen content; `NavDisplay` stitches everything together at runtime.
- Example routes: `HomeRoute`, `DetailRoute(itemId: String)`, `SettingsRoute`.
- Common operations:
  - `navigator.goTo(DetailRoute(itemId))` pushes a detail screen.
  - `navigator.goBack()` pops, returning `false` at root (allow the host to finish the activity).
  - `navigator.clearAndGoTo(HomeRoute)` replaces the stack (useful for logout/reset flows).
- Write helper extensions (e.g., `Navigator.popToHome`) when custom stack manipulation is needed.

## Dependency Injection (Metro)
- Scopes live next to their modules (`AppScope`, `LandingScope`, `SettingsScope`) and are plain Metro `@Scope` annotations. Annotate long-lived services (e.g., `Navigator`, `NetworkApi`, loggers) with `@AppScope`.
- `AppGraph` is the root `@DependencyGraph(AppScope::class)` and extends every feature graph factory. It provides qualified values with `@Provides @Named("…")` functions and exposes Metro-generated `Lazy<Set<EntryProviderInstaller>>` to bootstrap Navigation 3.
- Feature modules use `@GraphExtension(<FeatureScope>::class)` + `@ContributesTo(AppScope::class)` factories to contribute their bindings. Each extension can define scoped `@Provides` functions (e.g., coroutine scopes, screen factories) and add Nav3 installers via `@Provides @IntoSet` functions.
- Constructor injection remains the default. Swap any legacy `Lazy<T>` injection sites with Metro `Provider<T>` when a dependency must be resolved lazily.
- Graph instances are created through `createGraphFactory<AppGraph.Factory>().create(app)`; `AppImpl` caches that instance and everything else reads from it.

## UI Composition
- Apply Jetpack Compose exclusively; no XML fragments.
- Keep screens thin by pulling business logic into ViewModels and domain layers, leaving composables to render state and delegate events.
- Example: `ItemListScreen` observes `ItemListUiState`, renders list items, and delegates tap events to `onItemClicked = { viewModel.input(ILEvent.ItemClicked(it)) }`.
- Typography and spacing follow consistent design principles: use `AppSpacing` constants (dp4, dp8, dp16, etc.) for all padding/margins, maintain clear visual hierarchy, optimize for readability (see `AGENTS.md` for patterns).
- Wrap navigation and DI plumbing inside feature modules so the `:app` module only orchestrates high-level wiring.
