# Jetpack Navigation 3 Architecture

**Version**: 1.0.0  
**Last Updated**: 2025-08-06  
**Status**: Production Ready

## Overview

This document describes how Jetpack Navigation 3 is implemented in the Pudi Android app, providing both conceptual understanding and practical implementation details. Navigation 3 represents a paradigm shift from traditional Android navigation, offering developer-owned back stack management and true type-safe navigation without code generation.

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [Architecture in This App](#architecture-in-this-app)
3. [Implementation Details](#implementation-details)
4. [Navigation Patterns](#navigation-patterns)
5. [Module Structure](#module-structure)
6. [Testing Navigation](#testing-navigation)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

## Core Concepts

### Developer-Owned Back Stack

Unlike Navigation 2 where `NavController` manages the back stack internally, Navigation 3 gives developers direct control through a `SnapshotStateList`:

```kotlin
val backStack: SnapshotStateList<NavRoute> = mutableStateListOf(startDestination)
```

This approach provides:
- **Direct manipulation**: Add, remove, or reorder destinations programmatically
- **Observable state**: Back stack changes trigger recomposition automatically
- **Debugging transparency**: Inspect the exact navigation state at any time
- **Custom behaviors**: Implement complex navigation patterns easily

### Type-Safe Navigation Keys

Navigation destinations are represented by `@Serializable` data classes that implement `NavRoute`, a custom interface that extends `NavKey`:

```kotlin
// Custom interface in common/navigation
interface NavRoute : NavKey

// Usage in features
@Serializable
data object HomeScreenRoute : NavRoute

@Serializable
data class ProductDetailRoute(val productId: String) : NavRoute
```

Benefits:
- **Compile-time safety**: Invalid navigation targets fail at compile time
- **No string routes**: Eliminate typos and runtime navigation errors
- **Built-in arguments**: Parameters are part of the type definition
- **IDE support**: Full autocomplete and refactoring support
- **Type consistency**: All navigation uses `NavRoute` for better type safety

### Entry Providers

Entry providers map navigation keys to their UI implementation:

```kotlin
entryProvider {
    entry<HomeScreenRoute> {
        HomeScreen(modifier = Modifier.fillMaxSize())
    }
    
    entry<ProductDetailRoute> { route ->
        ProductDetailScreen(
            productId = route.productId,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

### NavDisplay

Replaces `NavHost` as the container for navigation content:

```kotlin
NavDisplay(
    backStack = navigator.backStack,
    modifier = Modifier.fillMaxSize(),
    onBack = { navigator.goBack() },
    entryProvider = entryProvider { /* ... */ }
)
```

## Architecture in This App

### Navigator Class

The app uses a centralized `Navigator` class to manage all navigation state:

```kotlin
@SingleIn(AppScope::class)
@Inject
class Navigator(
    @Named("startDestination") private val startDestination: NavRoute
) {
    val backStack: SnapshotStateList<NavRoute> = mutableStateListOf(startDestination)
    
    fun goTo(destination: NavRoute) {
        backStack.add(destination)
    }
    
    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        
        backStack.removeAt(backStack.lastIndex)
        return true
    }
    
    fun clearAndGoTo(destination: NavRoute) {
        backStack.clear()
        backStack.add(destination)
    }
}
```

Key aspects:
- **Singleton scope**: Single instance across the app via `@SingleIn(AppScope::class)`
- **Injectable**: Integrated with kotlin-inject-anvil DI
- **Type-safe**: Uses `NavRoute` type for all navigation operations
- **Simple API**: Clear methods for common navigation operations
- **Return values**: `goBack()` returns false when at root for proper app exit

### NavRoute Interface

The app defines a custom `NavRoute` interface that extends Navigation 3's `NavKey`:

```kotlin
// In common/navigation/Navigator.kt
interface NavRoute : NavKey

// Type alias for entry providers
typealias EntryProviderInstaller = EntryProviderBuilder<NavRoute>.() -> Unit
```

This provides:
- **Consistent typing**: All navigation in the app uses `NavRoute`
- **Extension point**: Can add custom functionality to all routes if needed
- **Type safety**: Navigator only accepts `NavRoute` destinations
- **Clean API**: Entry providers work with `NavRoute` type

### Dependency Injection Integration

The app uses kotlin-inject-anvil for dependency injection with Navigation 3:

#### AppComponent Configuration

```kotlin
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent {
    abstract val navigator: Navigator
    abstract val entryProviderInstallers: Set<EntryProviderInstaller>
    
    @Provides
    @Named("startDestination")
    fun provideStartDestination(): NavRoute = HomeRoutes.HomeScreenRoute
}
```

#### Feature Module Registration

Each feature contributes its navigation entries via multibinding:

```kotlin
@ContributesTo(AppScope::class)
interface HomeNavigationModule {
    @Provides
    @IntoSet
    fun provideHomeEntryProvider(
        homeComponentFactory: HomeComponent.Factory
    ): EntryProviderInstaller = {
        entry<HomeScreenRoute> {
            val homeComponent = homeComponentFactory.createHomeComponent()
            app.pudi.android.home.ui.HomeScreen(
                bindings = homeComponent.homeBindings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

### Multi-Module Navigation

The app's modular architecture leverages Navigation 3's design:

```
app/                          # Main app module with MainActivity
├── common/
│   └── navigation/          # Navigator, NavRoute interface, and base types
├── features/
│   ├── home/               # Home feature with its routes
│   ├── settings/           # Settings feature with navigation
│   └── discover/           # Discover feature module
└── domain/
    └── shared/             # Shared domain models
```

Each feature module:
1. Defines its navigation routes implementing `NavRoute` (public API)
2. Provides entry provider implementations (internal)
3. Registers with AppScope via DI

## Implementation Details

### Route Definitions

All routes in the app implement `NavRoute` and are serializable:

```kotlin
// Home Feature
sealed class HomeRoutes {
    @Serializable 
    data object LandingHomeRoute : NavRoute
    
    @Serializable 
    data object HomeScreenRoute : NavRoute
}

// Settings Feature
sealed class SettingsRoutes {
    @Serializable 
    data object LandingSettingsRoute : NavRoute
    
    @Serializable 
    data object ScreenARoute : NavRoute
    
    @Serializable 
    data object ScreenBRoute : NavRoute
}

// Discover Feature
sealed class DiscoverRoutes {
    @Serializable 
    data object LandingDiscoverRoute : NavRoute
}
```

Note: Landing routes are legacy from Navigation 2 and can be removed in future refactoring.

### MainActivity Setup

The main activity creates the navigation container:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appComponent = AppComponent.from(this)
        
        setContent {
            AppTheme(darkTheme = true) {
                val navigator = appComponent.navigator
                val entryProviders = appComponent.entryProviderInstallers
                
                Scaffold(
                    bottomBar = { 
                        BottomNavBar(navigator = navigator) 
                    }
                ) { innerPadding ->
                    NavDisplay(
                        backStack = navigator.backStack,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onBack = { 
                            if (!navigator.goBack()) {
                                finish()
                            }
                        },
                        entryProvider = entryProvider {
                            entryProviders.forEach { installer ->
                                this.installer()
                            }
                        }
                    )
                }
            }
        }
    }
}
```

### Bottom Navigation Implementation

The bottom navigation bar observes the back stack to update selection:

```kotlin
@Composable
fun BottomNavBar(navigator: Navigator) {
    val backStack by rememberUpdatedState(navigator.backStack)
    val currentDestination = backStack.lastOrNull()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .height(64.dp)
    ) {
        bottomNavTabs.forEach { bottomTab ->
            val isSelected = when (bottomTab.route) {
                is HomeRoutes.LandingHomeRoute -> 
                    currentDestination is HomeRoutes.HomeScreenRoute
                is DiscoverRoutes.LandingDiscoverRoute -> 
                    currentDestination is DiscoverRoutes.LandingDiscoverRoute
                is SettingsRoutes.LandingSettingsRoute -> 
                    currentDestination is SettingsRoutes.ScreenARoute || 
                    currentDestination is SettingsRoutes.ScreenBRoute
                else -> false
            }
            
            BottomTab(
                bottomTab,
                isSelected = isSelected,
                onTabSelected = { tab ->
                    when (tab.route) {
                        is HomeRoutes.LandingHomeRoute -> 
                            navigator.clearAndGoTo(HomeRoutes.HomeScreenRoute)
                        is DiscoverRoutes.LandingDiscoverRoute -> 
                            navigator.goTo(DiscoverRoutes.LandingDiscoverRoute)
                        is SettingsRoutes.LandingSettingsRoute -> 
                            navigator.goTo(SettingsRoutes.ScreenARoute)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

### Feature Navigation Modules

Each feature provides its navigation implementation:

#### Home Feature

```kotlin
@ContributesTo(AppScope::class)
interface HomeNavigationModule {
    @Provides
    @IntoSet
    fun provideHomeEntryProvider(
        homeComponentFactory: HomeComponent.Factory
    ): EntryProviderInstaller = {
        entry<HomeScreenRoute> {
            val homeComponent = homeComponentFactory.createHomeComponent()
            app.pudi.android.home.ui.HomeScreen(
                bindings = homeComponent.homeBindings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

#### Settings Feature with Internal Navigation

```kotlin
@ContributesTo(AppScope::class)
interface SettingsNavigationModule {
    @Provides
    @IntoSet
    fun provideSettingsEntryProvider(
        navigator: Navigator,
        settingsComponentFactory: SettingsComponent.Factory
    ): EntryProviderInstaller = {
        entry<ScreenARoute> {
            val settingsComponent = settingsComponentFactory.createSettingsComponent()
            settingsComponent.screenA { 
                navigator.goTo(ScreenBRoute)
            }
        }
        
        entry<ScreenBRoute> {
            val settingsComponent = settingsComponentFactory.createSettingsComponent()
            settingsComponent.screenB()
        }
    }
}
```

## Navigation Patterns

### Standard Forward Navigation

```kotlin
// Navigate to a new screen
navigator.goTo(ProductDetailRoute(productId = "123"))
```

### Back Navigation

```kotlin
// Go back one screen
if (!navigator.goBack()) {
    // At root, exit app
    activity.finish()
}
```

### Replace Current Screen

```kotlin
// Clear stack and go to new destination
navigator.clearAndGoTo(HomeScreenRoute)
```

### Conditional Navigation

```kotlin
// Only navigate if not already there
val currentDestination = navigator.backStack.lastOrNull()
if (currentDestination !is SettingsScreenRoute) {
    navigator.goTo(SettingsScreenRoute)
}
```

### Pop to Specific Destination

```kotlin
// Extension function for popping to a destination
fun Navigator.popTo(destination: NavRoute, inclusive: Boolean = false) {
    val index = backStack.indexOfLast { it == destination }
    if (index != -1) {
        val endIndex = if (inclusive) index else index + 1
        while (backStack.size > endIndex) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}
```

### Navigation with Results

```kotlin
// Screen A launches Screen B and expects a result
data class ScreenBRoute(val requestId: String) : NavRoute

// In Screen B, navigate back with result
navigator.goBack()
// Use a shared ViewModel or state holder to pass the result
```

## Module Structure

### Common Navigation Module

Located at `common/navigation/`, provides:
- `Navigator` class
- `NavRoute` interface extending `NavKey`
- `EntryProviderInstaller` typealias
- Base navigation utilities

Build configuration:
```kotlin
plugins {
    alias(libs.plugins.pudi.android.library)
    alias(libs.plugins.pudi.kotlin.inject.anvil)
}

dependencies {
    api(libs.jetpack.navigation3.runtime)
    api(libs.jetpack.navigation3.ui)
    implementation(project(":domain:shared"))
}
```

### Feature Module Structure

Each feature module follows this pattern:

```
features/home/
├── src/main/kotlin/
│   └── app/pudi/android/home/
│       ├── nav/
│       │   └── HomeRoute.kt        # Public navigation keys implementing NavRoute
│       ├── di/
│       │   ├── HomeComponent.kt    # Feature DI component
│       │   └── HomeNavigationModule.kt  # Navigation registration
│       └── ui/
│           └── HomeScreen.kt       # UI implementation
└── build.gradle.kts
```

### Dependencies Flow

```
app → features/* → common/navigation
                ↘ domain/shared
```

## Testing Navigation

### Unit Testing Navigator

```kotlin
class NavigatorTest {
    private lateinit var navigator: Navigator
    
    @BeforeEach
    fun setup() {
        navigator = Navigator(startDestination = HomeScreenRoute)
    }
    
    @Test
    fun `goTo adds destination to back stack`() {
        navigator.goTo(SettingsScreenRoute)
        
        assertEquals(2, navigator.backStack.size)
        assertEquals(HomeScreenRoute, navigator.backStack[0])
        assertEquals(SettingsScreenRoute, navigator.backStack[1])
    }
    
    @Test
    fun `goBack removes last destination`() {
        navigator.goTo(SettingsScreenRoute)
        
        val result = navigator.goBack()
        
        assertTrue(result)
        assertEquals(1, navigator.backStack.size)
        assertEquals(HomeScreenRoute, navigator.backStack[0])
    }
    
    @Test
    fun `goBack returns false at root`() {
        val result = navigator.goBack()
        
        assertFalse(result)
        assertEquals(1, navigator.backStack.size)
    }
    
    @Test
    fun `clearAndGoTo replaces entire stack`() {
        navigator.goTo(SettingsScreenRoute)
        navigator.goTo(DiscoverScreenRoute)
        
        navigator.clearAndGoTo(HomeScreenRoute)
        
        assertEquals(1, navigator.backStack.size)
        assertEquals(HomeScreenRoute, navigator.backStack[0])
    }
}
```

### Testing Entry Providers

```kotlin
@Test
fun `entry providers are registered correctly`() {
    val appComponent = createTestAppComponent()
    
    val entryProviders = appComponent.entryProviderInstallers
    
    assertTrue(entryProviders.isNotEmpty())
    // Verify specific providers are registered
}
```

### UI Testing Navigation Flows

```kotlin
class NavigationFlowTest {
    @Test
    fun `bottom navigation switches between tabs`() = runComposeTest {
        setContent {
            TestApp()
        }
        
        // Start at Home
        onNodeWithText("Home Screen").assertIsDisplayed()
        
        // Navigate to Settings
        onNodeWithText("Settings").performClick()
        onNodeWithText("Settings Screen A").assertIsDisplayed()
        
        // Navigate to Discover
        onNodeWithText("Discover").performClick()
        onNodeWithText("Discover Screen").assertIsDisplayed()
    }
    
    @Test
    fun `settings internal navigation works`() = runComposeTest {
        val navigator = Navigator(ScreenARoute)
        
        setContent {
            TestSettingsFlow(navigator)
        }
        
        // Start at Screen A
        onNodeWithText("Screen A").assertIsDisplayed()
        
        // Navigate to Screen B
        onNodeWithText("Go to B").performClick()
        
        // Verify navigation
        assertEquals(ScreenBRoute, navigator.backStack.last())
    }
}
```

## Best Practices

### 1. Route Organization

- Keep routes in dedicated `nav` packages
- Use sealed classes for grouping related routes
- Make routes part of feature's public API
- Use data objects for parameterless routes
- Use data classes for routes with parameters
- Always implement `NavRoute` for type consistency

### 2. Navigator Usage

- Inject Navigator where needed, don't pass through composition
- Create extension functions for common navigation patterns
- Handle edge cases (already at destination, at root, etc.)
- Keep navigation logic in ViewModels or use cases when complex
- Use `NavRoute` type for all navigation operations

### 3. Entry Provider Organization

- One entry provider per feature module
- Use factory pattern for component creation
- Keep entry providers focused on mapping, not logic
- Handle all feature routes in the same provider
- Type as `EntryProviderInstaller` for consistency

### 4. State Management

- Pass only IDs through navigation, not full objects
- Load data in destination screens
- Use ViewModels for complex state
- Consider using SavedStateHandle for process death

### 5. Performance

- Lazy load feature components in entry providers
- Avoid creating heavy objects in navigation keys
- Use remember for expensive computations in entries
- Profile navigation transitions for jank

### 6. Testing

- Unit test Navigator logic thoroughly
- Test entry provider registration
- UI test critical navigation flows
- Test configuration changes and process death
- Mock Navigator in ViewModel tests

## Troubleshooting

### Common Issues

#### Issue: "Unresolved reference 'NavDisplay'"
**Solution**: Import from correct package:
```kotlin
import androidx.navigation3.ui.NavDisplay  // Correct
// Not from androidx.navigation3.runtime
```

#### Issue: Entry provider not found for route
**Symptom**: Crash when navigating to a screen
**Solution**: Ensure the route's entry provider is registered and implements `NavRoute`:
```kotlin
@Serializable
data object YourRoute : NavRoute  // Must implement NavRoute

entry<YourRoute> {
    // Your composable
}
```

#### Issue: Navigation state lost on configuration change
**Symptom**: App returns to start destination after rotation
**Solution**: Ensure routes implement `NavRoute` and are `@Serializable`

#### Issue: Back button not working
**Symptom**: Hardware back button doesn't navigate back
**Solution**: Implement onBack in NavDisplay:
```kotlin
NavDisplay(
    onBack = { 
        if (!navigator.goBack()) {
            activity.finish()
        }
    }
)
```

#### Issue: Type mismatch with Navigator
**Symptom**: Cannot pass route to Navigator methods
**Solution**: Ensure all routes implement `NavRoute`:
```kotlin
// Correct
@Serializable
data object MyRoute : NavRoute

// Incorrect - won't work with Navigator
@Serializable  
data object MyRoute : NavKey  // Should be NavRoute
```

#### Issue: Duplicate screens in back stack
**Symptom**: Same screen appears multiple times
**Solution**: Check before navigating:
```kotlin
if (backStack.lastOrNull() !is TargetRoute) {
    navigator.goTo(TargetRoute)
}
```

#### Issue: Component not available in entry provider
**Symptom**: DI error when creating screens
**Solution**: Ensure component factory is properly injected and used

### Debug Tips

1. **Log back stack changes**:
```kotlin
LaunchedEffect(navigator.backStack) {
    Log.d("Navigation", "Back stack: ${navigator.backStack}")
}
```

2. **Visualize navigation state**:
```kotlin
@Composable
fun DebugNavigationOverlay(navigator: Navigator) {
    if (BuildConfig.DEBUG) {
        Text(
            text = navigator.backStack.joinToString(" → ") { 
                it::class.simpleName ?: "Unknown" 
            }
        )
    }
}
```

3. **Test navigation in isolation**:
```kotlin
@Preview
@Composable
fun PreviewNavigation() {
    val navigator = remember { Navigator(HomeScreenRoute) }
    // Test UI with navigator
}
```

## Migration from Navigation 2

For teams migrating from Navigation 2, key changes include:

1. **Define NavRoute interface**: Create custom interface extending NavKey
2. **Replace NavController with Navigator**: Use custom Navigator class
3. **Replace NavHost with NavDisplay**: Switch to new display component
4. **Convert routes to @Serializable NavRoute classes**: Implement NavRoute
5. **Replace NavGraphBuilder with EntryProviderInstaller**: Use new pattern
6. **Update DI to provide Navigator and entry providers**: Configure injection
7. **Refactor navigation calls to use Navigator methods**: Update call sites

See `.ai/plans/pudi-nav3-migration.md` for detailed migration steps.

## Future Enhancements

Potential improvements for the navigation architecture:

1. **Remove landing routes**: Simplify by removing legacy graph wrappers
2. **Add navigation middleware**: For analytics, logging, auth checks
3. **Implement deep linking**: Support for external navigation
4. **Add transition animations**: Custom transitions between screens
5. **ViewModel scoping**: Add support for navigation-scoped ViewModels
6. **Navigation testing DSL**: Create testing utilities for navigation
7. **NavRoute extensions**: Add common functionality to NavRoute interface

## Conclusion

Jetpack Navigation 3 provides a simpler, more maintainable approach to navigation in Compose applications. This app's implementation demonstrates:

- Clean separation of navigation logic with custom `NavRoute` interface
- Type-safe navigation without code generation  
- Natural multi-module support
- Testable navigation architecture
- Direct control over navigation behavior
- Consistent typing throughout the navigation system

The architecture scales well with app complexity while remaining debuggable and maintainable.