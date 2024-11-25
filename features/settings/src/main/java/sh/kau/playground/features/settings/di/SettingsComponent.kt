package sh.kau.playground.features.settings.di

import me.tatarka.inject.annotations.Inject
import sh.kau.playground.domain.quoter.api.QuotesRepo
import sh.kau.playground.domain.shared.di.Named
import sh.kau.playground.features.settings.ui.SettingsAScreen
import sh.kau.playground.features.settings.ui.SettingsBScreen
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(SettingsScope::class)
@SingleIn(SettingsScope::class)
interface SettingsComponent {

  val settingsAScreen: SettingsAScreen

  // kotlin-inject function injection (2)
  val settingsBScreen: SettingsBScreen

  //  // i would typically shove this in a Component (if there were more deps from the quotes
  // module)
  //  // TODO: remove direct dependency on QuotesRepoImpl here
  //  //    making QuotesRepoImpl directly injectable (via kotlin-inject-anvil)
  //  //    will prevent us from now needing feature:settings → :common:networking
  //    @Provides fun quotesRepo(quotesRepo: QuotesRepoImpl): QuotesRepo = quotesRepo

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createSettingsComponent(): SettingsComponent
  }

  //  companion object {
  //    fun create(appComponent: AppComponent): SettingsComponent {
  //      return SettingsComponent::class.create(appComponent)
  //    }
  //  }
}

@Inject
class SettingsBindings(
    @Named("appName") val appName: String,
    val quotesRepo: QuotesRepo,
)
