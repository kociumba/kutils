package org.kociumba.kutils.client.bazaar

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.implot.ImPlot
import imgui.flag.ImGuiWindowFlags
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.kociumba.kutils.log

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

private var fetchStarted = false
private var priceData: ImPlotPriceData? = null
private const val defaultTimeframe = 7L // days

fun priceGraphWindow(p: Product) {
    if (!fetchStarted) {
        fetchStarted = true
        log.info("Fetching price graph for ${p.product_id}")

        PriceDataFetcher.fetchPriceData(p.product_id).thenAccept { result ->
            result?.let {
                priceData = it.toImPlotData(defaultTimeframe)
            }
        }
    }

    ImGui.begin("Price Graph", ImGuiWindowFlags.AlwaysAutoResize)
    ImGui.text("Price graph for ${p.product_id}")

    priceData?.let { data ->
        if (ImPlot.beginPlot("Price Graph", "Time", "Price", ImVec2(600f, 300f))) {
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
        }
    } ?: run {
        ImGui.text("Loading data...")
    }

    ImGui.end()
}