package org.kociumba.kutils.client.bazaar

import imgui.ImFont
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiTableFlags
import imgui.type.ImBoolean
import imgui.type.ImDouble
import imgui.type.ImInt
import imgui.type.ImString
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.text.Text
import org.kociumba.kutils.client.c
import org.kociumba.kutils.client.imgui.ImColor
import org.kociumba.kutils.client.imgui.ImGuiKutilsTheme
import org.kociumba.kutils.client.imgui.coloredText
import org.kociumba.kutils.client.imgui.hexToImColor
import org.kociumba.kutils.log
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.screen.ImGuiScreen
import xyz.breadloaf.imguimc.screen.ImGuiWindow
import java.awt.Color
import java.lang.reflect.Field
import kotlin.Double
import java.text.DecimalFormat

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

var products: Map<String, Product> = emptyMap()

/**
 * Main bazaar related features ui
 *
 * The predictions are a little buggy so far but everything works fine
 * search is fucked tho for some reason
 *
 * Spent almost a whole day comparing this 1 to 1 with the go version
 * everything is essentially the same, but the predictions are still fucked.
 * At this point honestly think it's some edge cases with the kotlinx serialization 🤷
 */
@Environment(EnvType.CLIENT)
object bazaarUI: ImGuiScreen(Text.literal("BazaarUI"), true) {
    // default sigmoid for best results
    var smoothingType = ImInt(SmoothingTypes.SIGMOID.ordinal)
    var priceLimit = ImDouble(0.toDouble()) // start as 0 to not show in ui
    var priceLimitIf0 = 1e32
    var weeklySalesLimit = ImDouble(0.toDouble())
    var weeklySalesLimitIf0 = 1e32
    // default 10 results to display
    var displayResults = ImInt(10)
    var searchQuery = ImString("", 256)
    var displayList: MutableList<Product> = mutableListOf()
    var hideEnchantments = ImBoolean(false)

    const val minPrice = 100
    const val minWeeklyVolume = 10

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

    /**
     * main ui render entry point
     */
    override fun initImGui(): List<ImGuiWindow?>? {
        return listOf(
            ImGuiWindow(
                ImGuiKutilsTheme(),
                Text.literal("Bazaar Settings"),
                {
                    ImGui.setWindowPos(50f, 50f, ImGuiCond.Once)
                    ImGui.setWindowSize(400f, 200f, ImGuiCond.Once)

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
                    ImGui.text("Search")
                    ImGui.sameLine()
                    ImGui.inputText("##search", searchQuery)
                    ImGui.text("Hide enchanted books")
                    ImGui.sameLine()
                    ImGui.checkbox("##hideEnchantments", hideEnchantments)
                    ImGui.text("Get and calculate data")
                    ImGui.sameLine()
                    if (ImGui.button("Calculate")) {
                        try {
                            log.info("Getting bazaar data...")
                            var b = BazaarAPI.getBazaar()
                            products = emptyMap()
                            // doing this couse of how this is displayed, might use something else
                            if (priceLimit.get() == 0.0) {
                                priceLimit.set(priceLimitIf0)
                            }
                            if (weeklySalesLimit.get() == 0.0) {
                                weeklySalesLimit.set(weeklySalesLimitIf0)
                            }
//                            b.products.filter { (_, p) ->
//                                productFilter(p)
//                            }.forEach { products[it.key] = it.value }

                            // store for later
                            products = b.products
                            displayList = productFilter(b.products)

                            // reset the display values
                            if (priceLimit.get() == 1e32) {
                                priceLimit.set(0.0)
                            }
                            if (weeklySalesLimit.get() == 1e32) {
                                weeklySalesLimit.set(0.0)
                            }
                            log.info("Got ${products.size} products")
                        } catch (e: Exception) {
                            log.error("Something went wrong while filtering the bazaar data", e)
                        }
                    }
                },
                false,
            ),
            ImGuiWindow(
                ImGuiKutilsTheme(),
                Text.literal("Bazaar Results"),
                {
                    ImGui.setWindowPos(500f, 50f, ImGuiCond.Once)
                    ImGui.setWindowSize(800f, 900f, ImGuiCond.Once)

                    // this is pretty cool xd
                    var numberOfColumns = 7
                    if (!c.showWeeklyTraffic) numberOfColumns--
                    if (!c.showWeeklyAveragePrice) numberOfColumns--

                    ImGui.text("Bazaar data")
                    if (ImGui.beginTable("##bazaarTable", numberOfColumns, ImGuiTableFlags.Borders or ImGuiTableFlags.Resizable)) {
                        ImGui.tableSetupColumn("Product", ImGuiTableFlags.Sortable)
                        ImGui.tableSetupColumn("Sell Price", ImGuiTableFlags.Sortable)
                        ImGui.tableSetupColumn("Buy Price", ImGuiTableFlags.Sortable)
                        ImGui.tableSetupColumn("Difference", ImGuiTableFlags.Sortable)
                        if (c.showWeeklyTraffic) {
                            ImGui.tableSetupColumn("Weekly traffic", ImGuiTableFlags.Sortable)
                        }
                        if (c.showWeeklyAveragePrice) {
                        ImGui.tableSetupColumn("Weekly average price", ImGuiTableFlags.Sortable)
                        }
                        ImGui.tableSetupColumn("Prediction/Confidence", ImGuiTableFlags.Sortable)
                        ImGui.tableHeadersRow()
                        try {
                            // Filter products based on the search query
//                            val filteredProducts = products.filter { (_, p) ->
//                                p.product_id.contains(searchQuery.get(), ignoreCase = true)
//                            }

//                            filteredProducts.forEach { (_, p) ->
//                                renderProductRow(p)
//                            }

                            displayList.forEach { p ->
                                renderProductRow(p)
                            }
                        } catch (e: Exception) {
                            log.error("Something went wrong while trying to display the bazaar data", e)
                        }
                        ImGui.endTable()
                    }
                },
                false,
            )
        )
    }

