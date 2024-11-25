package sh.kau.playground.domain.app.di

import android.content.Context
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.common.log.di.LogComponent
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
    // component inheritance ↓
    // dependencies from below will now be available to AppComponent
    @Component val logComponent: LogComponent,
) {

    @Provides
    fun provideAppName(): @Named("appName") String = "My Playground!"

    @Provides
    fun provideDebuggableApp(): @Named("debuggableApp") Boolean = app.isDebuggable

  companion object {

    private var instance: AppComponent? = null

    fun from(context: Context): AppComponent {
      if (instance != null) return instance!!

      instance =
          AppComponent::class.create(
              context.applicationContext as App,
              LogComponent.Companion.create(context.applicationContext as App),
          )

      return instance!!
    }
  }
}
