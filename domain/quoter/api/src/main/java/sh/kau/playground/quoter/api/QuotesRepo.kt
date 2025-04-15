package sh.kau.playground.quoter.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface QuotesRepo {
  suspend fun quoteForTheDay(): Quote
}

@Serializable
data class Quote(
    @SerialName("q") val quote: String,
    @SerialName("a") val author: String,
)
