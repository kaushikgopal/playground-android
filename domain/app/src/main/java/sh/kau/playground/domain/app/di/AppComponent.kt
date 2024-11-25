package sh.kau.playground.domain.app.di

import android.content.Context
import me.tatarka.inject.annotations.Component
import sh.kau.playground.common.log.di.LogComponent
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.ConfigComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(
    // component inheritance ↓
    // dependencies from below will now be available to AppComponent
    @Component val configComponent: ConfigComponent,
    @Component val logComponent: LogComponent,
) {

  companion object {
    private var instance: AppComponent? = null

    fun from(context: Context): AppComponent {
      val app = context.applicationContext as App

      if (instance != null) return instance!!

      val config = ConfigComponent.Companion.create(app)
      instance =
          AppComponent::class.create(
              config,
              LogComponent.Companion.create(config),
          )

      return instance!!
    }
  }
}
