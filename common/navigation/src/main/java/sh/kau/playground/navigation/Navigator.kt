package sh.kau.playground.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.shared.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

typealias EntryProviderInstaller = EntryProviderBuilder<NavRoute>.() -> Unit

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

interface NavRoute : NavKey
