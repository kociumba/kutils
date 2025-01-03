package org.kociumba.kutils.client.bazaar

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImFloat
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.kociumba.kutils.client.imgui.ImGuiKutilsTheme
import org.kociumba.kutils.client.imgui.spinner
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

data class Weight(
    val name: String,
    val weight: ImFloat,
    var isDisabled: Boolean
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
        Weight("Price Spread", priceSpreadWeight, false),
        Weight("Volume Imbalance", volumeImbalanceWeight, true),
        Weight("Order Imbalance", orderImbalanceWeight, false),
        Weight("Moving Week Trend", movingWeekTrendWeight, false),
        Weight("Top Order Book Pressure", topOrderBookPressureWeight, true),
        Weight("Volume Factor", volumeFactorWeight, false),
        Weight("Profit Margin Factor", profitMarginFactorWeight, false)
    )

    val validatedWeights get() = allWeights.map { it.weight.get() }

    private val configPath: Path = Path("config/kutils/prediction-weights.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun render() {
        ImGui.begin(name, ImGuiWindowFlags.NoDocking or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.AlwaysAutoResize)

        // Check if any weight is exactly 1
        val activeWeight = allWeights.firstOrNull { it.weight.get() == 1f }

        // Update the disabled state for all weights
        allWeights.forEach { weight ->
            weight.isDisabled = activeWeight != null && weight.weight.get() != 1f
        }

        allWeights.forEach { (name, weight, isDisabled) ->
            ImGui.beginDisabled(isDisabled)
            ImGui.inputFloat(name, weight)
            ImGui.endDisabled()
        }

        val sum = allWeights.sumOf { it.weight.get().toDouble() }
        val isValid = abs(sum - 1.0) < 0.0001

        ImGui.spacing()

        if (!isValid) {
            ImGui.textColored(1f, 0f, 0f, 1f, "Warning: Normalizing weights to sum to 1.0")
            ImGui.sameLine()
            val textHeight = ImGui.getFrameHeight()
            val radius = textHeight / 2 - 1f
            val thickness = textHeight / 7f
            spinner("##s", radius, thickness, ImGui.getColorU32(1f, 0f, 0f, 1f))
        } else if (activeWeight == null) {
            ImGui.text("Sum of weights: ${"%.4f".format(sum)}")
        } else {
            ImGui.textColored(1.0f, 0.647f, 0.0f, 1.0f, "reset the values to reenable all weights")
        }

        ImGui.spacing()
//        if (ImGui.button("Normalize")) {
//            normalizeWeights()
//            saveWeights()
//        }

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

        if (ImGui.isWindowFocused()) {
            normalizeWeights()
        }

        ImGui.end()
    }

    /**
     * percentage scaling
     */
    private fun normalizeWeights() {
        val total = allWeights.sumOf { it.weight.get().toDouble() }
        if (total <= 0) return

        // For each weight that's set above 1.0, cap it at 1.0
        allWeights.forEach { (_, weight, _) ->
            if (weight.get() > 1f) weight.set(1f)
        }

        // Calculate how much weight is left to distribute
        val primaryWeights = allWeights.map { it.weight.get() }
        val remainingWeight = (1.0 - primaryWeights.sum()).toFloat()

        if (remainingWeight > 0) {
            // Get sum of weights that are less than their input value
            val smallWeightsSum = primaryWeights.filter { it < 1f }.sum()

            if (smallWeightsSum > 0) {
                // Distribute remaining weight proportionally
                allWeights.forEach { (_, weight) ->
                    val currentWeight = weight.get()
                    if (currentWeight < 1f) {
                        val proportion = currentWeight / smallWeightsSum
                        weight.set(currentWeight + (remainingWeight * proportion))
                    }
                }
            } else {
                // If all weights are 1.0, distribute remaining equally
                val equalShare = remainingWeight / allWeights.size
                allWeights.forEach { (_, weight) ->
                    weight.set(weight.get() + equalShare)
                }
            }
        } else if (remainingWeight < 0) {
            // If sum > 1, scale down proportionally everything except 1.0 values
            val scaleDown = (1.0f - primaryWeights.filter { it >= 1f }.sum()) /
                    primaryWeights.filter { it < 1f }.sum()

            allWeights.forEach { (_, weight) ->
                val currentWeight = weight.get()
                if (currentWeight < 1f) {
                    weight.set(currentWeight * scaleDown)
                }
            }
        }
    }

    private fun resetWeights() {
        allWeights.forEach { (_, weight, _) ->
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