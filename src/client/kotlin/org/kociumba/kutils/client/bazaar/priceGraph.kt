package org.kociumba.kutils.client.bazaar

import imgui.ImGui
import imgui.ImVec2
import imgui.ImVec4
import imgui.extension.implot.ImPlot
import imgui.extension.implot.flag.ImPlotAxisFlags
import imgui.extension.implot.flag.ImPlotCol
import imgui.extension.implot.flag.ImPlotFlags
import org.kociumba.kutils.client.bazaar.bazaarUI.getRealName
import org.kociumba.kutils.client.c
import org.kociumba.kutils.client.imgui.hexToImColor
import org.kociumba.kutils.client.imgui.spinner
import org.kociumba.kutils.log
import java.time.Instant
import java.time.temporal.ChronoUnit

fun PriceInfo.toImPlotData(days: Long = 7): ImPlotPriceData {
    val cutoffTime = Instant.now().minus(days, ChronoUnit.DAYS)

    val filteredData = prices
        .mapKeys { (timestampStr, _) ->
            Instant.parse(timestampStr)
        }
        .filterKeys { timestamp ->
            timestamp.isAfter(cutoffTime)
        }
        .toSortedMap()

    val times = Array(filteredData.size) { idx ->
        filteredData.keys.elementAt(idx).epochSecond.toDouble()
    }

    val buyPrices = Array(filteredData.size) { idx ->
        filteredData.values.elementAt(idx).b
    }

    val sellPrices = Array(filteredData.size) { idx ->
        filteredData.values.elementAt(idx).s
    }

    return ImPlotPriceData(times, buyPrices, sellPrices)
}

data class ImPlotPriceData(
    val times: Array<Double>,
    val buyPrices: Array<Double>,
    val sellPrices: Array<Double>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImPlotPriceData

        if (!times.contentEquals(other.times)) return false
        if (!buyPrices.contentEquals(other.buyPrices)) return false
        if (!sellPrices.contentEquals(other.sellPrices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = times.contentHashCode()
        result = 31 * result + buyPrices.contentHashCode()
        result = 31 * result + sellPrices.contentHashCode()
        return result
    }
}

private var fetchStates = mutableMapOf<String, Boolean>()
private var loadingStates = mutableMapOf<String, Boolean>()
private var priceDataMap = mutableMapOf<String, ImPlotPriceData?>()
private const val defaultTimeframe = 100L // days
var errorMessage: String? = null

fun priceGraphWindow(p: Product) {
    val productId = p.product_id

    // Initialize fetch state if needed
    if (!fetchStates.containsKey(productId)) {
        fetchStates[productId] = false
        loadingStates[productId] = false
    }


    fun fetchData() {
        fetchStates[productId] = true
        loadingStates[productId] = true
        errorMessage = null
        log.info("Fetching price graph for $productId")

        Thread {
            try {
                val fetchedPriceData = PriceDataFetcher.fetchPriceData(productId)
                if (fetchedPriceData != null) {
                    priceDataMap[productId] = fetchedPriceData.toImPlotData(defaultTimeframe)
                } else {
                    val warnMessage = "No price data received for product ID: $productId."
                    log.warn(warnMessage)
                    errorMessage = warnMessage
                    priceDataMap[productId] = null
                }

            } catch (e: Exception) {
                val errorText = "Error fetching price data for $productId. Check console."
                log.error(errorText, e)
                errorMessage = errorText
                priceDataMap[productId] = null
            } finally {
                loadingStates[productId] = false
            }
        }.start()
    }

    if (fetchStates[productId] == false) {
        fetchData()
    }

    ImGui.text("Price graph for ${getRealName(p)}")
    if (ImGui.button("Refresh data")) {
        fetchData()
    }
    if (loadingStates[productId] == true) {
        ImGui.sameLine()
        val buttonHeight = ImGui.getFrameHeight()
        val radius = buttonHeight / 2 - 1f
        val thickness = buttonHeight / 7f
        spinner("##s", radius, thickness, ImGui.getColorU32(1f, 1f, 1f, 1f))
    }


    if (errorMessage != null) {
        val errorColor = hexToImColor("#f87171")
        ImGui.textColored(errorColor.r, errorColor.g, errorColor.b, errorColor.a, errorMessage)
        return
    }

    if (priceDataMap[productId] == null) {
        ImGui.text("Loading data...")
        return
    }


    priceDataMap[productId]?.let { data ->
        var bg = c.mainWindowBackground
        ImPlot.pushStyleColor(ImPlotCol.FrameBg, ImVec4(bg.red / 255f, bg.green / 255f, bg.blue / 255f, bg.alpha / 255f))
        if (ImPlot.beginPlot(
                getRealName(p),
                "Time",
                "Price",
                ImVec2(800f, 400f),
                ImPlotFlags.AntiAliased or ImPlotFlags.NoChild,
                ImPlotAxisFlags.Time,
                ImPlotAxisFlags.LockMin
            )) {
            // Plot sell prices
            ImPlot.plotLine(
                "Sell Prices",
                data.times.toDoubleArray(),
                data.sellPrices.toDoubleArray(),
                data.sellPrices.size,
                0
            )
            // Plot buy prices
            ImPlot.plotLine(
                "Buy Prices",
                data.times.toDoubleArray(),
                data.buyPrices.toDoubleArray(),
                data.buyPrices.size,
                0
            )
            ImPlot.endPlot()
            ImPlot.popStyleColor()
        }
    }
}