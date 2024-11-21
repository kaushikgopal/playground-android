package sh.kau.playground.features.settings.di

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.domain.app.di.AppComponent
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.Named

@Component
abstract class SettingsComponent(
    @Component val parent: AppComponent,
) {
  abstract val bindings: SettingsBindings

  companion object {
    fun create(app: App): SettingsComponent {
      val parent = AppComponent.from(app)
      return SettingsComponent::class.create(parent)
    }
  }
}

@Inject
class SettingsBindings(
    @Named("appName") val appName: String,
) {
  val tag = "SettingsScreens"
}
