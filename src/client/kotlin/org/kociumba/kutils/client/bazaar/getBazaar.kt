package org.kociumba.kutils.client.bazaar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.kociumba.kutils.client.httpClient.Http
import org.kociumba.kutils.log

@Serializable
data class Bazaar(
    val success: Boolean,
    val lastUpdated: Long,
    val products: Map<String, Product>
)

@Serializable
data class Product(
    val product_id: String,
    val sell_summary: List<SellInfo>,
    val buy_summary: List<BuyInfo>,
    val quick_status: QuickStatus,
)

@Serializable
data class SellInfo(
    val amount: Int,
    val pricePerUnit: Double,
    val orders: Int
)

@Serializable
data class BuyInfo(
    val amount: Int,
    val pricePerUnit: Double,
    val orders: Int
)

// some of these are failing to unmarshall
@Serializable
data class QuickStatus(
    val productId: String,
    val sellPrice: Double,
    val sellVolume: Int,
    val sellMovingWeek: Long,
    val sellOrders: Int,
    val buyPrice: Double,
    val buyVolume: Int,
    val buyMovingWeek: Long,
    val buyOrders: Int
)

/**
 * utility to easily get hypixel data
 */
@Environment(EnvType.CLIENT)
object BazaarAPI {
    fun getBazaar(): Bazaar {
        var r = Http.getProxy("https://kutils-hypixel-proxy.kociumba.workers.dev/bazaar")
//        log.info("Bazaar response: $r")
        var bazaar: Bazaar = Json.decodeFromString(r)
        log.info("Bazaar: ${bazaar.products["FLAWLESS_AMETHYST_GEM"]}")
        return bazaar
    }
}