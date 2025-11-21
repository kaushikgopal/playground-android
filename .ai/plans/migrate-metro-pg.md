# Metro Option 3 ExecPlan — Playground Android

This ExecPlan is a living document. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` up to date while executing, per `~/.ai/commands/exec-plan.md`.

## Purpose / Big Picture

Playground Android currently wires every module through kotlin-inject + kotlin-inject-anvil (`0.7.2` / `0.1.0`) driven by the shared template convention plugins and a KSP toolchain on Kotlin `2.1.21`. Migrating the entire app to Metro (latest published tag `0.7.7`) gives us a single compiler-plugin-based DI stack that is K2-ready, removes KSP from all modules, and aligns the codebase with the architecture guidance already documented in `AGENTS.md`. Success looks like `make debug` (assembleDebug - lint) and `make tests` both completing with only Metro in the build graph, the app booting with `AppGraph` instead of `AppComponent`, landing/settings navigation working through Metro-provided entry installers, and no references to kotlin-inject/anvil artifacts in Gradle or source.

## Progress

- [x] (2025-11-21 00:06Z) Repository + documentation audit completed; ExecPlan authored with Option 3 (full Metro migration) scope.
- [x] (2025-11-21 00:58Z) Phase 1 – Version catalog/Kotlin upgrade validated on `make debug` with legacy DI still intact (Kotlin 2.2.20, KSP 2.2.20-2.0.4, Metro plugin alias declared, `make debug warnings=summary` successful).
- [x] (2025-11-21 01:03Z) Phase 2 – Build-logic + Gradle modules switched from KSP/kotlin-inject bundles to the Metro Gradle plugin and compiler classpath (Template plugins now apply Metro, all module `ksp(...)`/`libs.bundles.kotlin.inject*` deps removed, `make debug` fails early in `domain/shared:compileKotlin` due to missing `me.tatarka` annotations as expected).
- [x] (2025-11-21 01:25Z) Phase 3 – Scope + annotation migration (custom scopes/qualifiers rebuilt on Metro, all `me.tatarka` imports removed, Kotlin DI annotations swapped to Metro counterparts, `Lazy` injection sites converted to `Provider`).
- [x] (2025-11-21 01:25Z) Phase 4 – Graph rewrites (`AppComponent` replaced with `AppGraph`, Landing/Settings rewritten as Metro graph extensions with entry installers, Metro `createGraphFactory` wiring adopted, `make debug` succeeds end-to-end).
- [x] (2025-11-21 01:31Z) Phase 5 – Validation, documentation, and cleanup (docs now reference Metro, README/AGENTS/ARCHITECTURE updated, `make tests warnings=summary` passes, `./gradlew :app:installDebug` fails only because no device/emulator is attached).

## Surprises & Discoveries

- The shared `TemplateFeatureConventionPlugin` still applies the KSP plugin and wires both `libs.bundles.kotlin.inject` bundles plus the `template.android` behaviors, so every Android module receives kotlin-inject runtime/compiler artifacts implicitly (build-logic/convention/src/main/kotlin/TemplateFeatureConventionPlugin.kt:25-61). Replacing KSP/DI tooling must happen inside these conventions first to avoid editing a dozen module build files twice.
- JVM-only modules such as `domain/shared` and `domain/quoter/api` also apply KSP via direct `alias(libs.plugins.ksp)` declarations (domain/shared/build.gradle.kts:3-10, domain/quoter/api/build.gradle.kts:2-13). Once the catalog entries are removed, these modules will not sync until Metro (or no DI plugin) is applied explicitly, so the order of operations matters.
- `LandingComponent` exposes a `LandingScreen` typealias via manual `@Provides` functions because kotlin-inject could not apply `@Inject` to top-level typealias factories (features/landing/src/main/java/sh/kau/playground/landing/di/LandingComponent.kt:27-35). The migration has to keep those providers (now Metro `@Provides`) otherwise Nav3 entry installers will not resolve the Compose lambda.
- Several classes rely on kotlin-inject’s ability to inject `Lazy<T>` directly (`QuotesRepoImpl` in domain/quoter/impl and `SettingsBViewModelImpl` in features/settings/impl). Metro prefers `dev.zacsweers.metro.Provider<T>` for deferred resolution; we must swap to Provider or explicit factory lambdas, then audit coroutine usage (`SettingsBViewModelImpl` launches IO work off the injected coroutineScope) to keep behavior intact.
- `AppComponent` only declares `SettingsComponent.Factory` explicitly even though `LandingComponent.Factory` is contributed into the graph via `@ContributesSubcomponent.Factory(AppScope::class)` (AppComponent.kt:19-38, LandingComponent.kt:37-48). When rewriting to Metro we need `AppGraph : LandingGraph.Factory, SettingsGraph.Factory` so that entry installers retain compile-time coverage.
- Upgrading Kotlin to 2.2.20 introduces new kotlin-inject/KSP warnings about generated components accessing `Usf`/`UsfViewModel` supertypes (e.g., app/build/generated/ksp/debug/kotlin/sh/kau/playground/features/settings/di/InjectSettingsComponentFinalKotlinInjectAppComponent.kt:43,56). The warnings do not fail the build but confirm this bump is the last one we can take before replacing the DI stack.
- Removing kotlin-inject dependencies immediately surfaces missing annotation errors in `domain/shared/src/main/java/sh/kau/playground/shared/di/Named.kt` (`Unresolved reference 'me'`, `Unresolved reference 'Qualifier'`) during `:domain:shared:compileKotlin`, confirming Phase 3 must rehome qualifiers before builds can pass.
- Metro's runtime is compiled for JVM 11+, so every Android/Kotlin module had to move from `JvmTarget.JVM_1_8` to JVM 11 and align Java compile options/toolchains, otherwise the compiler refused to inline `createGraphFactory` calls.
- Metro scopes conflict with explicit scope annotations on graph-level bindings; keeping `@LandingScope`/`@SettingsScope` on ViewModels and screens caused `IncompatiblyScopedBindings` errors until the scope annotations were removed from the classes (the graph extension scopes those bindings automatically).
- Qualifier bindings only resolve when the qualifier annotates the provider declaration rather than the return type; switching to `@Named` on the `@Provides` functions (and annotating property getters) unblocked the missing-binding diagnostics emitted by Metro.

## Decision Log

- Decision: Follow Metro Option 3 (full replacement, no dual-stack interop) for Playground just as in the Codex migration. Rationale: keeps build logic simple, ensures every module is on the same DI story, and avoids maintaining adapters between kotlin-inject subcomponents and Metro graphs. Date/Author: 2025-11-21 / Codex.
- Decision: Upgrade Kotlin to `2.2.20` (or the latest K2 minor verified by Metro 0.7.7) before touching source so that the compiler plugin can load cleanly, then remove `ksp` + kotlin-inject catalog entries only after all Gradle scripts stop referencing them. Date/Author: 2025-11-21 / Codex.
- Decision: Preserve the `EntryProviderInstaller` multibinding pattern that feeds Nav3 by using Metro’s `@IntoSet` (on provider functions inside graph extensions) and exposing `Lazy<Set<EntryProviderInstaller>>` from `AppGraph`. Date/Author: 2025-11-21 / Codex.
- Decision: Introduce `@AppScope` under `domain/shared/src/main/java/sh/kau/playground/shared/di` and convert all previous `@SingleIn(AppScope::class)` usages to scope annotations (feature scopes already live next to their modules). Date/Author: 2025-11-21 / Codex.
- Decision: Replace kotlin-inject `Lazy<T>` injection points with `dev.zacsweers.metro.Provider<T>` (or Metro `Lazy<T>` if added later) so that `QuotesRepoImpl` and `SettingsBViewModelImpl` retain deferred initialization without relying on Kotlin stdlib delegated lazy injection. Date/Author: 2025-11-21 / Codex.
- Decision: Standardize every module on JVM target 11 (Android + Kotlin/JVM) to satisfy Metro’s compiler requirements and avoid the `Cannot inline bytecode built with JVM target 11 into bytecode built with JVM target 1.8` failure. Date/Author: 2025-11-21 / Codex.

## Outcomes & Retrospective

Pending until Metro fully drives the app graphs and the validation suite passes.

## Context and Orientation

- Modules: `:app`, `:features:landing`, `:features:settings:{api,impl}`, domain modules (`:domain:ui`, `:domain:quoter:{api,impl}`, `:domain:shared`), and shared utilities (`:common:{log, navigation, networking, usf, usf:log, lint-rules}`). All Android modules go through `template.android` and most feature modules additionally use `template.feature`.
- Tooling: Kotlin `2.1.21`, AGP `8.12.3`, KSP `2.1.21-2.0.2`. `gradle/libs.versions.toml` declares bundles for kotlin-inject runtime/compiler artifacts plus the KSP plugin alias.
- DI entry points: `app/src/main/java/sh/kau/playground/app/di/AppComponent.kt` is the root `@MergeComponent`. `LandingComponent` and `SettingsComponent` live under their respective modules and contribute `EntryProviderInstaller` lambdas for Nav3. `Navigator` and the logging composites are scoped singletons through `@SingleIn(AppScope::class)`.
- Runtime usage: `AppImpl` caches `AppComponent` and calls `CompositeLogger.install(appComponent.loggers.value)`. `MainActivity` pulls `navigator` and `entryProviderInstallers` from the same component before constructing `NavDisplay`.
- Commands: `make debug` (default target) assembles the app without lint, `make tests` executes `./gradlew -x lint testDebugUnitTest`, `kill-ksp` exists specifically for kotlin-inject daemon cleanup and should be retired after Metro.

## Before vs After

### Before – kotlin-inject + anvil

- Toolchain mixes Kotlin 2.1.21 + KSP. Every Gradle module either applies `alias(libs.plugins.ksp)` or inherits it from the template plugin, and dependencies rely on `libs.bundles.kotlin.inject` bundles.
- Graph structure centers on `AppComponent` (`@MergeComponent(AppScope::class)`) exposing fields like `navigator`, `loggers`, and `entryProviderInstallers`. Feature boundaries rely on `@ContributesSubcomponent` with nested factories contributed to `AppScope`.
- Scope management and bindings come from `software.amazon.lastmile.kotlin.inject.anvil` annotations (`@SingleIn`, `@ContributesBinding`, `@ContributesSubcomponent.Factory`). Qualifiers and providers come from `me.tatarka.inject.annotations`.
- Graph creation uses `AppComponent::class.create(app)` from kotlin-inject’s generated extensions and caches the instance manually.

### After – Metro-only DI

- Version catalog declares `dev.zacsweers.metro` plugin alias (0.7.7) and drops `ksp`, `kotlin-inject`, and `kotlin-inject-anvil` bundles. Kotlin ≥ 2.2.20 is set globally.
- `AppGraph` is an `@DependencyGraph(AppScope::class)` interface that extends each feature graph factory and exposes shared singletons (`navigator`, `entryProviderInstallers`, `loggers`). Graph instances are created through `createGraphFactory<AppGraph.Factory>().create(app)`.
- Feature boundaries become `@GraphExtension(Scope::class)` interfaces in their modules. Each contributes its own factory plus `@IntoSet` entry installers via Metro providers.
- Scope annotations are local (`@AppScope`, `@LandingScope`, `@SettingsScope`). Bindings use Metro’s `@ContributesBinding`, `@ContributesIntoSet`, and `@ContributesTo`. Deferred injections use `Provider<T>`.
- Build completes without any KSP tasks; only the Metro compiler plugin runs.

### Representative Before/After Examples

`app/src/main/java/sh/kau/playground/app/di/AppComponent.kt`:

```kotlin
// Before
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(@get:Provides val app: App) : SettingsComponent.Factory {
  @Provides fun provideAppName(): @Named("appName") String = "My Playground!"
  @Provides fun provideDebuggableApp(): @Named("debuggableApp") Boolean = app.isDebuggable
  abstract val loggers: Lazy<Set<LogcatLogger>>
  abstract val navigator: Navigator
  abstract val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>
  companion object { fun from(context: Context): AppComponent = AppComponent::class.create(context.applicationContext as App) }
}
```

```kotlin
// After
@AppScope
@DependencyGraph(AppScope::class)
interface AppGraph : LandingGraph.Factory, SettingsGraph.Factory {
  val navigator: Navigator
  val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>
  val loggers: Lazy<Set<LogcatLogger>>

