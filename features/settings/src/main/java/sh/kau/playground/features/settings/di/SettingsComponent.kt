package sh.kau.playground.features.settings.di

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.domain.app.di.AppComponent
import sh.kau.playground.domain.shared.di.Named
import sh.kau.playground.features.settings.ui.SettingsAScreen

@Component
abstract class SettingsComponent(
    @Component val parent: AppComponent,
) {

  // kotlin-inject function injection (2)
  abstract val settingsAScreen: SettingsAScreen

  companion object {
    fun create(appComponent: AppComponent): SettingsComponent {
      return SettingsComponent::class.create(appComponent)
    }
  }
}

@Inject
class SettingsBindings(
    @Named("appName") val appName: String,
) {
  val tag = "SettingsScreens"
}
