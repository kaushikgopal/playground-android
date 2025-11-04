# Jetpack Navigation 3 Migration Plan

**Version**: 2.0.0  
**Last Updated**: 2025-08-06  
**Status**: Complete Migration Guide — Marked Complete in playground-android

Note: This migration guide has been executed and validated for the target repository. No further action is pending under this plan.

## Overview

This document provides a comprehensive, step-by-step guide for migrating from Compose Navigation 2 to Jetpack Navigation 3 in a multi-module Android application using kotlin-inject-anvil. This plan is based on the successful migration of the Pudi Android app and can be applied to similar projects.

## Table of Contents

1. [Pre-Migration Assessment](#pre-migration-assessment)
2. [Migration Strategy](#migration-strategy)
3. [Step-by-Step Migration](#step-by-step-migration)
4. [Post-Migration Validation](#post-migration-validation)
5. [Common Pitfalls](#common-pitfalls)
6. [Timeline Estimation](#timeline-estimation)

## Pre-Migration Assessment

### Requirements Check

Before starting migration, ensure:

- [ ] Kotlin 2.0+ (for kotlinx.serialization support)
- [ ] Compile SDK 36 or higher
- [ ] kotlin-inject-anvil properly configured
- [ ] All navigation routes are `@Serializable` (or can be made so)
- [ ] KSP version compatible with Kotlin version

### Current State Analysis

Identify and document:

1. **Navigation Structure**
   - Main NavHost location and setup
   - Number of navigation graphs
   - Bottom navigation or drawer navigation
   - Nested navigation flows

2. **Route Definitions**
   - How routes are currently defined (sealed classes, objects, strings)
   - Navigation arguments and their types
   - Deep linking requirements

3. **Dependency Injection**
   - Current DI framework (kotlin-inject-anvil, Hilt, Koin, etc.)
   - How navigation is currently injected
   - Component/module structure

4. **Feature Modules**
   - Module dependencies
   - Public navigation APIs
   - Internal navigation logic

### Example Current State (Navigation 2)

```kotlin
// Routes using sealed classes
sealed class HomeRoutes {
    @Serializable data object HomeGraphRoute
    @Serializable data object HomeScreenRoute
}

// NavHost setup
NavHost(
    navController = navController,
    startDestination = HomeRoutes.HomeGraphRoute
) {
    homeGraph(navController)
    settingsGraph(navController)
    discoverGraph(navController)
}

// Graph extension
fun NavGraphBuilder.homeGraph(navController: NavController) {
    navigation<HomeRoutes.HomeGraphRoute>(
        startDestination = HomeRoutes.HomeScreenRoute
    ) {
        composable<HomeRoutes.HomeScreenRoute> {
            HomeScreen()
        }
    }
}
```

## Migration Strategy

### Approach: Incremental Migration with Minimal Changes

The strategy focuses on reusing existing code while introducing Navigation 3 concepts:

1. **Reuse existing @Serializable routes** - Just extend NavRoute interface
2. **Create centralized Navigator** - Replace NavController
3. **Convert graphs to entry providers** - Map routes to UI
4. **Update MainActivity** - Replace NavHost with NavDisplay
5. **Migrate feature by feature** - Start with simplest features
6. **Clean up legacy code** - Remove old navigation after validation

### Key Principle: Maximum Code Reuse

Instead of rewriting navigation from scratch:
- Keep existing route names and structure
- Reuse existing screen composables
- Maintain current DI component structure
- Preserve navigation logic where possible

## Step-by-Step Migration

### Step 1: Update Dependencies

#### 1.1 Update gradle/libs.versions.toml

```toml
[versions]
kotlin = "2.1.21"  # Or your current version
ksp = "2.1.21-2.0.2"  # Match Kotlin version
nav3Core = "1.0.0-alpha04"

[libraries]
# Remove old navigation
# compose-navigation = { module = "androidx.navigation:navigation-compose", version.ref = "composeNavigation" }

# Add Navigation 3
jetpack-navigation3-runtime = { 
    module = "androidx.navigation3:navigation3-runtime", 
    version.ref = "nav3Core" 
}
jetpack-navigation3-ui = { 
    module = "androidx.navigation3:navigation3-ui", 
    version.ref = "nav3Core" 
}
```

#### 1.2 Update KSP version if needed

If you encounter KSP version mismatch:
```toml
[versions]
ksp = "2.1.21-2.0.2"  # Update to match your Kotlin version
```

### Step 2: Create Navigation Infrastructure

#### 2.1 Create common:navigation module

Create `common/navigation/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.pudi.android.library)
    alias(libs.plugins.pudi.kotlin.inject.anvil)
}

android {
    namespace = "app.pudi.android.common.navigation"
}

dependencies {
    api(libs.jetpack.navigation3.runtime)
    api(libs.jetpack.navigation3.ui)
    implementation(project(":domain:shared"))
}
```

#### 2.2 Create Navigator and NavRoute

Create `common/navigation/src/main/java/.../Navigator.kt`:

```kotlin
package app.pudi.android.common.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import app.pudi.android.shared.di.Named
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

// Custom interface for type safety
interface NavRoute : NavKey

// Type alias for entry provider functions
typealias EntryProviderInstaller = EntryProviderBuilder<NavRoute>.() -> Unit

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

#### 2.3 Add to settings.gradle.kts

```kotlin
include(":common:navigation")
```

### Step 3: Update AppComponent

Modify your main DI component to provide navigation dependencies:

```kotlin
// app/src/main/java/.../di/AppComponent.kt

import app.pudi.android.common.navigation.EntryProviderInstaller
import app.pudi.android.common.navigation.NavRoute
import app.pudi.android.common.navigation.Navigator

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
) {
    // Add these new properties
    abstract val navigator: Navigator
    abstract val entryProviderInstallers: Set<EntryProviderInstaller>
    
    // Add provider for start destination
    @Provides
    @Named("startDestination")
    fun provideStartDestination(): NavRoute = HomeRoutes.HomeScreenRoute
    
    // ... existing code ...
}
```

### Step 4: Update Existing Routes

For each feature, update routes to implement NavRoute:

#### 4.1 Home Feature Routes

```kotlin
// features/home/src/.../nav/HomeRoute.kt

import app.pudi.android.common.navigation.NavRoute
import kotlinx.serialization.Serializable

sealed class HomeRoutes {
    // Just add : NavRoute to existing routes
    @Serializable 
    data object HomeScreenRoute : NavRoute  // Was: data object HomeScreenRoute
    
    // Keep graph routes for now if you have them
    @Serializable 
    data object LandingHomeRoute : NavRoute  // Optional, can be removed later
}
```

#### 4.2 Settings Feature Routes

```kotlin
// features/settings/src/.../nav/SettingsGraph.kt

import app.pudi.android.common.navigation.NavRoute

sealed class SettingsRoutes {
    @Serializable 
    data object ScreenARoute : NavRoute
    
    @Serializable 
    data object ScreenBRoute : NavRoute
    
    @Serializable 
    data object LandingSettingsRoute : NavRoute  // Optional
}
```

#### 4.3 Discover Feature Routes

```kotlin
// features/discover/src/.../nav/DiscoverGraph.kt

import app.pudi.android.common.navigation.NavRoute

sealed class DiscoverRoutes {
    @Serializable 
    data object DiscoverScreenRoute : NavRoute
    
    @Serializable 
    data object LandingDiscoverRoute : NavRoute  // Optional
}
```

### Step 5: Create Feature Navigation Modules

For each feature, create a navigation module that provides entry providers:

#### 5.1 Home Navigation Module

Create `features/home/src/.../di/HomeNavigationModule.kt`:

```kotlin
package app.pudi.android.home.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import app.pudi.android.common.navigation.EntryProviderInstaller
import app.pudi.android.home.nav.HomeRoutes.*
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

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
        // Note: Skip LandingHomeRoute if it's just a wrapper
    }
}
```

#### 5.2 Settings Navigation Module

Create `features/settings/src/.../di/SettingsNavigationModule.kt`:

```kotlin
package app.pudi.android.features.settings.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import app.pudi.android.common.navigation.EntryProviderInstaller
import app.pudi.android.common.navigation.Navigator
import app.pudi.android.features.settings.nav.SettingsRoutes.*
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

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
                // Internal navigation within settings
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

#### 5.3 Update Feature Components

Modify feature components to expose screens properly:

```kotlin
// features/home/src/.../di/HomeComponent.kt

@ContributesSubcomponent(AppScope::class)
interface HomeComponent {
    
    val homeBindings: HomeBindings  // Expose bindings instead of screen
    
    @ContributesSubcomponent.Factory
    interface Factory {
        fun createHomeComponent(): HomeComponent
    }
}
```

### Step 6: Update MainActivity

Replace NavHost with NavDisplay:

```kotlin
// app/src/main/java/.../ui/MainActivity.kt

import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import app.pudi.android.common.navigation.EntryProviderInstaller

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appComponent = AppComponent.from(this)
        
        enableEdgeToEdge()
        setContent {
            AppTheme(darkTheme = true) {
                val navigator = appComponent.navigator
                val entryProviders = appComponent.entryProviderInstallers
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { 
                        BottomNavBar(navigator = navigator) 
                    },
                ) { innerPadding ->
                    NavDisplay(
                        backStack = navigator.backStack,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onBack = { 
                            if (!navigator.goBack()) {
                                finish()  // Exit app if at root
                            }
                        },
                        entryProvider = entryProvider {
                            // Register all feature entry providers
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

### Step 7: Update Bottom Navigation

Modify bottom navigation to use Navigator:

```kotlin
// app/src/main/java/.../ui/BottomNavBar.kt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import app.pudi.android.common.navigation.Navigator

@Composable
fun BottomNavBar(navigator: Navigator) {
    val backStack by rememberUpdatedState(navigator.backStack)
    val currentDestination = backStack.lastOrNull()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .height(64.dp),
    ) {
        bottomNavTabs.forEach { bottomTab ->
            BottomTab(
                bottomTab,
                isSelected = when (bottomTab.route) {
                    is HomeRoutes.LandingHomeRoute -> 
                        currentDestination is HomeRoutes.HomeScreenRoute
                    is DiscoverRoutes.LandingDiscoverRoute -> 
                        currentDestination is DiscoverRoutes.LandingDiscoverRoute
                    is SettingsRoutes.LandingSettingsRoute -> 
                        currentDestination is SettingsRoutes.ScreenARoute || 
                        currentDestination is SettingsRoutes.ScreenBRoute
                    else -> false
                },
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

### Step 8: Clean Up Old Code

After verifying the migration works:

#### 8.1 Remove old navigation functions

Delete or comment out:
- `addHomeGraph()` functions
- `addSettingsGraph()` functions  
- `addDiscoverGraph()` functions
- Any NavGraphBuilder extensions

#### 8.2 Remove old dependencies

From module build files:
```kotlin
// Remove
// implementation(libs.compose.navigation)
```

#### 8.3 Clean up imports

Remove unused imports:
- `androidx.navigation.NavController`
- `androidx.navigation.NavHost`
- `androidx.navigation.NavGraphBuilder`

## Post-Migration Validation

### Testing Checklist

#### Basic Navigation
- [ ] App launches with correct start destination
- [ ] Forward navigation works (goTo)
- [ ] Back navigation works (goBack, hardware back button)
- [ ] Bottom navigation switches tabs correctly
- [ ] Tab selection state updates correctly

#### Navigation State
- [ ] Configuration changes preserve navigation state
- [ ] Process death and restoration work
- [ ] No duplicate screens in back stack
- [ ] Deep links work (if applicable)

#### Feature-Specific
- [ ] Settings A → B navigation works
- [ ] All screens are accessible
- [ ] Navigation callbacks work
- [ ] ViewModels are properly scoped

### Build Verification

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint
```

### Manual Testing Script

1. **Launch app** - Verify starts at home
2. **Navigate to each tab** - Check all tabs work
3. **Test back button** - Ensure proper back behavior
4. **Rotate device** - Verify state preservation
5. **Navigate deeply** - Test multi-level navigation
6. **Kill and restore** - Test process death

## Common Pitfalls

### 1. Wrong DI Annotations

**Problem**: Using Hilt annotations with kotlin-inject-anvil
```kotlin
// Wrong
@ActivityRetainedScoped  // Hilt annotation
```

**Solution**: Use correct kotlin-inject-anvil annotations
```kotlin
// Correct
@SingleIn(AppScope::class)
```

### 2. Wrong Multibinding Annotation

**Problem**: Using @ContributesBinding for functions
```kotlin
// Wrong - @ContributesBinding doesn't work on functions
@ContributesBinding(AppScope::class, multibinding = true)
fun provideEntryProvider(): EntryProviderInstaller
```

**Solution**: Use @IntoSet
```kotlin
// Correct
@IntoSet
fun provideEntryProvider(): EntryProviderInstaller
```

### 3. NavDisplay Import Error

**Problem**: Wrong import package
```kotlin
// Wrong
import androidx.navigation3.runtime.NavDisplay
```

**Solution**: Import from ui package
```kotlin
// Correct
import androidx.navigation3.ui.NavDisplay
```

### 4. KSP Version Mismatch

**Problem**: KSP version incompatible with Kotlin
```
ksp-2.0.20-1.0.25 is too old for kotlin-2.1.21
```

**Solution**: Update KSP version to match Kotlin
```toml
[versions]
kotlin = "2.1.21"
ksp = "2.1.21-2.0.2"  # Match major.minor version
```

### 5. Type Mismatch with Any vs NavRoute

**Problem**: Using Any instead of NavRoute
```kotlin
// Can cause issues
val backStack: SnapshotStateList<Any>
```

**Solution**: Use consistent typing with NavRoute
```kotlin
// Better
val backStack: SnapshotStateList<NavRoute>
```

## Timeline Estimation

### For a Small App (3-5 features)
- **Infrastructure setup**: 30 minutes
- **Update routes**: 15 minutes
- **Create navigation modules**: 45 minutes
- **Update MainActivity**: 30 minutes
- **Testing**: 1 hour
- **Total**: ~3 hours

### For a Medium App (5-10 features)
- **Infrastructure setup**: 1 hour
- **Update routes**: 30 minutes
- **Create navigation modules**: 2 hours
- **Update MainActivity**: 1 hour
- **Testing**: 2 hours
- **Cleanup**: 30 minutes
- **Total**: ~1 day

### For a Large App (10+ features)
- **Planning**: 4 hours
- **Infrastructure setup**: 2 hours
- **Update routes**: 2 hours
- **Create navigation modules**: 1 day
- **Update MainActivity**: 2 hours
- **Testing**: 1 day
- **Cleanup**: 4 hours
- **Total**: ~3-4 days

## Migration Script

For teams wanting to automate parts of the migration:

```kotlin
// Quick script to add NavRoute to existing routes
fun updateRouteFile(file: File) {
    val content = file.readText()
    val updated = content
        .replace(": NavKey", ": NavRoute")
        .replace("sealed class", "import app.pudi.android.common.navigation.NavRoute\n\nsealed class")
        .replace("data object (\\w+)\\s*\$".toRegex(), "data object $1 : NavRoute")
        .replace("data class (\\w+)".toRegex(), "data class $1")
    
    if (!updated.contains(": NavRoute")) {
        // Handle cases where NavKey wasn't used
        val finalUpdate = updated
            .replace("data object (\\w+)(?!.*:)".toRegex(), "data object $1 : NavRoute")
    }
    
    file.writeText(finalUpdate)
}
```

## Rollback Plan

If issues arise during migration:

1. **Keep old navigation code** - Don't delete until validated
2. **Feature flags** - Use flags to switch between old/new
3. **Parallel implementation** - Run both systems temporarily
4. **Incremental rollout** - Migrate one feature at a time

## Success Metrics

Migration is successful when:

- ✅ All navigation flows work correctly
- ✅ No crashes related to navigation
- ✅ State preservation works
- ✅ Tests pass
- ✅ Code is cleaner and more maintainable
- ✅ Team understands new navigation model

## Resources

- [Navigation 3 Documentation](.ai/docs/navigation3-architecture.md)
- [Official Navigation 3 Guide](https://developer.android.com/guide/navigation/navigation-3)
- [kotlin-inject-anvil Documentation](https://github.com/amzn/kotlin-inject-anvil)

## Conclusion

Migrating to Navigation 3 simplifies navigation architecture while providing better type safety and testability. The key to success is:

1. **Reuse existing code** - Don't rewrite everything
2. **Migrate incrementally** - One feature at a time
3. **Test thoroughly** - Validate each step
4. **Keep it simple** - Use NavRoute interface for consistency

The migration typically takes 3-6 hours for small to medium apps, with most time spent on testing and validation. The resulting architecture is cleaner, more maintainable, and easier to test.
