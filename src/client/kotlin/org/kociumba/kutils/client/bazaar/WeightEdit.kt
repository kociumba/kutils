package org.kociumba.kutils.client.bazaar

import imgui.ImGui
import imgui.type.ImFloat
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.kociumba.kutils.client.imgui.ImGuiKutilsTheme
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.math.abs
import org.kociumba.kutils.log

@Serializable
private data class WeightsData(
    val priceSpread: Float = 0.1428571429f,
    val volumeImbalance: Float = 0.1428571429f,
    val orderImbalance: Float = 0.1428571429f,
    val movingWeekTrend: Float = 0.1428571429f,
    val topOrderBookPressure: Float = 0.1428571429f,
    val volumeFactor: Float = 0.1428571429f,
    val profitMarginFactor: Float = 0.1428571429f
)

object WeightEdit : Renderable {
    override fun getName(): String? {
        return "WeightEditor"
    }

    override fun getTheme(): Theme? {
        return ImGuiKutilsTheme()
    }

    var rendered = false

    // Weight ImFloat objects
    var priceSpreadWeight = ImFloat(0.1428571429f)
    var volumeImbalanceWeight = ImFloat(0.1428571429f)
    var orderImbalanceWeight = ImFloat(0.1428571429f)
    var movingWeekTrendWeight = ImFloat(0.1428571429f)
    var topOrderBookPressureWeight = ImFloat(0.1428571429f)
    var volumeFactorWeight = ImFloat(0.1428571429f)
    var profitMarginFactorWeight = ImFloat(0.1428571429f)

    private val allWeights = listOf(
        "Price Spread" to priceSpreadWeight,
        "Volume Imbalance" to volumeImbalanceWeight,
        "Order Imbalance" to orderImbalanceWeight,
        "Moving Week Trend" to movingWeekTrendWeight,
        "Top Order Book Pressure" to topOrderBookPressureWeight,
        "Volume Factor" to volumeFactorWeight,
        "Profit Margin Factor" to profitMarginFactorWeight
    )

    val validatedWeights get() = allWeights.map { it.second.get() }

    private val configPath: Path = Path("config/kutils/prediction-weights.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun render() {
        ImGui.begin(name)

        allWeights.forEach { (name, weight) ->
            ImGui.inputFloat(name, weight)
        }

        val sum = allWeights.sumOf { it.second.get().toDouble() }
        val isValid = abs(sum - 1.0) < 0.0001

        ImGui.spacing()
        ImGui.text("Sum of weights: ${"%.4f".format(sum)}")

        if (!isValid) {
            ImGui.textColored(1f, 0f, 0f, 1f, "Warning: Weights must sum to 1.0")
        }

        ImGui.spacing()
        if (ImGui.button("Normalize")) {
            normalizeWeights()
            saveWeights()
        }

        ImGui.sameLine()
        if (ImGui.button("Reset")) {
            resetWeights()
            saveWeights()
        }

        ImGui.sameLine()
        if (ImGui.button("Save values")) {
            normalizeWeights()
            saveWeights()
            Imguimc.pullRenderableAfterRender(this)
            rendered = false
        }

        ImGui.end()
    }

    private fun normalizeWeights() {
        val sum = allWeights.sumOf { it.second.get().toDouble() }
        if (sum > 0) {
            allWeights.forEach { (_, weight) ->
                weight.set((weight.get() / sum).toFloat())
            }
        }
    }

    private fun resetWeights() {
        allWeights.forEach { (_, weight) ->
            weight.set(0.1428571429f)
        }
    }

    fun saveWeights() {
        try {
            val data = WeightsData(
                priceSpread = priceSpreadWeight.get(),
                volumeImbalance = volumeImbalanceWeight.get(),
                orderImbalance = orderImbalanceWeight.get(),
                movingWeekTrend = movingWeekTrendWeight.get(),
                topOrderBookPressure = topOrderBookPressureWeight.get(),
                volumeFactor = volumeFactorWeight.get(),
                profitMarginFactor = profitMarginFactorWeight.get()
            )

            configPath.parent?.createDirectories()
            configPath.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            log.error("Failed to save weights: ${e.message}")
        }
    }

    fun loadWeights() {
        try {
            if (configPath.exists()) {
                val data = json.decodeFromString<WeightsData>(configPath.readText())
                priceSpreadWeight.set(data.priceSpread)
                volumeImbalanceWeight.set(data.volumeImbalance)
                orderImbalanceWeight.set(data.orderImbalance)
                movingWeekTrendWeight.set(data.movingWeekTrend)
                topOrderBookPressureWeight.set(data.topOrderBookPressure)
                volumeFactorWeight.set(data.volumeFactor)
                profitMarginFactorWeight.set(data.profitMarginFactor)
            }
        } catch (e: Exception) {
            log.error("Failed to load weights: ${e.message}")
            resetWeights() // Fallback to default values
        }
    }
}