    // direct port of the go version
    fun getDiff(p: Product): Double {
        return -(p.quick_status.sellPrice - p.quick_status.buyPrice)
    }

    // direct port of the go version
    fun productFilter(p: Map<String, Product>): MutableList<Product> {
        val filtered: MutableList<Product> = mutableListOf()
        for (v in p) {
            if (isProductEligible(v.value)) {
                filtered.addLast(v.value)
            }
        }

        filtered.sortByDescending { getDiff(it) }

        // omit this to show everything in this version
//        if (filtered.size > displayResults.get()) {
//            filtered.subList(displayResults.get(), filtered.size).clear()
//        }

        return filtered
    }

    // direct port of the go version, double-checked it
    fun isProductEligible(p: Product): Boolean {
        if (searchQuery.get() != "") {
            if (!p.product_id.lowercase().contains(searchQuery.get().lowercase())) {
                return false
            }
            return (p.quick_status.buyPrice < priceLimit.get() || priceLimit.get() == 1e32) &&
                    (p.quick_status.sellMovingWeek > weeklySalesLimit.get() || weeklySalesLimit.get() == 1e32)
        }

        return p.quick_status.buyPrice > minPrice &&
                p.quick_status.sellPrice > minPrice &&
                p.quick_status.sellMovingWeek > minWeeklyVolume &&
                p.quick_status.buyMovingWeek > minWeeklyVolume &&
                (p.quick_status.buyPrice < priceLimit.get() || priceLimit.get() == 1e32) &&
                p.quick_status.sellMovingWeek > weeklySalesLimit.get() || weeklySalesLimit.get() == 1e32
    }

    data class Averages(
        var sellAverage : Double,
        var buyAverage : Double
    )

    fun averagePrice(p: Product): Averages {
        var r = Averages(0.0, 0.0)

        p.sell_summary.forEach { s ->
            r.sellAverage += s.pricePerUnit
        }
        r.sellAverage /= p.sell_summary.size

        p.buy_summary.forEach { s ->
            r.buyAverage += s.pricePerUnit
        }
        r.buyAverage /= p.buy_summary.size

        return r
    }

    data class InflatedStatus(
        var sellInflated : Boolean,
        var buyInflated : Boolean
    )

