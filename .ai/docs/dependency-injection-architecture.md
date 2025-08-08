# Dependency Injection Architecture

This document explains how the Playground Android project uses **kotlin-inject** with **Anvil** for dependency injection across the application.

## Overview

The app uses **kotlin-inject** as its dependency injection framework, enhanced with **Anvil** for multi-module support. This combination provides:

- Compile-time safety with no runtime reflection
- Automatic component merging across modules
- Support for scoped dependencies
- Multi-bindings for extensible features

## Core Setup

### Build Configuration

The DI framework is configured in `gradle/libs.versions.toml`:

```kotlin
// Dependencies
kotlin-inject = "0.7.2"
kotlin-inject-anvil = "0.1.0"

// Bundles for easy inclusion
kotlin-inject = [
    "kotlin-inject-runtime",
    "kotlin-inject-anvil-runtime",
    "kotlin-inject-anvil-runtime-utils",
]
kotlin-inject-compiler = [
    "kotlin-inject-compiler",
    "kotlin-inject-anvil-compiler",
]
```

Modules include these dependencies via:
```kotlin
// In build.gradle.kts
ksp(libs.bundles.kotlin.inject.compiler)
implementation(libs.bundles.kotlin.inject)
```

## Component Architecture

### Application Component

The root component (`AppComponent`) serves as the main dependency graph entry point:

```kotlin
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
) : SettingsComponent.Factory {
    
    // Singleton provisions
    @Provides fun provideAppName(): @Named("appName") String = "My Playground!"
    
    // Multi-bindings for extensibility
    abstract val loggers: Set<LogcatLogger>
    abstract val entryProviderInstallers: Set<EntryProviderInstaller>
    
    // Core services
    abstract val navigator: Navigator
}
```

Key features:
- `@MergeComponent(AppScope::class)` - Automatically merges all `@ContributesBinding` from other modules
- `@SingleIn(AppScope::class)` - Ensures single instance in app scope
- Implements factory interfaces for subcomponents

### Feature Subcomponents

Features use subcomponents for isolation and lifecycle management:

```kotlin
@ContributesSubcomponent(SettingsScope::class)
@SingleIn(SettingsScope::class)
interface SettingsComponent {
    
    // Feature-specific dependencies
    val settingsAScreen: SettingsAScreen
    val settingsBScreen: SettingsBScreen
    
    @ContributesSubcomponent.Factory(AppScope::class)
    interface Factory {
        fun createSettingsComponent(): SettingsComponent
    }
}
```

## Key DI Patterns

### 1. Multi-Bindings for Navigation

The navigation system uses multi-bindings to collect route providers from all features:

```kotlin
// In LandingComponent
@Provides
@IntoSet
fun provideSettingsEntryProvider(
    navigator: Navigator,
    landingComponentFactory: Factory,
): EntryProviderInstaller = {
    entry<LandingScreenRoute> {
        LandingScreen {
            navigator.goTo(SettingsRoutes.ScreenARoute)
        }
    }
}
```

```kotlin
// Collected in AppComponent
abstract val entryProviderInstallers: Set<EntryProviderInstaller>
```

This pattern allows each feature to contribute its navigation entries independently, which are automatically collected into a set.

### 2. Function Injection for Composables

SettingsScreenB demonstrates function injection, allowing Compose functions to be injected:

```kotlin
// Type alias for the composable function
typealias SettingsBScreen = @Composable () -> Unit

// Injectable composable function
@Inject
@SingleIn(SettingsScope::class)
@Composable
fun SettingsBScreen(bindings: SettingsBindings) {
    // Composable implementation
}
```

```kotlin
// Used in SettingsComponent
interface SettingsComponent {
    val settingsBScreen: SettingsBScreen  // Injected as a function
}

// Navigation setup
entry<ScreenBRoute> { 
    settingsComponent.settingsBScreen()  // Called as a function
}
```

This allows Compose functions to be part of the DI graph while maintaining their composable nature.

### 3. Cross-Module Bindings

Modules contribute implementations to the app scope using `@ContributesBinding`:

```kotlin
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class QuotesRepoImpl(
    private val api: NetworkApi,
) : QuotesRepo {
    // Implementation
}
```

This automatically binds `QuotesRepoImpl` to `QuotesRepo` interface in the app component without explicit wiring.

## Component Interaction

### Scope Hierarchy

```
AppScope (Application lifetime)
    ├── SettingsScope (Feature lifetime)
    └── LandingScope (Feature lifetime)
```

### Component Creation Flow

1. **App Launch**: `AppComponent.from(context)` creates the root component
2. **Feature Navigation**: Lazy subcomponent creation on first access
   ```kotlin
   val settingsComponent by lazy { 
       factory.createSettingsComponent() 
   }
   ```
3. **Screen Rendering**: Components provide screens/dependencies

### Dependency Flow Example

```kotlin
// 1. AppComponent provides global dependencies
@Provides fun provideAppName(): @Named("appName") String

// 2. Feature component accesses app dependencies
@Inject
class SettingsBindings(
    @Named("appName") val appName: String,  // From AppComponent
    val quotesRepo: QuotesRepo,             // From domain module
)

// 3. Screen uses feature bindings
@Composable
fun SettingsBScreen(bindings: SettingsBindings) {
    val quote = bindings.quotesRepo.quoteForTheDay()
}
```

## Benefits of This Architecture

1. **Compile-Time Safety**: All DI errors caught at build time
2. **Module Independence**: Features can be developed in isolation
3. **Automatic Wiring**: Anvil merges components without boilerplate
4. **Testability**: Easy to provide test implementations
5. **Performance**: No runtime reflection or proxy generation
6. **Type Safety**: Full IDE support and refactoring

## Common Patterns

### Named Dependencies
```kotlin
@Provides fun provideDebugFlag(): @Named("debuggableApp") Boolean
```

### Singleton Scope
```kotlin
@SingleIn(AppScope::class)
class NetworkService
```

### Factory Pattern
```kotlin
interface Factory {
    fun create(param: String): Component
}
```

### Multi-Binding Collections
```kotlin
abstract val installers: Set<EntryProviderInstaller>
```

This architecture provides a robust, type-safe dependency injection solution that scales well with multi-module Android applications.