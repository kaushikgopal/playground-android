# Dependency Injection Architecture

This document explains how the Playground Android project uses **kotlin-inject** with **Anvil** for dependency injection across the application.

## Overview

The app uses **kotlin-inject** as its dependency injection framework, enhanced with **Anvil** for multi-module support. This combination provides:

- Compile-time safety with no runtime reflection
- Automatic component merging across modules
- Support for scoped dependencies
- Multi-bindings for extensible features
- Native lazy injection for performance optimization

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
    
    // Lazy multi-bindings for performance
    abstract val loggers: Lazy<Set<LogcatLogger>>
    abstract val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>
    
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
    
    // Lazy-loaded feature screens
    val settingsAScreen: Lazy<SettingsAScreen>
    val settingsBScreen: Lazy<SettingsBScreen>
    
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

## Lazy Injection Guide

kotlin-inject provides native support for `Lazy<T>` injection to defer initialization of expensive dependencies until they're actually needed. This powerful feature requires careful consideration of when to use it.

### When to Use Lazy Injection

#### ✅ Good Candidates for Lazy

1. **Heavy I/O Dependencies**
   - Network clients (Retrofit, Ktor, OkHttp)
   - Database instances (Room, SQLDelight)
   - File system managers
   - Large resource loaders

2. **Feature-Specific Components**
   - Feature modules not needed at startup
   - Optional features based on user settings
   - Premium/paid features
   - A/B test variations

3. **Multi-Bindings and Collections**
   - Plugin systems
   - Navigation providers
   - Event handlers that may not all be used

4. **Third-Party SDKs**
   - Analytics (Firebase, Amplitude)
   - Crash reporting (Crashlytics, Sentry)
   - Ad networks
   - Payment processors

5. **UI Components**
   - Screens accessed through navigation
   - Dialogs and bottom sheets
   - Complex custom views
   - Image loaders and processors

### When NOT to Use Lazy Injection

#### ❌ Poor Candidates for Lazy

1. **Critical Startup Dependencies**
   ```kotlin
   // Navigator needed immediately for first screen
   abstract val navigator: Navigator  // Keep eager
   
   // App configuration required at launch
   abstract val appConfig: AppConfig  // Keep eager
   ```

2. **Cheap/Lightweight Objects**
   ```kotlin
   // Simple values - no benefit from lazy
   @Provides fun provideAppName(): String = "MyApp"
   @Provides fun provideDebugFlag(): Boolean = BuildConfig.DEBUG
   
   // Data classes with no logic
   data class Settings(val theme: String, val locale: String)
   ```

3. **Frequently Accessed Dependencies**
   ```kotlin
   // Used on every screen/request
   class ApiService(private val auth: AuthManager)  // Keep eager
   
   // Called in tight loops
   class Calculator(private val mathUtils: MathUtils)  // Keep eager
   ```

4. **Validation & Fail-Fast Components**
   ```kotlin
   // Want immediate failure if misconfigured
   class SecurityManager(private val config: SecurityConfig) {
       init {
           config.validate() // Fail at startup, not runtime
       }
   }
   ```

5. **Simple Factories & Providers**
   ```kotlin
   // Lightweight factory methods
   @Provides fun provideGson(): Gson = Gson()  // Keep eager
   ```

### Performance Considerations

#### Lazy Overhead Analysis

```kotlin
// Lazy access has overhead
class Example {
    // Each .value call has:
    // - Null check
    // - Thread-safety check (if synchronized)
    // - Potential lock acquisition
    fun frequent(api: Lazy<Api>) {
        repeat(1000) {
            api.value.call()  // Overhead × 1000
        }
    }
    
    // Better: Cache the value locally
    fun better(api: Lazy<Api>) {
        val apiInstance = api.value  // Single overhead
        repeat(1000) {
            apiInstance.call()  // Direct access
        }
    }
}
```

#### Memory Trade-offs

- **Lazy wrapper overhead**: ~16-24 bytes per instance
- **Synchronization state**: Additional memory for thread-safety
- **Double storage**: During initialization, both Lazy and instance exist briefly

### Decision Framework

Use this decision tree to determine if lazy injection is appropriate:

```
┌─ Is it needed at app startup? ─┐
│                                 │
│ YES → Use EAGER injection       │
│                                 │
│ NO ↓                           │
│                                 │
├─ Is it expensive to create? ───┤
│                                 │
│ NO → Use EAGER injection        │
│                                 │
│ YES ↓                          │
│                                 │
├─ Is it frequently accessed? ───┤
│                                 │
│ YES → Consider EAGER injection  │
│      (benchmark to decide)      │
│                                 │
│ NO ↓                           │
│                                 │
├─ Is it always used? ───────────┤
│                                 │
│ YES → Consider EAGER injection  │
│      (unless very expensive)    │
│                                 │
│ NO → Use LAZY injection ✓       │
└─────────────────────────────────┘
```

