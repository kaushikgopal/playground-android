# Navigation 3 - Developer-Owned Back Stack

## Quick Overview

Navigation 3 gives **YOU control of the back stack**. It's just a list you manage directly - no framework magic.

**Traditional Navigation 2:**
```kotlin
navController.navigate("settings")  // Framework manages stack internally
```

**Navigation 3 in this app:**
```kotlin
navigator.goTo(SettingsRoutes.ScreenARoute)  // You manage the stack directly
```

**Why this matters:**
- ✅ See exactly what's in your navigation stack
- ✅ Type-safe routes (no string typos)
- ✅ Easy testing (it's just a list)
- ✅ Custom navigation patterns (like `navigateToTab`)

## Core Implementation

### The Navigator

```kotlin
// common/navigation/Navigator.kt:14-54
@SingleIn(AppScope::class)
@Inject
class Navigator(@Named("startDestination") private val startDestination: NavRoute) {
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
    
    // Special method for bottom navigation
    fun navigateToTab(destination: NavRoute) {
        val index = backStack.indexOfFirst { it::class == destination::class }
        if (index != -1) {
            // Tab exists, pop to it
            while (backStack.size > index + 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        } else {
            // New tab, clear and navigate
            backStack.clear()
            backStack.add(destination)
        }
    }
}
```

Navigation is just list manipulation - simple and predictable.

### Type-Safe Routes

```kotlin
// common/navigation/Navigator.kt:57
interface NavRoute : NavKey

// features/home/nav/HomeRoutes.kt:6-8
object HomeRoutes {
    @Serializable 
    data object HomeScreenRoute : NavRoute
}

// features/settings/nav/SettingsGraph.kt:6-10
object SettingsRoutes {
    @Serializable 
    data object ScreenARoute : NavRoute
    @Serializable 
    data object ScreenBRoute : NavRoute
}

// features/discover/nav/DiscoverGraph.kt:6-8
object DiscoverRoutes {
    @Serializable 
    data object DiscoverScreenRoute : NavRoute
}
```

**Why `NavRoute` instead of `NavKey`?**
- Consistent typing across the app
- Navigator only accepts `NavRoute`
- Clear app-specific API

### Connecting Routes to UI

```kotlin
// features/settings/SettingsComponent.kt:34-47
@Provides
@IntoSet
fun provideSettingsEntryProvider(
    navigator: Navigator,
    factory: Factory
): EntryProviderInstaller = {
    val settingsComponent by lazy { factory.createSettingsComponent() }
    
    entry<ScreenARoute> {
        SettingsAScreen(
            bindings = settingsComponent.settingsBindings,
            navToSettingsB = { navigator.goTo(ScreenBRoute) }
        )
    }
    
    entry<ScreenBRoute> { 
        SettingsBScreen(bindings = settingsComponent.settingsBindings) 
    }
}
```

Each feature provides its own navigation mapping through DI.

### MainActivity Setup

```kotlin
// app/ui/MainActivity.kt:24-40
setContent {
    AppTheme(darkTheme = true) {
        val navigator = appComponent.navigator
        val entryProviders = appComponent.entryProviderInstallers
        
        Scaffold(
            bottomBar = { BottomNavBar(navigator = navigator) }
        ) { innerPadding ->
            NavDisplay(
                backStack = navigator.backStack,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onBack = { if (!navigator.goBack()) finish() },
                entryProvider = entryProvider { 
                    entryProviders.value.forEach { installer -> installer() }
                }
            )
        }
    }
}
```

## Navigation Patterns in This App

### Bottom Navigation
```kotlin
// app/ui/BottomNavBar.kt:42-59
bottomNavTabs.forEach { bottomTab ->
    BottomTab(
        bottomTab,
        isSelected = when (bottomTab.route) {
            is HomeRoutes.HomeScreenRoute ->
                currentDestination is HomeRoutes.HomeScreenRoute
            is DiscoverRoutes.DiscoverScreenRoute ->
                currentDestination is DiscoverRoutes.DiscoverScreenRoute
            is SettingsRoutes.ScreenARoute ->
                currentDestination is SettingsRoutes.ScreenARoute || 
                currentDestination is SettingsRoutes.ScreenBRoute
            else -> false
        },
        onTabSelected = { tab ->
            // Uses special navigateToTab for consistent behavior
            navigator.navigateToTab(tab.route)
        }
    )
}
```

### Internal Feature Navigation
```kotlin
// Settings A navigates to Settings B
SettingsAScreen(
    navToSettingsB = { navigator.goTo(ScreenBRoute) }
)
```

### Back Navigation
```kotlin
// In MainActivity
onBack = { if (!navigator.goBack()) finish() }
```

## When to Navigate Where

### Direct Navigator Calls
**When:** Simple navigation from UI components
```kotlin
Button(onClick = { navigator.goTo(SettingsRoutes.ScreenARoute) })
```

### ViewModel with Effects
**When:** Navigation depends on business logic
```kotlin
// In ViewModel
override suspend fun process(event: Event) {
    when (event) {
        is Event.SaveSuccess -> emitEffect(Effect.NavigateBack)
    }
}

// In UI
LaunchedEffect(viewModel) {
    viewModel.effects.collect { effect ->
        when (effect) {
            is Effect.NavigateBack -> navigator.goBack()
        }
    }
}
```

### Entry Provider Navigation
**When:** Internal feature navigation
```kotlin
entry<HomeScreenRoute> {
    HomeScreen(
        onSettingsClick = { navigator.goTo(SettingsRoutes.ScreenARoute) }
    )
}
```

## Multi-Module Structure

```
app/                          # MainActivity with NavDisplay
├── common/navigation/        # Navigator + NavRoute interface
├── features/
│   ├── home/                # HomeScreenRoute
│   ├── settings/            # ScreenARoute, ScreenBRoute
│   └── discover/            # DiscoverScreenRoute
```

Each feature:
1. **Defines routes** (public API in nav package)
2. **Provides entries** (internal in di package)
3. **Registers via DI** (automatic with @IntoSet)

## Testing Navigation

### Unit Testing Navigator
```kotlin
@Test
fun `tab navigation clears stack`() {
    val navigator = Navigator(HomeRoutes.HomeScreenRoute)
    navigator.goTo(SettingsRoutes.ScreenARoute)
    
    navigator.navigateToTab(DiscoverRoutes.DiscoverScreenRoute)
    
    assertEquals(1, navigator.backStack.size)
    assertEquals(DiscoverRoutes.DiscoverScreenRoute, navigator.backStack[0])
}

@Test
fun `back at root returns false`() {
    val navigator = Navigator(HomeRoutes.HomeScreenRoute)
    
    assertFalse(navigator.goBack())
    assertEquals(1, navigator.backStack.size)
}
```

### UI Testing Navigation
```kotlin
@Test
fun `bottom nav switches tabs correctly`() = runComposeTest {
    setContent { MainScreen() }
    
    onNodeWithText("Settings").performClick()
    onNodeWithText("Settings Screen A").assertIsDisplayed()
    
    onNodeWithText("Discover").performClick()
    onNodeWithText("Discover Screen").assertIsDisplayed()
}
```

## Common Issues & Solutions

### Route Not Found
**Problem:** Crash when navigating  
**Solution:** Ensure entry provider is registered
```kotlin
@Provides
@IntoSet  // Don't forget this!
fun provideEntries(): EntryProviderInstaller = { /* ... */ }
```

### Lost State on Rotation
**Problem:** Returns to start after rotation  
**Solution:** Routes must be `@Serializable` and implement `NavRoute`

### Duplicate Screens
**Problem:** Same screen multiple times in stack  
**Solution:** Check before adding
```kotlin
if (backStack.lastOrNull() != targetRoute) {
    goTo(targetRoute)
}
```

## Performance Tips

### 1. Lazy Component Creation
```kotlin
// Components created once and cached
val component by lazy { factory.createComponent() }
entry<Route> { Screen(component.bindings) }
```

### 2. Light Route Objects
```kotlin
// ✅ Good: Just IDs
data class ProductRoute(val id: String) : NavRoute

// ❌ Bad: Full objects
data class ProductRoute(val product: Product) : NavRoute
```

### 3. Remember in Entries
```kotlin
entry<Route> {
    val data = remember { expensiveOperation() }
    Screen(data)
}
```

## Migration from Navigation 2

| Navigation 2 | Navigation 3 (This App) |
|--------------|------------------------|
| `NavController` | `Navigator` with visible stack |
| `NavHost` | `NavDisplay` |
| String routes | `@Serializable` data objects |
| `navigate("route")` | `goTo(Route)` |
| `popBackStack()` | `goBack()` |
| Hidden stack | `backStack: SnapshotStateList` |

## Key Benefits

1. **Transparency** - Debug navigation state easily
2. **Type Safety** - No string-based errors
3. **Flexibility** - Custom patterns like `navigateToTab`
4. **Testability** - Just test list operations
5. **Simplicity** - ~40 lines of navigation code you control

The entire navigation system is simple, predictable, and fully under your control.