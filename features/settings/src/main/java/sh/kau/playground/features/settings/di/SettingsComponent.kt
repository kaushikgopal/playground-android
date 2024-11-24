package sh.kau.playground.features.settings.di

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.domain.app.di.AppComponent
import sh.kau.playground.domain.quoter.api.QuotesRepo
import sh.kau.playground.domain.quoter.impl.QuotesRepoImpl
import sh.kau.playground.domain.shared.di.Named
import sh.kau.playground.features.settings.ui.SettingsAScreen
import sh.kau.playground.features.settings.ui.SettingsBScreen

@Component
abstract class SettingsComponent(
    @Component val parent: AppComponent,
) {

  abstract val settingsAScreen: SettingsAScreen

  // kotlin-inject function injection (2)
  abstract val settingsBScreen: SettingsBScreen

  // i would typically shove this in a Component (if there were more deps from the quotes module)
  @Provides fun quotesRepo(quotesRepo: QuotesRepoImpl): QuotesRepo = quotesRepo

  companion object {
    fun create(appComponent: AppComponent): SettingsComponent {
      return SettingsComponent::class.create(appComponent)
    }
  }
}

@Inject
class SettingsBindings(
    @Named("appName") val appName: String,
    val quotesRepo: QuotesRepo,
)
