package sh.kau.playground.common.log.di

import logcat.LogcatLogger
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.common.log.AndroidLogger
import sh.kau.playground.common.log.AndroidLogger2
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.Named

@Component
abstract class LogComponent(
  val app: App,
) {

  @Provides
  fun provideDebuggableApp(): @Named("debuggableApp") Boolean = app.isDebuggable

  @IntoSet
  @Provides
  protected fun provideAndroidLogger(logger: AndroidLogger): LogcatLogger = logger

  @IntoSet
  @Provides
  protected fun provideAndroidLogger2(logger: AndroidLogger2): LogcatLogger = logger

  abstract val loggers: Set<LogcatLogger> // multi-bindings

  companion object {
    fun create(app: App): LogComponent = LogComponent::class.create(app)
  }
}
