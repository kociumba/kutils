package org.kociumba.kutils.client.bazaar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.kociumba.kutils.client.httpClient.Http
import org.kociumba.kutils.log

@Serializable
data class Items(
    val success: Boolean,
    val lastUpdated: Long,
    val items: List<Item>
)

@Serializable
data class Item(
//    val material: String,
//    val durability:
//    val color: String,
    val name: String,
//    val category: String,
//    val tier: String,
//    val stats: ItemStats,
//    val npc_sell_price: Long,
    val id: String
)

//@Serializable
//data class ItemStats(
//    val DEFENSE: Int,
//    val HEALTH: Int
//)

/**
 * utility to easily get hypixel skyblock item data
 */
@Environment(EnvType.CLIENT)
object ItemsAPI {
    fun getItems(): Items {
        var r = Http.getProxy("https://kutils-hypixel-proxy.kociumba.workers.dev/items")
//        log.info("Bazaar response: $r")
        val json = Json {
            ignoreUnknownKeys = true
        }
        var items: Items = json.decodeFromString(r)
        log.info("Bazaar: ${items.items[0]}")
        return items
    }
}