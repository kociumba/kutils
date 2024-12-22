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
private var priceDataMap = mutableMapOf<String, ImPlotPriceData>()
private const val defaultTimeframe = 100L // days

fun priceGraphWindow(p: Product) {
    val productId = p.product_id

    // Initialize fetch state if needed
    if (!fetchStates.containsKey(productId)) {
        fetchStates[productId] = false
    }

    // Start fetch if not already started
    if (fetchStates[productId] == false) {
        fetchStates[productId] = true
        log.info("Fetching price graph for $productId")

        Thread {
            priceDataMap[productId] = PriceDataFetcher.fetchPriceData(productId)?.toImPlotData(defaultTimeframe)!!
        }.start()
    }

    ImGui.text("Price graph for ${getRealName(p)}")

    priceDataMap[productId]?.let { data ->
        var bg = c.mainWindowBackground
        ImPlot.pushStyleColor(ImPlotCol.FrameBg, ImVec4(bg.red / 255f, bg.green / 255f, bg.blue / 255f, bg.alpha / 255f))
        if (ImPlot.beginPlot(
                "Price Graph###$productId",
                "Time",
                "Price",
                ImVec2(800f, 400f),
                ImPlotFlags.AntiAliased or ImPlotFlags.NoChild or ImPlotFlags.Crosshairs or ImPlotFlags.YAxis2,
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

//            ImPlot.showMetricsWindow()

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
    } ?: run {
        ImGui.text("Loading data...")
    }
}