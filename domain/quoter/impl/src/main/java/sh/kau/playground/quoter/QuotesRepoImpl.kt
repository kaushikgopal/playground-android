package sh.kau.playground.quoter

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.networking.NetworkApi
import sh.kau.playground.quoter.Quote
import sh.kau.playground.quoter.QuotesRepo
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class QuotesRepoImpl(
    private val api: Lazy<NetworkApi>,
) : QuotesRepo {

  override suspend fun quoteForTheDay(): Quote {
    return fetchQuote()
  }

  private suspend fun fetchQuote(): Quote {
    val response: HttpResponse =
        api.value.client().get("https://zenquotes.io/api/today") {
          contentType(ContentType.Application.Json)
        }
    return response.body<List<Quote>>()[0]
  }
}
