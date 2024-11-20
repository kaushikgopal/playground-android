package sh.kau.playground.common.log.di

import me.tatarka.inject.annotations.Provides

//@ContributesTo(AppScope::class)
interface LogComponent {
  @Provides fun provideDummyInt(): Int = 9
}