  @Provides fun appName(): @Named("appName") String = "My Playground!"
  @Provides fun debuggableApp(app: App): @Named("debuggableApp") Boolean = app.isDebuggable

  @DependencyGraph.Factory
  fun interface Factory { fun create(@Provides app: App): AppGraph }

  companion object {
    fun from(context: Context): AppGraph =
        createGraphFactory<Factory>().create(context.applicationContext as App)
  }
}
```

`features/landing/src/main/java/sh/kau/playground/landing/di/LandingComponent.kt`:

```kotlin
// Before
@ContributesSubcomponent(LandingScope::class)
@SingleIn(LandingScope::class)
interface LandingComponent {
  @Provides @SingleIn(LandingScope::class)
  fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  @Provides fun provideLandingScreen(viewModel: LandingViewModel, navigator: Navigator): LandingScreen =
      sh.kau.playground.landing.ui.LandingScreen(viewModel, navigator)
  val landingScreen: LandingScreen

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createLandingComponent(): LandingComponent
    @Provides @IntoSet
    fun provideLandingEntryProvider(factory: Factory): EntryProviderInstaller = {
      val component by lazy { factory.createLandingComponent() }
      entry<LandingScreenRoute> { component.landingScreen() }
    }
  }
}
```

```kotlin
// After
@GraphExtension(LandingScope::class)
interface LandingGraph {
  val landingScreen: LandingScreen

