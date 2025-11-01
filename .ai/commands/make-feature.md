# Create Feature Command

Creates a new feature for Pudi with USF architecture. Generates all boilerplate following project conventions.

## Feature Types

**UI Feature** - Screen with USF ViewModel (e.g., `:features:discover`)
**Domain Feature** - Business logic, no UI (e.g., `:domain:quoter`)

## Execution Strategy

**For complex features or significant changes**, create an exec-plan first (see `@.ai/plans/PLANS.md`). Use this command to generate the initial boilerplate, then use the exec-plan to implement the actual feature logic.

**For simple CRUD/list screens**, this command alone may suffice.

## Workflow

### 1. Gather Information

Ask the user:
- **Feature name** (PascalCase, e.g., `FeedReader`, `ArticleSync`)
- **Type**: `ui` or `domain`
- **Module structure**: `single` or `api-impl` (recommend single unless interface needs cross-module exposure)

If not provided, prompt for each.

Validate feature name is PascalCase. Reject if not.

### 2. Determine Paths and Packages

**UI Feature:**
- Module: `:features:<kebab-case>` (e.g., `:features:feed-reader`)
- Package: `app.pudi.android.<lowercase>` (e.g., `app.pudi.android.feedreader`)
- Structure: `di/`, `nav/`, `ui/` packages

**Domain Feature:**
- Module: `:domain:<kebab-case>` (e.g., `:domain:feed-parser`)
- Package (single): `app.pudi.android.domain.<lowercase>`
- Package (api-impl): `app.pudi.android.<lowercase>.api` and `app.pudi.android.<lowercase>.impl`

Generate name variations:
- PascalCase: `FeedReader`
- camelCase: `feedReader`
- kebab-case: `feed-reader`
- lowercase: `feedreader`

### 3. Create Directory Structure

**UI Feature (single module):**
```
features/<kebab>/
├── build.gradle.kts
└── src/main/java/app/pudi/android/<lowercase>/
    ├── di/
    ├── nav/
    └── ui/
```

**Domain Feature (api-impl):**
```
domain/<kebab>/
├── api/
│   ├── build.gradle.kts
│   └── src/main/java/app/pudi/android/<lowercase>/api/
└── impl/
    ├── build.gradle.kts
    └── src/main/java/app/pudi/android/<lowercase>/impl/
```

### 4. Generate Files

#### UI Feature Files

**`di/<Feature>Scope.kt`**
```kotlin
package app.pudi.android.<lowercase>.di

import me.tatarka.inject.annotations.Scope

@Scope
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class <Feature>Scope
```

**`di/<Feature>Component.kt`**
```kotlin
package app.pudi.android.<lowercase>.di

import androidx.navigation3.runtime.entry
import app.pudi.android.<lowercase>.nav.<Feature>Routes
import app.pudi.android.<lowercase>.ui.<Feature>Screen
import app.pudi.android.navigation.EntryProviderInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(<Feature>Scope::class)
@SingleIn(<Feature>Scope::class)
interface <Feature>Component {

  val <camelCase>Screen: Lazy<<Feature>Screen>

  @Provides
  @SingleIn(<Feature>Scope::class)
  fun provideCoroutineScope(): CoroutineScope =
      CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun create<Feature>Component(): <Feature>Component

    @Provides
    @IntoSet
    fun provide<Feature>EntryProvider(factory: Factory): EntryProviderInstaller = {
      val component by lazy { factory.create<Feature>Component() }
      entry<<Feature>Routes.<Feature>ScreenRoute> { component.<camelCase>Screen.value() }
    }
  }
}
```

**`nav/<Feature>Routes.kt`**
```kotlin
package app.pudi.android.<lowercase>.nav

import app.pudi.android.navigation.NavRoute
import kotlinx.serialization.Serializable

object <Feature>Routes {
  @Serializable data object <Feature>ScreenRoute : NavRoute
}
```

**`ui/<Feature>ViewModel.kt`**
```kotlin
package app.pudi.android.<lowercase>.ui

import app.pudi.android.usf.api.Usf

interface <Feature>ViewModel : Usf<<Feature>Event, <Feature>UiState, <Feature>Effect>

sealed interface <Feature>Event {
  data object BackClicked : <Feature>Event
}

data class <Feature>UiState(
    val isLoading: Boolean = false,
    val onBackClicked: () -> Unit = {},
)

sealed interface <Feature>Effect {
  data object NavigateBack : <Feature>Effect
}
```

