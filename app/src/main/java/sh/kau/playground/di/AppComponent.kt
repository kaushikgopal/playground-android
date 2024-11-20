package sh.kau.playground.di

import android.content.Context
import android.content.pm.ApplicationInfo
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.App
import sh.kau.playground.domain.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

// Contribution
@ContributesTo(AppScope::class)
interface AppConfigComponent {

  @Provides fun provideAppName(): String = "My Playground!"

  @Provides
  fun provideDebugApp(app: App): @Named("debugApp") Boolean =
      (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

// Merging
@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
) : KotlinInjectAppComponentMerged {

  abstract val loggers: Set<LogcatLogger>

  companion object {
    private var instance: AppComponent? = null

    fun from(context: Context): AppComponent {
      return instance
          ?: AppComponent::class.create(context.applicationContext as App).also { instance = it }
    }
  }
}