  @Provides
  @LandingScope
  fun coroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  @Provides
  fun landingScreen(viewModel: LandingViewModel, navigator: Navigator): LandingScreen =
      sh.kau.playground.landing.ui.LandingScreen(viewModel, navigator)

  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  interface Factory {
    fun createLandingGraph(): LandingGraph

    @Provides
    @IntoSet
    fun landingEntryProvider(factory: Factory): EntryProviderInstaller = {
      val graph by lazy { factory.createLandingGraph() }
      entry<LandingScreenRoute> { graph.landingScreen() }
    }
  }
}
```

`common/log/src/main/java/sh/kau/playground/log/AndroidLogger.kt`:

```kotlin
// Before
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
class AndroidLogger(
    @Named("debuggableApp") val debuggableApp: Boolean,
    androidLogger: AndroidLogcatLogger = AndroidLogcatLogger(LogPriority.VERBOSE),
) : LogcatLogger by androidLogger
```

```kotlin
// After
@AppScope
@ContributesIntoSet(AppScope::class)
class AndroidLogger @Inject constructor(
    @Named("debuggableApp") private val debuggableApp: Boolean,
    androidLogger: AndroidLogcatLogger = AndroidLogcatLogger(LogPriority.VERBOSE),
) : LogcatLogger by androidLogger {
  override fun isLoggable(priority: LogPriority): Boolean = super.isLoggable(priority) && debuggableApp
}
```

## Plan of Work

### Milestone 1 – Catalog + Kotlin upgrade
1. Capture current DI footprint (`rg -n "ksp\(" --glob "*.gradle.kts"`, `rg -n "software\\.amazon" -g"*.kt"`).
2. Update `gradle/libs.versions.toml`:
   - Bump `kotlin` to `2.2.20` (or the version Metro 0.7.7 requires) and align `agp` if necessary.
   - Remove `ksp`, `kotlin-inject`, and `kotlin-inject-anvil` entries from `[versions]`, `[libraries]`, `[bundles]`, and `[plugins]` once Gradle files stop referencing them.
   - Add `metro = "0.7.7"` under `[versions]` and `plugins.metro = { id = "dev.zacsweers.metro", version.ref = "metro" }`.
3. Declare `alias(libs.plugins.metro) apply false` in the root `build.gradle.kts` so convention plugins can consume it.
4. Run `./gradlew --version` followed by `make debug warnings=summary` to confirm the Kotlin bump is accepted while legacy DI artifacts still exist.

### Milestone 2 – Build-logic + Gradle script migration
1. Edit `TemplateAndroidConventionPlugin` and `TemplateFeatureConventionPlugin`:
   - Remove `plugins.apply(libs.plugins.ksp...)` calls.
   - Apply the Metro plugin (`plugins.apply(libs.plugins.metro.get().pluginId)`) anywhere Kotlin code is compiled.
   - Drop the `ksp(...)` and `implementation(libs.bundles.kotlin.inject)` blocks.
   - Keep Compose + serialization plugins as-is.
2. For JVM or specialized modules (`domain/shared`, `domain/quoter/api`, `common/usf`, etc.) that declare `alias(libs.plugins.ksp)` manually, remove the alias and apply Metro (if DI is needed) or nothing (if the module no longer needs compile-time DI).
3. Clean up module dependencies so that no `libs.bundles.kotlin.inject*` entries remain. Replace runtime DI deps with Metro’s runtime (which is provided by the compiler plugin; no explicit dependency needed unless Metro runtime artifacts are added later).
4. Run `rg -n "ksp\\(" --glob "*.gradle.kts"` and `rg -n "kotlin.inject" --glob "*.gradle.kts"` until both return zero matches.
5. Execute `make debug` expecting failures limited to missing `me.tatarka`/`software.amazon` symbols, which signals Gradle wiring is complete.

### Milestone 3 – Scope + annotation migration
1. Add `domain/shared/src/main/java/sh/kau/playground/shared/di/AppScope.kt` with Metro’s `@Scope` annotation + targets (matching existing scope annotations).
2. Replace all `@SingleIn(AppScope::class)` with `@AppScope`, `@SingleIn(LandingScope::class)` with `@LandingScope`, etc.
3. Swap imports from `me.tatarka.inject.annotations.*` and `software.amazon.lastmile...` to the Metro equivalents (`dev.zacsweers.metro.Inject`, `@ContributesBinding`, `@Provides`, `@Qualifier`, `@IntoSet`, etc.). Update `domain/shared/di/Named.kt` to use Metro’s `@Qualifier`.
4. Convert Kotlin-inject `Lazy<T>` injection points to Metro providers:
   - Constructor parameters become `Provider<QuotesRepo>` or `Provider<NetworkApi>` as needed.
   - Replace `.value` usages with `.get()`.
5. Compile each module individually (`./gradlew :common:log:compileDebugKotlin`, etc.) to catch annotation gaps early.

### Milestone 4 – Root graph rewrite
1. Rename `AppComponent.kt` to `AppGraph.kt` and convert it into an `@DependencyGraph` interface. Expose `val navigator`, `val entryProviderInstallers`, `val loggers`, and any other singletons as abstract properties.
2. Replace `@get:Provides val app: App` with a nested `@DependencyGraph.Factory` that accepts `@Provides app: App`. Use `createGraphFactory<AppGraph.Factory>()` in the companion `from` helper.
3. Update `AppImpl` and `MainActivity` to depend on `AppGraph` instead of `AppComponent`. Ensure `CompositeLogger.install(appGraph.loggers.value)` remains correct.
4. Verify qualifiers (`@Named("startDestination")`, `@Named("debuggableApp")`, `@Named("appName")`) are still provided via `@Provides` methods on the graph.
5. Run `./gradlew :app:compileDebugKotlin` to confirm the new graph compiles before tackling features.

### Milestone 5 – Feature graph extensions
1. Convert `LandingComponent` to `LandingGraph`:
   - Apply `@GraphExtension(LandingScope::class)`.
   - Annotate provider functions with `@LandingScope` where lifetimes must persist for the feature.
   - Keep the `LandingScreen` provider that wraps the typealias, now returning from Metro `@Provides`.
   - Annotate the factory with both `@GraphExtension.Factory` and `@ContributesTo(AppScope::class)` so `AppGraph` inherits it automatically.
2. Convert `SettingsComponent` similarly, ensuring both `SettingsAScreen` (class) and `SettingsBScreen` (function) injection sites still work. Keep the coroutine scope provider and entry installers for `ScreenARoute` / `ScreenBRoute`.
3. Update `AppGraph` to extend `LandingGraph.Factory` and `SettingsGraph.Factory`. Adjust entry installers to call the new factory methods.
4. Re-run feature compilations individually (`./gradlew :features:landing:compileDebugKotlin`, `./gradlew :features:settings:impl:compileDebugKotlin`).

### Milestone 6 – Binding/class updates
1. ViewModels (`LandingViewModelImpl`, `SettingsAViewModelImpl`, `SettingsBViewModelImpl`) and UI entry classes (`SettingsAScreen`, `SettingsBScreen`) should use Metro `@ContributesBinding` / scope annotations without specifying `boundType` (Metro infers the interface).
2. `Navigator`, `NetworkApi`, `QuotesRepoImpl`, `AndroidLogger`, `AndroidLogger2`, and `CompositeLogger` consumers all need `@AppScope` plus Metro binding annotations. Replace `Lazy<NetworkApi>`/`Lazy<QuotesRepo>` injection with `Provider`s.
3. Ensure `EntryProviderInstaller` multibindings remain `@IntoSet` functions inside the feature graph factories. Metro can import `dev.zacsweers.metro.IntoSet`.
4. Replace Kotlin-inject-specific comments (e.g., `// kotlin-inject multi-bindings (0)`) with Metro equivalents or remove them.
5. Update tests (if any) that reference DI types explicitly; e.g., if unit tests instantiate `Navigator` using old constructors, adjust to the new annotations as needed.

