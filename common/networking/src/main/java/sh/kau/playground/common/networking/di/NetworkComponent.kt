package sh.kau.playground.common.networking.di

import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.common.networking.KtorClient

@Component
abstract class NetworkComponent {
  @Provides fun httpClient(): HttpClient = KtorClient

  companion object {
    fun create() = NetworkComponent::class.create()
  }
}