    fun isInflated(p: Product, a: Averages) : InflatedStatus {
        // c.shouldConsiderInflatedPercent is a float that is the percent the current price from quick_status
        // needs to be higher than the average to be considered inflated
        var s = InflatedStatus(false, false)

        s.sellInflated = p.quick_status.sellPrice > a.sellAverage * (1 + c.shouldConsiderInflatedPercent)
        s.buyInflated = p.quick_status.buyPrice > a.buyAverage * (1 + c.shouldConsiderInflatedPercent)

        return s
    }

    private val decimalFormatter = DecimalFormat("#,##0.00")
//    private val inflatedWarning = "⚠ " // renders as "? " couse I can't load custom fonts for now
    private val inflatedWarning = "!!! " // alternate until I make the fork with font loading

    private fun renderProductRow(p: Product) {
        // hide enchantments
        if (hideEnchantments.get() && p.quick_status.productId.lowercase().contains("ENCHANTMENT".lowercase())) return

        ImGui.tableNextRow()
        val avg = averagePrice(p)
        val infl = isInflated(p, avg)
        val warn = hexToImColor("#ff0000")

        // Product ID
        ImGui.tableNextColumn()
        coloredText("#cba6f7", p.product_id)

        // Sell Price
        ImGui.tableNextColumn()
        if (infl.sellInflated) {
            ImGui.pushStyleColor(ImGuiCol.Text, warn.r, warn.g, warn.b, warn.a)
            ImGui.text(inflatedWarning)
            if (ImGui.isItemHovered()) {
                inflatedWarningTooltip()
            }
            ImGui.popStyleColor()
            ImGui.sameLine()
        }
        coloredText("#94e2d5", decimalFormatter.format(p.quick_status.sellPrice))

        // Buy Price
        ImGui.tableNextColumn()
        if (infl.buyInflated) {
            ImGui.pushStyleColor(ImGuiCol.Text, warn.r, warn.g, warn.b, warn.a)
            ImGui.text(inflatedWarning)
            if (ImGui.isItemHovered()) {
                inflatedWarningTooltip()
            }
            ImGui.popStyleColor()
            ImGui.sameLine()
        }
        coloredText("#eba0ac", decimalFormatter.format(p.quick_status.buyPrice))

        // Difference
        ImGui.tableNextColumn()
        val difference = p.quick_status.buyPrice - p.quick_status.sellPrice
        coloredText("#89b4fa", decimalFormatter.format(difference))

        if (c.showWeeklyTraffic) {
            // Weekly traffic
            ImGui.tableNextColumn()
            // these are longs, needs different formatting
            coloredText(
                "#fab387",
                "Sell: ${decimalFormatter.format(p.quick_status.sellMovingWeek)}, " +
                        "Buy: ${decimalFormatter.format(p.quick_status.buyMovingWeek)}"
            )
        }

        if (c.showWeeklyAveragePrice) {
            // Averages
            ImGui.tableNextColumn()
            coloredText(
                "#f9e2af", "Sell: ${decimalFormatter.format(avg.sellAverage)}, " +
                        "Buy: ${decimalFormatter.format(avg.buyAverage)}"
            )
        }

        // Prediction
        ImGui.tableNextColumn()
        val (prediction, confidence) = BazaarMath.getPrediction(p, SmoothingTypes.entries[smoothingType.get()])
        val predictionText = when {
            prediction > 0 -> "+${decimalFormatter.format(prediction)}/${decimalFormatter.format(confidence)}%"
            prediction < 0 -> "${decimalFormatter.format(prediction)}/${decimalFormatter.format(confidence)}%"
            else -> "N/A"
        }
        val predictionColor = when {
            prediction > 0 -> "#a6e3a1"  // Green for positive
            prediction < 0 -> "#f38ba8"  // Red for negative
            else -> "#ffffff"  // White for N/A
        }
        coloredText(predictionColor, predictionText)
    }

    fun inflatedWarningTooltip() {
        ImGui.beginTooltip()
        ImGui.text("Price is inflated by more than ${c.shouldConsiderInflatedPercent * 100}%")
        ImGui.endTooltip()
    }

    fun reset() {
        log.debug("reset called")
//        smoothingType.set(SmoothingTypes.SIGMOID.ordinal)
//        priceLimit.set(1e32)
//        weeklySalesLimit.set(1e32)
//        displayResults.set(10)
        setAlreadyInitialised(false)
    }
}