**`ui/<Feature>ViewModelImpl.kt`**
```kotlin
package app.pudi.android.<lowercase>.ui

import app.pudi.android.<lowercase>.di.<Feature>Scope
import app.pudi.android.usf.scope.ResultScope
import app.pudi.android.usf.viewmodel.UsfViewModel
import app.pudi.android.usf.viewmodel.inputEventCallback
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesBinding(<Feature>Scope::class, boundType = <Feature>ViewModel::class)
@SingleIn(<Feature>Scope::class)
@Inject
class <Feature>ViewModelImpl(
    coroutineScope: CoroutineScope,
) :
    <Feature>ViewModel,
    UsfViewModel<<Feature>Event, <Feature>UiState, <Feature>Effect>(
        coroutineScope = coroutineScope,
    ) {

  override fun initialState(): <Feature>UiState {
    return <Feature>UiState(
        onBackClicked = inputEventCallback(<Feature>Event.BackClicked),
    )
  }

  override suspend fun ResultScope<<Feature>UiState, <Feature>Effect>.process(event: <Feature>Event) {
    when (event) {
      is <Feature>Event.BackClicked -> emitEffect(<Feature>Effect.NavigateBack)
    }
  }
}
```

**`ui/<Feature>Screen.kt`**
```kotlin
package app.pudi.android.<lowercase>.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.pudi.android.<lowercase>.di.<Feature>Scope
import app.pudi.android.ui.AppTypography
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(<Feature>Scope::class)
class <Feature>Screen(
    private val viewModel: <Feature>ViewModel,
) {
  @Composable
  operator fun invoke() {
    val uiState by viewModel.state.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
      Text(
          text = "<Feature Name Spaced>",
          style = AppTypography.displayMedium,
          modifier = Modifier.padding(16.dp),
      )
    }
  }
}

@Preview
@Composable
fun Preview<Feature>Screen() {
  // TODO: Add preview implementation
}
```

**`build.gradle.kts`**
```kotlin
plugins {
  id("com.android.library")
  id("template.feature")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  // Add dependencies as needed
}
```

#### Domain Feature Files (api-impl)

**`api/src/main/java/app/pudi/android/<lowercase>/api/<Feature>Repository.kt`**
```kotlin
package app.pudi.android.<lowercase>.api

interface <Feature>Repository {
  // Define interface methods
}
```

**`impl/src/main/java/app/pudi/android/<lowercase>/impl/<Feature>RepositoryImpl.kt`**
```kotlin
package app.pudi.android.<lowercase>.impl

import app.pudi.android.<lowercase>.api.<Feature>Repository
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class <Feature>RepositoryImpl : <Feature>Repository {
  // Implementation
}
```

**`api/build.gradle.kts`**
```kotlin
plugins {
  id("com.android.library")
  id("template.android")
}

android {
  namespace = libs.versions.app.namespace.get() + ".<lowercase>.api"
}
```

**`impl/build.gradle.kts`**
```kotlin
plugins {
  id("com.android.library")
  id("template.android")
}

android {
  namespace = libs.versions.app.namespace.get() + ".<lowercase>.impl"
}

dependencies {
  implementation(project(":<module-path>:api"))
}
```

### 5. Register Module

Add to `settings.gradle.kts`:

**Single module:**
```kotlin
include(":<location>:<kebab-case>")
```

**api-impl:**
```kotlin
include(":<location>:<kebab-case>:api")
include(":<location>:<kebab-case>:impl")
```

### 6. Build

```bash
./gradlew :<module-path>:assemble
```

This generates Anvil DI bindings.

### 7. Next Steps

**UI Feature:**
1. Navigate to the screen: `navigator.goTo(<Feature>Routes.<Feature>ScreenRoute)`
2. Implement screen UI in `<Feature>Screen.kt`
3. Add events/state to `<Feature>ViewModel.kt`
4. Process events in `<Feature>ViewModelImpl.kt`

**Domain Feature:**
1. Define interface methods in api
2. Implement in impl
3. Inject interface where needed

**All features:**
- Run `make ktfmt` to format
- Run `make tests` to verify no breakage
- Consider creating an exec-plan for complex implementation

## Important Patterns

**Navigation:** All UI features register via `EntryProviderInstaller` in Component Factory. The entry maps route → composable screen.

**DI Scopes:** UI features use custom scope (e.g., `@DiscoverScope`), domain uses `@AppScope`.

**ViewModel Pattern:** Interface extends `Usf<Event, UiState, Effect>`, implementation extends `UsfViewModel` with `@ContributesBinding`.

**CoroutineScope:** Each UI Component provides its own scope for ViewModel lifecycle.

**Lazy Initialization:** Components created lazily on first navigation via `entry<Route> { component.screen.value() }`.

## Reference

Study these for canonical patterns:
- **UI:** `:features:discover` - Complete screen with USF + Navigation 3
- **Domain:** `:domain:quoter` - api-impl split with DI binding

## Common Issues

**"Unresolved reference" after generation:** Run `./gradlew :<module>:assemble` to generate Anvil bindings.

**Navigation doesn't work:** Verify EntryProviderInstaller is in Component Factory with `@Provides @IntoSet`.

**DI fails:** Scope annotations must match across Component, Screen, and ViewModel.

**Module not found:** Ensure `settings.gradle.kts` includes the module correctly.
