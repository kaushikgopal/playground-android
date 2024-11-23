package sh.kau.playground.domain.quoter.api

interface QuotesRepo {
  fun quoteForTheDay(): Quote
}

data class Quote(
    val quote: String,
    val author: String,
)