### Milestone 7 – Validation + cleanup
1. Run `make debug warnings=summary`, `make tests warnings=summary`, and `./gradlew :app:installDebug`.
2. Smoke the installed build: launch, verify Landing renders, navigate to Settings A & B, ensure `Navigator.goBack` works, and confirm quotes load (watch logcat for [USF] + [Ktor] output).
3. Remove or repurpose the `kill-ksp` Make target; update README/AGENTS/ARCHITECTURE docs to mention Metro-based DI and the new terminology (AppGraph, graph extensions).
4. Final hygiene scans: `rg -n "kotlin-inject"`, `rg -n "software\\.amazon"`, `rg -n "me\\.tatarka"` should all return zero matches.
5. Capture any deviations (e.g., additional Provider conversions) in the `Surprises & Discoveries` section.

## Concrete Steps

1. Baseline DI references:
   ```bash
   rg -n "ksp\\(" --glob "*.gradle.kts"
   rg -n "software\\.amazon" -g"*.kt"
   rg -n "me\\.tatarka" -g"*.kt"
   ```
2. Catalog + plugin prep:
   ```bash
   $EDITOR gradle/libs.versions.toml
   ./gradlew --version
   make debug warnings=summary | cat
   ```
3. Build-logic edits:
   ```bash
   $EDITOR build-logic/convention/src/main/kotlin/TemplateAndroidConventionPlugin.kt
   $EDITOR build-logic/convention/src/main/kotlin/TemplateFeatureConventionPlugin.kt
   rg -n "ksp\\(" --glob "*.gradle.kts"
   ```
