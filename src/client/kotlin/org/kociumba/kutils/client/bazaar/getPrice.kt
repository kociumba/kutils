package org.kociumba.kutils.client.bazaar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.kociumba.kutils.log
import java.net.HttpURLConnection
import java.net.URI

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

    fun fetchPriceData(itemId: String): PriceInfo? {
        try {
            val url = URI("https://pricehistory.notenoughupdates.org?item=${itemId}").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode !in 200..299) {
                log.warn("Failed to fetch price data for $itemId, status code: ${connection.responseCode}")
                return null
            }

            // damn, actually works
            connection.inputStream.use { inputStream ->
                val prices = json.decodeFromStream<Map<String, Price>>(inputStream)
                return PriceInfo(prices)
            }
        } catch (e: Exception) {
            log.error("Failed to fetch price data for $itemId", e)
            return null
        }
    }
}