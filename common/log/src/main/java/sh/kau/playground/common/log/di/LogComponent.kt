package sh.kau.playground.common.log.di

import logcat.LogcatLogger
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(AppScope::class)
interface LogComponent {
  //
  //  @IntoSet
  //  @Provides
  //  fun provideAndroidLogger(logger: AndroidLogger): LogcatLogger = logger
  //
  //  @IntoSet
  //  @Provides
  //  fun provideAndroidLogger2(logger: AndroidLogger2): LogcatLogger = logger

//  val loggers: Set<LogcatLogger> // multi-bindings

  //  companion object {
  //    fun create(config: ConfigComponent): LogComponent = LogComponent::class.create(config)
  //  }
}
