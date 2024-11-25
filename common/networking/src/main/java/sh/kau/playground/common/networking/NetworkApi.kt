package sh.kau.playground.common.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import logcat.logcat
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * This class abstracts away the implementation details for networking.
 * * It prevents direct dependency over networking libraries (like Ktor) to spread across the app
 */
@SingleIn(AppScope::class)
@Inject
class NetworkApi() {
  private val client: Lazy<HttpClient> = lazy {
    HttpClient(Android) {
      install(ContentNegotiation) {
        json(
            json =
                Json {
                  prettyPrint = true
                  isLenient = true
                  ignoreUnknownKeys = true
                },
        )
      }
      install(Logging) {
        logger =
            object : Logger {
              override fun log(message: String) {
                logcat { "[Ktor] $message" }
              }
            }
        level = LogLevel.ALL
      }
      engine {
        connectTimeout = TIME_OUT
        socketTimeout = TIME_OUT
      }
    }
  }

  fun client(): HttpClient = client.value
}

private const val TIME_OUT: Int = 30_000
