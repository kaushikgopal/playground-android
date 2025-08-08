# Dependency Injection with kotlin-inject + Anvil

## Quick Overview

This app uses **kotlin-inject** (compile-time DI) with **Anvil** (automatic component merging) for dependency injection. Think of it as Dagger but simpler and fully compile-time safe.

**Why this combo?**
- ✅ Zero runtime reflection (unlike Hilt/Dagger)
- ✅ Compile-time errors for missing dependencies
- ✅ Automatic component merging across modules
- ✅ Native Kotlin (no Java annotation processors)

## Core Concepts in 30 Seconds

```kotlin
// 1. Mark classes as injectable
@Inject
class MyService(private val api: NetworkApi)

// 2. Provide to app scope automatically
@ContributesBinding(AppScope::class)
class MyServiceImpl : MyService

// 3. Create subcomponents for features
@ContributesSubcomponent(FeatureScope::class)
interface FeatureComponent

// 4. Everything merges into AppComponent automatically
@MergeComponent(AppScope::class)
abstract class AppComponent
```

## Real Implementation Examples

### Basic Service Injection

```kotlin
// domain/quoter/impl/QuotesRepoImpl.kt:9-16
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class QuotesRepoImpl(
    private val api: Lazy<NetworkApi>,  // Lazy for performance
) : QuotesRepo {
    override suspend fun quoteForTheDay() = api.value.getQuote()
}
```

**Key patterns:**
- `@Inject` - Makes class injectable
- `@SingleIn(AppScope::class)` - One instance per app
- `@ContributesBinding` - Auto-binds implementation to interface
- `Lazy<T>` - Defers initialization until first use

### Feature Components (Scoped Isolation)

```kotlin
// features/settings/impl/SettingsComponent.kt:20-46
@ContributesSubcomponent(SettingsScope::class)
@SingleIn(SettingsScope::class)
interface SettingsComponent {
    // Lazy-loaded screens
    val settingsAScreen: Lazy<SettingsAScreen>
    val settingsBScreen: Lazy<SettingsBScreen>
    
    @ContributesSubcomponent.Factory(AppScope::class)
    interface Factory {
        fun createSettingsComponent(): SettingsComponent
        
        @Provides
        @IntoSet
        fun provideSettingsEntryProvider(factory: Factory): EntryProviderInstaller = {
            val settingsComponent by lazy { factory.createSettingsComponent() }
            entry<ScreenARoute> { settingsComponent.settingsAScreen.value.Content() }
        }
    }
}
```