4. Module cleanup and annotation swaps:
   ```bash
   ./gradlew :common:log:compileDebugKotlin
   ./gradlew :features:landing:compileDebugKotlin
   rg -n "software\\.amazon" -g"*.kt"
   ```
5. Final verification:
   ```bash
   make debug warnings=summary
   make tests warnings=summary
   ./gradlew :app:installDebug
   rg -n "kotlin-inject"
   ```

2025-11-21 00:58Z – Steps 1 and 2 were executed: `./gradlew --version` acknowledged the Gradle-side Kotlin tooling, `make debug warnings=summary` succeeded under Kotlin 2.2.20 + KSP 2.2.20-2.0.4 with legacy DI still in place. Warnings about `Usf`/`UsfViewModel` access surfaced (see Surprises & Discoveries) but do not block progress.
2025-11-21 01:03Z – Step 5 now fails during `:domain:shared:compileKotlin` after removing the kotlin-inject bundles; the log shows unresolved references to `me.tatarka.inject.annotations.Qualifier` in `domain/shared/src/main/java/sh/kau/playground/shared/di/Named.kt`, which is the expected breakpoint before Metro annotations replace the old qualifier.
2025-11-21 01:25Z – Final Phase 3/4 verification via repeated `make debug warnings=summary` runs: after replacing scopes/annotations, rewriting AppGraph/LandingGraph/SettingsGraph, updating providers to Metro `@Named`, and bumping JVM targets to 11 the debug build now completes cleanly. Earlier runs exposed the JVM target mismatch and Metro scope diagnostics captured under Surprises.
2025-11-21 01:31Z – `make tests warnings=summary` passes end-to-end; `./gradlew :app:installDebug` still fails because no emulator/device is connected, which matches expectations for this headless environment.
2025-11-21 01:25Z – Final Phase 3/4 verification via repeated `make debug warnings=summary` runs: after replacing scopes/annotations, rewriting AppGraph/LandingGraph/SettingsGraph, updating providers to Metro `@Named`, and bumping JVM targets to 11 the debug build now completes cleanly. Earlier runs exposed the JVM target mismatch and Metro scope diagnostics captured under Surprises.

