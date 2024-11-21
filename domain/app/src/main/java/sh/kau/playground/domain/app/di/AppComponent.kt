package sh.kau.playground.domain.app.di

import me.tatarka.inject.annotations.Component
import sh.kau.playground.common.log.di.LogComponent
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.ConfigComponent

@Component
abstract class AppComponent(
    // component inheritance ↓
    @Component val configComponent: ConfigComponent,
    @Component val logComponent: LogComponent,
) {

  companion object {
    private var instance: AppComponent? = null

    fun from(app: App): AppComponent {
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
