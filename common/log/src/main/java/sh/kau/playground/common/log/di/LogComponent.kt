package sh.kau.playground.common.log.di

import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(AppScope::class)
interface LogComponent {
  @Provides fun provideDummyInt(): Int = 9
}