## Validation and Acceptance

- `make debug warnings=summary` completes with Metro-generated sources only (no KSP tasks or kotlin-inject artifacts in Gradle output).
- `make tests warnings=summary` passes; unit tests referencing DI-managed classes still compile and behave.
- `./gradlew :app:installDebug` installs a build that navigates Landing → Settings A → Settings B and back without crashes, while logcat shows Metro-provided loggers.
- `rg -n "kotlin-inject"`, `rg -n "software\\.amazon"`, and `rg -n "me\\.tatarka"` return zero matches in the repo.
- Documentation (`README.md`, `AGENTS.md`, `ARCHITECTURE.md`) references Metro instead of kotlin-inject/anvil, and the `kill-ksp` target is either removed or marked obsolete.

## Idempotence and Recovery

Each milestone confines changes to a predictable set of files (catalog, build-logic, specific modules). If a step fails, revert only those files (e.g., `git checkout -- gradle/libs.versions.toml build-logic/...`) rather than resetting the repo. Metro graph generation failures are deterministic; rerun the targeted `./gradlew :module:compileDebugKotlin` after adjusting annotations. Keep commits milestone-sized to allow rolling back a single phase without losing earlier verified work.

## Artifacts and Notes

Baseline search results to compare after migration:

```bash
$ rg -n "ksp\\(" --glob "*.gradle.kts"
domain/shared/build.gradle.kts:9:    ksp(libs.bundles.kotlin.inject.compiler)
common/navigation/build.gradle.kts:12:  ksp(libs.bundles.kotlin.inject.compiler)
domain/quoter/api/build.gradle.kts:9:    ksp(libs.bundles.kotlin.inject.compiler)
common/networking/build.gradle.kts:13:  ksp(libs.bundles.kotlin.inject.compiler)
domain/quoter/impl/build.gradle.kts:13:    ksp(libs.bundles.kotlin.inject.compiler)
common/log/build.gradle.kts:12:    ksp(libs.bundles.kotlin.inject.compiler)
app/build.gradle.kts:45:  ksp(libs.bundles.kotlin.inject.compiler)
```