**Why subcomponents?**
- Feature isolation (settings can't access landing dependencies)
- Lifecycle management (cleaned up when feature exits)
- Performance (only created when navigated to)

### The App Component Hub

```kotlin
// app/AppComponent.kt:17-38
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)  // Merges all @ContributesBinding
abstract class AppComponent(
    @get:Provides val app: App,
) : SettingsComponent.Factory {  // Implements factory interfaces
    
    // Simple provides
    @Provides 
    fun provideAppName(): @Named("appName") String = "My Playground!"
    
    // Multi-bindings collected from all modules
    abstract val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>
    
    // Core services
    abstract val navigator: Navigator
}
```

## When to Use What

### Use `@Inject` Constructor
**When:** Creating any class that needs dependencies
```kotlin
@Inject
class SettingsBViewModelImpl(
    coroutineScope: CoroutineScope,
    val quotesRepo: Lazy<QuotesRepo>,
)
```

### Use `@ContributesBinding`
**When:** Binding implementation to interface across modules
```kotlin
@ContributesBinding(AppScope::class, boundType = QuotesRepo::class)
class QuotesRepoImpl : QuotesRepo
```

### Use `@ContributesSubcomponent`
**When:** Creating isolated feature scopes
```kotlin
@ContributesSubcomponent(SettingsScope::class)
interface SettingsComponent
```

### Use `Lazy<T>`
**When:** Deferring expensive initialization
```kotlin
// ✅ Good: Heavy network client
val api: Lazy<NetworkApi>

// ❌ Bad: Simple string
val name: Lazy<String>  // Unnecessary overhead
```

## Multi-Bindings (Plugin Pattern)

The app uses multi-bindings for extensible features like navigation:

```kotlin
// Each feature contributes its navigation
@Provides
@IntoSet
fun provideSettingsEntryProvider(): EntryProviderInstaller = { /* ... */ }

// AppComponent collects all contributions
abstract val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>

// MainActivity uses them all
entryProviders.value.forEach { it() }
```

**When to use:** Plugin systems, navigation providers, loggers, analytics

## Function Injection (Composables as Dependencies)

Unique kotlin-inject feature - inject Compose functions:

```kotlin
// Define type alias
typealias SettingsBScreen = @Composable () -> Unit

// Make composable injectable
@Inject
@Composable
fun SettingsBScreen(bindings: SettingsBindings) { /* UI */ }

// Use in component
val settingsBScreen: Lazy<SettingsBScreen>

// Call in navigation
entry<ScreenBRoute> { settingsComponent.settingsBScreen.value() }
```

## Performance Best Practices

### 1. Use Lazy for Heavy Dependencies
```kotlin
// ✅ Network clients, databases, SDKs
val api: Lazy<NetworkApi>
val db: Lazy<AppDatabase>

// ❌ Simple values, frequently used
val config: Config  // Keep eager
```

### 2. Lazy Multi-Bindings
```kotlin
// Only resolve when needed
abstract val loggers: Lazy<Set<LogcatLogger>>
```

### 3. Subcomponent Creation
```kotlin
// Create once, cache in lazy
val settingsComponent by lazy { 
    factory.createSettingsComponent() 
}
```

## Common Patterns

### Named Dependencies
```kotlin
@Provides 
fun provideApiUrl(): @Named("apiUrl") String = "https://api.example.com"

// Usage
@Inject
class ApiClient(@Named("apiUrl") val baseUrl: String)
```

### Scoped Coroutines
```kotlin
@Provides
@SingleIn(SettingsScope::class)
fun provideCoroutineScope(): CoroutineScope = 
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
```

### Factory Pattern
```kotlin
interface ViewModelFactory {
    fun create(id: String): MyViewModel
}
```

## Testing

```kotlin
class TestAppComponent : AppComponent {
    override val api = Lazy { mockk<NetworkApi>() }
    override val navigator = TestNavigator()
}
```

## Migration from Dagger/Hilt

| Dagger/Hilt | kotlin-inject + Anvil |
|-------------|----------------------|
| `@Module` | Not needed |
| `@Component` | `@MergeComponent` |
| `@Subcomponent` | `@ContributesSubcomponent` |
| `@Binds` | `@ContributesBinding` |
| `@Singleton` | `@SingleIn(AppScope::class)` |
| `@ActivityScoped` | `@SingleIn(CustomScope::class)` |
| `@Provides` | `@Provides` (same) |
| `@IntoSet` | `@IntoSet` (same) |

## Troubleshooting

### "Cannot find symbol AppComponent"
**Fix:** Run `./gradlew kspKotlin` to generate

### "Unresolved reference for binding"
**Fix:** Ensure implementation has `@ContributesBinding(AppScope::class)`

### "Cyclic dependency detected"
**Fix:** Use `Lazy<T>` to break the cycle

### "Multiple bindings for same type"
**Fix:** Use `@Named` qualifiers or different interfaces

## Key Takeaways

1. **Everything is compile-time** - No runtime surprises
2. **Lazy by default** for heavy dependencies
3. **Subcomponents** for feature isolation
4. **Multi-bindings** for extensibility
5. **Anvil merges automatically** - No manual wiring

The DI setup prioritizes simplicity and performance, making it easy to add new features without boilerplate while maintaining fast app startup.