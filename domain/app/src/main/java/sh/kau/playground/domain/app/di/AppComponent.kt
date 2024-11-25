package sh.kau.playground.domain.app.di

import android.content.Context
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.common.log.di.LogComponent
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.ConfigComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
    // component inheritance ↓
    // dependencies from below will now be available to AppComponent
    @Component val configComponent: ConfigComponent,
    @Component val logComponent: LogComponent,
) {

  companion object {
    private var instance: AppComponent? = null

    fun from(context: Context): AppComponent {
      if (instance != null) return instance!!

      val app = context.applicationContext as App

      val config = ConfigComponent.Companion.create(app)
      instance =
          AppComponent::class.create(
              context.applicationContext as App,
              config,
              LogComponent.Companion.create(config),
          )

      return instance!!
    }
  }
}
