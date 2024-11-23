package sh.kau.playground.domain.shared.di

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.domain.shared.App

@AppScope
@Component
abstract class ConfigComponent(
    private val app: App,
) {

  @Provides fun provideAppName(): @Named("appName") String = "My Playground!"

  @Provides fun isDebuggable(): @Named("debuggableApp") Boolean = app.isDebuggable

  companion object {
    fun create(app: App): ConfigComponent = ConfigComponent::class.create(app)
  }
}