### Implementation Examples

#### 1. Lazy Network Client
```kotlin
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class QuotesRepoImpl(
    private val api: Lazy<NetworkApi>,  // Network client created on first use
) : QuotesRepo {
    
    override suspend fun quoteForTheDay(): Quote {
        // NetworkApi is initialized here, not at injection time
        return api.value.client().get("https://api.example.com/quote")
    }
}
```

#### 2. Lazy Multi-Bindings
```kotlin
// In AppComponent
abstract val loggers: Lazy<Set<LogcatLogger>>
abstract val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>

// Usage in MainActivity
val entryProviders = appComponent.entryProviderInstallers
// ... later when needed
entryProvider = entryProvider { 
    entryProviders.value.forEach { it() }  // Resolved on first navigation
}
```

#### 3. Lazy Feature Screens
```kotlin
interface SettingsComponent {
    val settingsAScreen: Lazy<SettingsAScreen>
    val settingsBScreen: Lazy<SettingsBScreen>
}

// Navigation setup - screens created only when navigated to
entry<ScreenARoute> { 
    settingsComponent.settingsAScreen.value.Content() 
}
entry<ScreenBRoute> { 
    settingsComponent.settingsBScreen.value() 
}
```

### Real-World Lazy Patterns

#### Complete Lazy Chain
Ensure lazy propagates through the entire dependency chain:
```kotlin
// Good: Complete lazy chain
SettingsBScreen (Lazy) → SettingsBindings → QuotesRepo (Lazy) → NetworkApi (Lazy)

// Bad: Broken chain (QuotesRepo eager breaks the chain)
SettingsBScreen (Lazy) → SettingsBindings → QuotesRepo (Eager) → NetworkApi (Lazy)
```

#### Lazy Initialization Patterns
```kotlin
// Pattern 1: Lazy field with single access point
class Repository(private val api: Lazy<ApiService>) {
    suspend fun getData() = api.value.fetch()
}

// Pattern 2: Cache lazy value for multiple uses
class Service(private val client: Lazy<HttpClient>) {
    private val httpClient by lazy { client.value }
    
    fun get() = httpClient.get()
    fun post() = httpClient.post()
}

// Pattern 3: Conditional lazy usage
class FeatureManager(
    private val premiumFeature: Lazy<PremiumService>,
    private val userSettings: UserSettings
) {
    fun executeFeature() {
        if (userSettings.isPremium) {
            premiumFeature.value.execute()  // Only init if premium
        }
    }
}
```

### Testing with Lazy Dependencies

```kotlin
// Test with lazy mocks
class ServiceTest {
    @Test
    fun `test with lazy dependency`() {
        val mockApi = mockk<ApiService>()
        val lazyApi = Lazy { mockApi }
        val service = MyService(lazyApi)
        
        // Verify lazy not accessed until needed
        service.init()
        verify(exactly = 0) { mockApi.anyMethod() }
        
        // Verify lazy accessed when used
        service.fetchData()
        verify(exactly = 1) { mockApi.fetchData() }
    }
}
```

### Migration Guide

Converting existing eager injection to lazy:

```kotlin
// Before: Eager injection
class MyService(@Inject private val api: NetworkApi)

// After: Lazy injection
class MyService(@Inject private val api: Lazy<NetworkApi>)

// Usage changes from:
api.doSomething()

// To:
api.value.doSomething()
```

The lazy value is cached after first access, so subsequent calls to `.value` return the same instance without re-initialization.

### Current Project Analysis

#### Should Remain Eager
- **Navigator** - Required immediately for first screen navigation
- **String constants** (`appName`, `debuggableApp`) - Trivial initialization cost
- **Start destination** - Needed at app launch

#### Currently Lazy (Correct)
- **NetworkApi** - Heavy I/O client with connection pooling
- **QuotesRepo** - Only used in SettingsBScreen
- **Feature screens** - Loaded on navigation
- **Multi-bindings** (`loggers`, `entryProviderInstallers`) - Not immediately needed
- **Feature components** - Created on first navigation

### Summary

Lazy injection is a powerful optimization tool but not a silver bullet. Use it strategically for:
- Heavy initialization costs
- Optional features
- Deferred functionality

Keep eager injection for:
- Startup essentials
- Lightweight objects
- Frequently accessed dependencies
- Fail-fast validations

The goal is to balance startup performance, runtime efficiency, and code maintainability. Profile your app to make data-driven decisions about lazy vs eager injection.

This architecture provides a robust, type-safe dependency injection solution that scales well with multi-module Android applications while maintaining optimal performance through strategic use of lazy initialization.