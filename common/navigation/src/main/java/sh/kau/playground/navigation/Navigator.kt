package sh.kau.playground.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import sh.kau.playground.shared.di.AppScope

typealias EntryProviderInstaller = EntryProviderBuilder<NavRoute>.() -> Unit

@AppScope
@Inject
class Navigator(
    @param:Named("startDestination") private val startDestination: NavRoute,
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

interface NavRoute : NavKey