```bash
$ rg -n "software\\.amazon" -g"*.kt"
common/log/src/main/java/sh/kau/playground/log/AndroidLogger.kt:8:import software.amazon.lastmile.kotlin.inject.anvil.AppScope
common/networking/src/main/java/sh/kau/playground/networking/NetworkApi.kt:13:import software.amazon.lastmile.kotlin.inject.anvil.AppScope
... (total 23 hits across log, navigation, landing, settings, quoter, and app modules)
```

Metro release reference:

```bash
$ gh release list --repo ZacSweers/metro | head
0.7.7  Latest  0.7.7  2025-11-19T21:03:59Z
```

Document any new surprises encountered mid-migration directly in `Surprises & Discoveries` with file paths + evidence.

## Interfaces and Dependencies

Target end-state definitions (to guide implementation):

```kotlin
// domain/shared/src/main/java/sh/kau/playground/shared/di/AppScope.kt
@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope
```

```kotlin
// app/src/main/java/sh/kau/playground/app/di/AppGraph.kt
@AppScope
@DependencyGraph(AppScope::class)
interface AppGraph : LandingGraph.Factory, SettingsGraph.Factory {
  val navigator: Navigator
  val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>
  val loggers: Lazy<Set<LogcatLogger>>

  @Provides fun appName(): @Named("appName") String
  @Provides fun debuggableApp(app: App): @Named("debuggableApp") Boolean

  @DependencyGraph.Factory
  fun interface Factory { fun create(@Provides app: App): AppGraph }
}
```

