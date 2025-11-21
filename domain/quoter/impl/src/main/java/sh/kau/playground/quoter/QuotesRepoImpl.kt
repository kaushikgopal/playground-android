package sh.kau.playground.quoter

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import sh.kau.playground.networking.NetworkApi
import sh.kau.playground.shared.di.AppScope

@Inject
@AppScope
@ContributesBinding(AppScope::class)
class QuotesRepoImpl(
    private val api: Provider<NetworkApi>,
) : QuotesRepo {

  override suspend fun quoteForTheDay(): Quote {
    return fetchQuote()
  }

  private suspend fun fetchQuote(): Quote {
    val response: HttpResponse =
        api().client().get("https://zenquotes.io/api/today") {
          contentType(ContentType.Application.Json)
        }
    return response.body<List<Quote>>()[0]
  }
}
