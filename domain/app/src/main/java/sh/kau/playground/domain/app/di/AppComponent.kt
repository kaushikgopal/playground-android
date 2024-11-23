package sh.kau.playground.domain.app.di

import android.content.Context
import me.tatarka.inject.annotations.Component
import sh.kau.playground.common.log.di.LogComponent
import sh.kau.playground.common.networking.di.NetworkComponent
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.AppScope
import sh.kau.playground.domain.shared.di.ConfigComponent

@AppScope
@Component
abstract class AppComponent(
    // component inheritance ↓
    @Component val configComponent: ConfigComponent,
    @Component val networkComponent: NetworkComponent,
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
              NetworkComponent.Companion.create(),
              LogComponent.Companion.create(config),
          )

      return instance!!
    }
  }
}
