package sh.kau.playground.di

import android.content.Context
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.App
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn


// Contribution
@ContributesTo(AppScope::class)
interface NameComponent {
    @Provides
    fun provideAppName(): String = "My Playground!"
}

// Merging
@Component
@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
) : AppComponentMerged {

    companion object {
        private var instance: AppComponent? = null

        fun from(context: Context): AppComponent {
            return instance
                ?: AppComponent::class.create(context.applicationContext as App).also { instance = it }
        }
    }
}
