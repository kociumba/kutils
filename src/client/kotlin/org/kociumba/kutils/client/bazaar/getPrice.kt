package org.kociumba.kutils.client.bazaar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture
import org.kociumba.kutils.log

@Serializable
data class PriceInfo(
    val prices: Map<String, Price>
)

@Serializable
data class Price(
    val b: Double,
    val s: Double
)

/**
 * Once again huge thanks to NotEnoughUpdates for the historical price data,
 * if this should be public then oopsie I guess
 *
 * https://pricehistory.notenoughupdates.org?item=ANY_ITEM_ID
 *
 * Needs better fetching than the others
 */
object PriceDataFetcher {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun fetchPriceData(itemId: String): CompletableFuture<PriceInfo?> {
        return CompletableFuture.supplyAsync {
            try {
                val url = URL("https://pricehistory.notenoughupdates.org/api/price?item=$itemId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode !in 200..299) {
                    return@supplyAsync null
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                return@supplyAsync json.decodeFromString<PriceInfo>(response)
            } catch (e: Exception) {
                log.error("Failed to fetch price data for $itemId", e)
                return@supplyAsync null
            }
        }
    }
}