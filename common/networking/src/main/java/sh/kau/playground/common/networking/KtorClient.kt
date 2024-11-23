package sh.kau.playground.common.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TIME_OUT = 30_000

internal val KtorClient: HttpClient =
    HttpClient(Android) {
      //
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
        logger = Logger.DEFAULT
        level = LogLevel.BODY
      }

      engine {
        connectTimeout = TIME_OUT
        socketTimeout = TIME_OUT
      }
    }
