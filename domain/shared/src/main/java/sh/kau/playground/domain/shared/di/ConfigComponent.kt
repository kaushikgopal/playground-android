package sh.kau.playground.domain.shared.di

import me.tatarka.inject.annotations.Provides
import sh.kau.playground.domain.shared.App
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(AppScope::class)
interface ConfigComponent {
  @Provides fun provideAppName(): @Named("appName") String = "My Playground!"

  @Provides fun provideDebuggableApp(app: App): @Named("debuggableApp") Boolean = app.isDebuggable
}
