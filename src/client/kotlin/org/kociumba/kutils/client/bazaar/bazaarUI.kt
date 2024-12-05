package org.kociumba.kutils.client.bazaar

import imgui.ImGui
import imgui.flag.ImGuiTableFlags
import imgui.type.ImDouble
import imgui.type.ImInt
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.kociumba.kutils.client.imgui.ImGuiKutilsTheme
import org.kociumba.kutils.log
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.screen.ImGuiScreen
import xyz.breadloaf.imguimc.screen.ImGuiWindow
import java.lang.reflect.Field

/**
 * Types of smoothing functions that can be applied to the data
 */
enum class SmoothingTypes(val displayName: String) {
    SIGMOID("Sigmoid"),
    TANH("Tanh"),
    SATURATING("Saturating"),
    PIECEWISE("Piecewise"),
    NONE("None"),
}

var products = mutableMapOf<String, Product>()

/**
 * Main bazaar related features ui
 */
object bazaarUI: ImGuiScreen(Text.literal("BazaarUI"), true) {
    // default sigmoid for best results
    var smoothingType = ImInt(SmoothingTypes.SIGMOID.ordinal)
    var priceLimit = ImDouble(1e32)
    var weeklySalesLimit = ImDouble(1e32)
    // default 10 results to display
    var displayResults = ImInt(10)

    private lateinit var alreadyInitialisedField: Field

    /**
     * Hacky reflection shenanigans, but we need it to not duplicate imgui windows in memory
     */
    init {
        try {
            alreadyInitialisedField = ImGuiScreen::class.java.getDeclaredField("alreadyInitialised")
            alreadyInitialisedField.isAccessible = true
        } catch (e: Exception) {
            log.error("Failed to get alreadyInitialised field", e)
        }
    }

    private fun setAlreadyInitialised(value: Boolean) {
        try {
            alreadyInitialisedField.setBoolean(this, value)
        } catch (e: Exception) {
            log.error("Failed to set alreadyInitialised", e)
        }
    }

    fun getPred(p: Map.Entry<String, Product>): BazaarMath.PredictionResult {
        return BazaarMath.getPrediction(p.value, SmoothingTypes.entries[smoothingType.get()])
    }

    /**
     * TODO: refactor this to not have like 50 nested scopes
     */
    override fun initImGui(): List<ImGuiWindow?>? {
        log.info("initImGui called")
        return listOf(
            ImGuiWindow(
                ImGuiKutilsTheme(),
                Text.literal("Bazaar Settings"),
                {
                    ImGui.text("Smoothing Type")
                    ImGui.sameLine()
                    ImGui.combo(
                        "##smoothingType",
                        smoothingType,
                        SmoothingTypes.entries.map { it.displayName }.toTypedArray()
                    )
                    ImGui.text("Price Limit")
                    ImGui.sameLine()
                    ImGui.inputDouble("##priceLimit", priceLimit)
                    ImGui.text("Weekly Sales Limit")
                    ImGui.sameLine()
                    ImGui.inputDouble("##weeklySalesLimit", weeklySalesLimit)
                    ImGui.text("Get and calculate data")
                    ImGui.sameLine()
                    if (ImGui.button("Calculate")) {
                        try {
                            log.info("Getting bazaar data...")
                            var b = BazaarAPI.getBazaar()
                            products.clear()
                            b.products.filter { (_, p) ->
                                val minPrice = 1.0 // Adjust this value as needed
                                val minWeeklyVolume = 1.0 // Adjust this value as needed
                                p.quick_status.buyPrice > minPrice &&
                                        p.quick_status.sellPrice > minPrice &&
                                        p.quick_status.sellMovingWeek > minWeeklyVolume &&
                                        p.quick_status.buyMovingWeek > minWeeklyVolume &&
                                        (p.quick_status.buyPrice < priceLimit.get() || priceLimit.get() == 1e32) &&
                                        (p.quick_status.sellMovingWeek.toDouble() > weeklySalesLimit.get() || weeklySalesLimit.get() == 1e32)
                            }.forEach { products[it.key] = it.value }
                            log.info("Got ${products.size} products")
                        } catch (e: Exception) {
                            log.error("Something went wrong while filtering the bazaar data", e)
                        }
                    }
                },
                true
            ),
            ImGuiWindow(
                ImGuiKutilsTheme(),
                Text.literal("Bazaar Results"),
                {
                    ImGui.text("Bazaar data")
                    if (ImGui.beginTable("##bazaarTable", 6, ImGuiTableFlags.Borders)) {
                        ImGui.tableSetupColumn("Product")
                        ImGui.tableSetupColumn("Sell Price")
                        ImGui.tableSetupColumn("Buy Price")
                        ImGui.tableSetupColumn("Difference")
                        ImGui.tableSetupColumn("Weekly traffic")
                        ImGui.tableSetupColumn("Prediction/Confidence")
                        ImGui.tableHeadersRow()
                        try {
                            products.forEach { (_, p) ->
                                ImGui.tableNextRow()
                                ImGui.tableNextColumn()
                                ImGui.text(p.product_id)
                                ImGui.tableNextColumn()
                                ImGui.text(p.quick_status.sellPrice.toString())
                                ImGui.tableNextColumn()
                                ImGui.text(p.quick_status.buyPrice.toString())
                                ImGui.tableNextColumn()
                                ImGui.text((p.quick_status.buyPrice - p.quick_status.sellPrice).toString())
                                ImGui.tableNextColumn()
                                ImGui.text("Sell: ${p.quick_status.sellMovingWeek}, Buy: ${p.quick_status.buyMovingWeek}")
                                ImGui.tableNextColumn()
                                ImGui.text(
                                    run {
                                        val (prediction, confidence) = getPred(mapOf("key" to p).entries.first())
                                        when {
                                            prediction > 0 -> "+ %.2f/%.2f%%".format(prediction, confidence)
                                            prediction < 0 -> "- %.2f/%.2f%%".format(prediction, confidence)
                                            else -> "N/A"
                                        }
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            log.error("Something went wrong while trying to display the bazaar data", e)
                        }
                        ImGui.endTable()
                    }
                },
                true,
            )
        )
    }

    fun reset() {
        log.info("reset called")
//        smoothingType.set(SmoothingTypes.SIGMOID.ordinal)
//        priceLimit.set(1e32)
//        weeklySalesLimit.set(1e32)
//        displayResults.set(10)
        setAlreadyInitialised(false)
    }
}