# Navigation 3 - Developer-Owned Back Stack

## Quick Overview

Navigation 3 flips the script: **YOU own the back stack**, not the framework. It's just a list of screens you manage directly.

**Traditional Navigation 2:**
```kotlin
navController.navigate("settings")  // Framework manages stack internally
```

**Navigation 3:**
```kotlin
backStack.add(SettingsRoute)  // You manage the stack directly
```

**Why this matters:**
- ✅ See exactly what's in your navigation stack
- ✅ Manipulate navigation however you want
- ✅ Type-safe routes (no string typos)
- ✅ Easy testing (it's just a list)

## Core Implementation

### The Navigator (Your Stack Manager)

```kotlin
// common/navigation/Navigator.kt:14-34
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
}
```

That's it. Navigation is just list manipulation.

### Type-Safe Routes

```kotlin
// common/navigation/Navigator.kt:36
interface NavRoute : NavKey

// features/landing/nav/LandingRoutes.kt
@Serializable
data object LandingScreenRoute : NavRoute

// features/settings/nav/SettingsRoutes.kt
@Serializable
data object ScreenARoute : NavRoute
@Serializable
data object ScreenBRoute : NavRoute

// With parameters
@Serializable
data class ProductRoute(val productId: String) : NavRoute
```

**Why `NavRoute` instead of `NavKey`?**
- Consistent typing across the app
- Navigator only accepts `NavRoute`
- Cleaner, app-specific API

### Connecting Routes to UI

```kotlin
// features/settings/SettingsComponent.kt:39-45
@Provides
@IntoSet
fun provideSettingsEntryProvider(factory: Factory): EntryProviderInstaller = {
    val settingsComponent by lazy { factory.createSettingsComponent() }
    
    entry<ScreenARoute> { 
        settingsComponent.settingsAScreen.value.Content() 
    }
    entry<ScreenBRoute> { 
        settingsComponent.settingsBScreen.value() 
    }
}
```

Each feature provides its own navigation mapping.

### The Main Activity Setup

```kotlin
// app/ui/MainActivity.kt:24-40
setContent {
    val navigator = appComponent.navigator
    val entryProviders = appComponent.entryProviderInstallers
    
    Scaffold { innerPadding ->
        NavDisplay(
            backStack = navigator.backStack,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            onBack = { if (!navigator.goBack()) finish() },
            entryProvider = entryProvider { 
                entryProviders.value.forEach { it() }  // Install all routes
            }
        )
    }
}
```

## Common Navigation Patterns

### Simple Forward Navigation
```kotlin
// In your UI or ViewModel
navigator.goTo(SettingsRoute)
```

### Back Navigation
```kotlin
// Returns false when at root
if (!navigator.goBack()) {
    activity.finish()  // Exit app
}
```

### Tab/Bottom Navigation
```kotlin
// Replace entire stack for tab switches
navigator.clearAndGoTo(HomeRoute)
```

### Conditional Navigation
```kotlin
// Check current screen before navigating
val current = navigator.backStack.lastOrNull()
if (current !is SettingsRoute) {
    navigator.goTo(SettingsRoute)
}
```

### Pop to Specific Screen
```kotlin
// Extension function for complex navigation
fun Navigator.popTo(route: NavRoute, inclusive: Boolean = false) {
    val index = backStack.indexOfLast { it == route }
    if (index != -1) {
        val endIndex = if (inclusive) index else index + 1
        while (backStack.size > endIndex) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}

// Usage: Go back to home
navigator.popTo(HomeRoute)
```

### Navigation with Parameters
```kotlin
@Serializable
data class ProductRoute(
    val productId: String,
    val source: String = "browse"
) : NavRoute

// Navigate with params
navigator.goTo(ProductRoute(productId = "123", source = "search"))

// Access in destination
entry<ProductRoute> { route ->
    ProductScreen(
        productId = route.productId,
        source = route.source
    )
}
```

## When to Choose What

### Direct Navigator Calls
**When:** Simple navigation from UI
```kotlin
Button(onClick = { navigator.goTo(SettingsRoute) })
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

### Navigation in Entry Providers
**When:** Internal feature navigation
```kotlin
entry<LandingScreenRoute> {
    LandingScreen(
        onSettingsClick = { navigator.goTo(SettingsRoute) }
    )
}
```

## Multi-Module Setup

```
app/                        # MainActivity with NavDisplay
├── common/navigation/      # Navigator + NavRoute interface
├── features/
│   ├── landing/           # Defines LandingRoute, provides entries
│   └── settings/          # Defines SettingsRoutes, provides entries
```

Each feature:
1. **Defines routes** (public API)
2. **Provides entries** (internal implementation)
3. **Registers via DI** (automatic with @IntoSet)

## Testing Navigation

### Unit Test Navigator
```kotlin
@Test
fun `navigation adds to stack`() {
    val navigator = Navigator(HomeRoute)
    
    navigator.goTo(SettingsRoute)
    
    assertEquals(listOf(HomeRoute, SettingsRoute), navigator.backStack)
}

@Test
fun `back navigation at root returns false`() {
    val navigator = Navigator(HomeRoute)
    
    assertFalse(navigator.goBack())
    assertEquals(1, navigator.backStack.size)
}
```

### UI Test Navigation
```kotlin
@Test
fun `settings navigation flow`() = runComposeTest {
    val navigator = Navigator(ScreenARoute)
    
    setContent { SettingsFlow(navigator) }
    
    onNodeWithText("Go to B").performClick()
    
    assertEquals(ScreenBRoute, navigator.backStack.last())
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

### Type Mismatch
**Problem:** Can't pass route to Navigator
**Solution:** Implement `NavRoute`, not `NavKey`
```kotlin
@Serializable
data object MyRoute : NavRoute  // ✅ Correct
```

## Performance Tips

### 1. Lazy Component Creation
```kotlin
val component by lazy { factory.createComponent() }
entry<Route> { component.screen() }
```

### 2. Avoid Heavy Route Objects
```kotlin
// ✅ Good: Just IDs
data class ProductRoute(val id: String) : NavRoute

// ❌ Bad: Full objects
data class ProductRoute(val product: Product) : NavRoute
```

### 3. Use remember for Expensive Ops
```kotlin
entry<Route> {
    val data = remember { loadExpensiveData() }
    Screen(data)
}
```

## Migration from Navigation 2

| Navigation 2 | Navigation 3 |
|--------------|--------------|
| `NavController` | `Navigator` with visible stack |
| `NavHost` | `NavDisplay` |
| String routes | `@Serializable` data classes |
| `navigate("route")` | `goTo(Route)` |
| `popBackStack()` | `goBack()` |
| XML nav graphs | Kotlin entry providers |
| Hidden stack | `backStack: SnapshotStateList` |

## Key Advantages

1. **Transparency** - See and debug your exact navigation state
2. **Flexibility** - Implement any navigation pattern easily
3. **Type Safety** - No string-based navigation errors
4. **Testability** - Just test list operations
5. **Simplicity** - Navigation is just data structure manipulation

The entire navigation system is ~30 lines of code you control, not thousands of lines of framework magic.