```kotlin
// features/landing/.../LandingGraph.kt
@GraphExtension(LandingScope::class)
interface LandingGraph {
  val landingScreen: LandingScreen

  @Provides @LandingScope
  fun coroutineScope(): CoroutineScope

  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  interface Factory {
    fun createLandingGraph(): LandingGraph

    @Provides @IntoSet
    fun entryInstaller(factory: Factory): EntryProviderInstaller
  }
}
```

```kotlin
// common/log/.../AndroidLogger.kt
@AppScope
@ContributesIntoSet(AppScope::class)
class AndroidLogger @Inject constructor(
    @Named("debuggableApp") private val debuggableApp: Boolean,
) : LogcatLogger { /* ... */ }
```

```kotlin
// domain/quoter/impl/.../QuotesRepoImpl.kt
@AppScope
@ContributesBinding(AppScope::class)
class QuotesRepoImpl @Inject constructor(
    private val api: Provider<NetworkApi>,
) : QuotesRepo {
  private suspend fun fetchQuote(): Quote = api.get().client().get(...)
}
```

These definitions ensure Metro enforces scopes/graph wiring at compile time while keeping the navigation + logging behavior identical to today’s runtime.

Plan update 2025-11-21 00:58Z — Completed Phase 1 (Kotlin/KSP bump with Metro plugin alias) and documented the new KSP warnings surfaced by Kotlin 2.2.20.
Plan update 2025-11-21 01:03Z — Completed Phase 2 by applying Metro through the convention plugins, stripping all `ksp(...)`/kotlin-inject bundle usages from module Gradle files, removing the catalog aliases, and recording the expected `domain/shared:compileKotlin` failure that now blocks further progress until Metro annotations land.
Plan update 2025-11-21 01:25Z — Completed Phases 3 & 4 by migrating all scopes/annotations to Metro, rewriting `AppGraph`/feature graph extensions, converting lazy injections to `Provider`, and validating that `make debug warnings=summary` now succeeds under the Metro-only stack.
Plan update 2025-11-21 01:31Z — Completed Phase 5 with documentation refresh (README/AGENTS/ARCHITECTURE + Makefile cleanup) and validation commands (`make tests warnings=summary`, attempted `./gradlew :app:installDebug` with failure attributed to no attached device).
