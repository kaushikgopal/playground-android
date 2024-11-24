package sh.kau.playground.domain.quoter.impl

import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.common.networking.di.NetworkScope
import sh.kau.playground.domain.quoter.api.Quote
import sh.kau.playground.domain.quoter.api.QuotesRepo

@NetworkScope
@Inject
class QuotesRepoImpl(
    client: HttpClient,
) : QuotesRepo {

  override fun quoteForTheDay(): Quote {
    val quote =
        listOf(
                "Mistakes are always forgivable, if one has the courage to admit them:",
                "The key to immortality is first living a life worth remembering",
                "If you love life, don't waste time, for time is what life is made up of",
                "To hell with circumstances; I create opportunities",
                "A quick temper will make a fool of you soon enough",
                "Take no thought of who is right or wrong or who is better than. Be not for or against",
            )
            .random()

    return Quote(quote, "Bruce Lee")
  }
}
