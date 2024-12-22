package org.kociumba.kutils.client.bazaar

import imgui.ImGui
import imgui.extension.implot.ImPlot
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiTableFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import imgui.type.ImDouble
import imgui.type.ImInt
import imgui.type.ImString
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.text.Text
import net.minecraft.util.Util
import org.kociumba.kutils.client.c
import org.kociumba.kutils.client.imgui.*
import org.kociumba.kutils.log
import xyz.breadloaf.imguimc.screen.ImGuiScreen
import xyz.breadloaf.imguimc.screen.ImGuiWindow
import java.lang.reflect.Field
import java.net.URI
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
var items: Map<String, Item> = emptyMap()

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
object bazaarUI : ImGuiScreen(Text.literal("BazaarUI"), true) {
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
    var hideEnchantments = ImBoolean(true)
    var displayInternalNames = ImBoolean(false)
    var error: BazaarRenderError? = null

    const val minPrice = 100
    const val minWeeklyVolume = 10

    private lateinit var alreadyInitialisedField: Field

    data class BazaarRenderError(
        var e: Exception,
        var text: String
    )

    /**
     * Hacky reflection shenanigans, but we need it to not duplicate imgui windows in memory
     */
    init {
        ImPlot.createContext()
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

    fun genericTooltip(text: String) {
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip()
            ImGui.text(text)
            ImGui.endTooltip()
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
                    genericTooltip("the smoothing type used by the predictions")
                    ImGui.sameLine()
                    ImGui.combo(
                        "##smoothingType",
                        smoothingType,
                        SmoothingTypes.entries.map { it.displayName }.toTypedArray()
                    )
                    ImGui.text("Price Limit")
                    genericTooltip("the price limit of displayed items")
                    ImGui.sameLine()
                    ImGui.inputDouble("##priceLimit", priceLimit)
                    ImGui.text("Weekly Sales Limit")
                    genericTooltip("the weekly sales limit of displayed items")
                    ImGui.sameLine()
                    ImGui.inputDouble("##weeklySalesLimit", weeklySalesLimit)
                    ImGui.text("Search")
                    genericTooltip("the search query for the bazaar")
                    ImGui.sameLine()
                    ImGui.inputText("##search", searchQuery)
                    ImGui.text("Hide enchanted books")
                    genericTooltip("hide enchanted books from being shown")
                    ImGui.sameLine()
                    ImGui.checkbox("##hideEnchantments", hideEnchantments)
                    ImGui.text("Display internal names")
                    genericTooltip("display the internal names of the items, used by hypixel")
                    ImGui.sameLine()
                    ImGui.checkbox("##displayInternalNames", displayInternalNames)
                    ImGui.text("Get and calculate data")
                    genericTooltip("pull the newest bazaar data and calculate the predictions")
                    ImGui.sameLine()
                    if (ImGui.button("Calculate")) {
                        try {
                            log.info("Getting bazaar data...")
//                            throw Exception("test error")
                            var b = BazaarAPI.getBazaar()
                            var i = ItemsAPI.getItems()
                            products = emptyMap()
                            // doing this couse of how this is displayed, might use something else

//                            b.products.filter { (_, p) ->
//                                productFilter(p)
//                            }.forEach { products[it.key] = it.value }

                            // store for later
                            products = b.products

                            items = i.items.associateBy { item -> item.id }

//                            log.info(items)

                            // reset the display values

                            log.info("Got ${products.size} products")
                        } catch (e: Exception) {
                            log.error("Something went wrong while filtering the bazaar data", e)
                            error = BazaarRenderError(e, "Something went wrong while filtering the bazaar data")
                        }
                    }
                    renderPriceGraphs()
                },
                false,
            ),
            ImGuiWindow(
                ImGuiKutilsTheme(),
                Text.literal("Bazaar Results"),
                {
                    ImGui.setWindowPos(500f, 50f, ImGuiCond.Once)
                    ImGui.setWindowSize(800f, 900f, ImGuiCond.Once)

                    error?.let { errorPopup(it) }

                    // this is pretty funny
                    var numberOfColumns = 7
                    if (!c.showWeeklyTraffic) numberOfColumns--
                    if (!c.showWeeklyAveragePrice) numberOfColumns--

                    ImGui.text("Bazaar data")
                    if (ImGui.beginTable(
                            "##bazaarTable",
                            numberOfColumns,
                            ImGuiTableFlags.Borders or ImGuiTableFlags.Resizable
                        )
                    ) {
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
                            if (priceLimit.get() == 0.0) {
                                priceLimit.set(priceLimitIf0)
                            }
                            if (weeklySalesLimit.get() == 0.0) {
                                weeklySalesLimit.set(weeklySalesLimitIf0)
                            }

                            displayList = productFilter(products) // will this lag too much ?
                            // it's not, barely affects anything

                            if (priceLimit.get() == 1e32) {
                                priceLimit.set(0.0)
                            }
                            if (weeklySalesLimit.get() == 1e32) {
                                weeklySalesLimit.set(0.0)
                            }

                            displayList.forEach { p ->
                                renderProductRow(p)
                            }
                        } catch (e: Exception) {
                            log.error("Something went wrong while trying to display the bazaar data", e)
                            error = BazaarRenderError(e, "Something went wrong while trying to display the bazaar data")
                        }
                        ImGui.endTable()
                    }
                },
                false,
            )
        )
    }

    /**
     * Wow, actual error handling with the user in mind, incredible xd
     */
    fun errorPopup(e: BazaarRenderError) {
        val warn = hexToImColor("#f5c6c6")
        val link = hexToImColor("#8200ff")

        ImGui.pushStyleColor(ImGuiCol.PopupBg, warn.r, warn.g, warn.b, warn.a)
        ImGui.pushStyleColor(ImGuiCol.Text, 0f, 0f, 0f, 1f)
        ImGui.openPopup("##errorPopup")
        if (ImGui.beginPopup("##errorPopup")) {
            ImGui.text("${e.text}\n  ${e.e}\n\nPlease report this here:")
//            ImGui.sameLine()
            ImGui.textColored(
                link.r,
                link.g,
                link.b,
                link.a,
                "https://github.com/kociumba/kutils/issues (clickable link)"
            )
            if (ImGui.isItemClicked()) {
                val url = URI("https://github.com/kociumba/kutils/issues")
                try {
                    Util.getOperatingSystem().open(url) // minecraft does some weird stuff
                } catch (e: Exception) {
                    log.error("Failed to open the github issues page $e")
                }
            }

            ImGui.pushStyleColor(ImGuiCol.Text, 1f, 1f, 1f, 1f)
            if (ImGui.button("Close")) {
                error = null // reset
                ImGui.closeCurrentPopup()
            }
            ImGui.popStyleColor()
            ImGui.endPopup()
        }
        ImGui.popStyleColor()
        ImGui.popStyleColor()
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
            if (!p.product_id.lowercase().contains(searchQuery.get().lowercase().trim { ch -> ch == ' ' })) {
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
        var sellAverage: Double,
        var buyAverage: Double
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
        var sellInflated: Boolean,
        var buyInflated: Boolean
    )

    /**
     * get real item name or product id if the name is not found
     */
    fun getRealName(p: Product): String {
        return items[p.product_id]?.name ?: p.product_id.lowercase()
    }

    fun isInflated(p: Product, a: Averages): InflatedStatus {
        // c.shouldConsiderInflatedPercent is a float that is the percent the current price from quick_status
        // needs to be higher than the average to be considered inflated
        var s = InflatedStatus(false, false)

        s.sellInflated = p.quick_status.sellPrice > a.sellAverage * (1 + c.shouldConsiderInflatedPercent)
        s.buyInflated = p.quick_status.buyPrice > a.buyAverage * (1 + c.shouldConsiderInflatedPercent)

        return s
    }

    private val decimalFormatter = DecimalFormat("#,##0.00")

    //    private val inflatedWarning = "⚠ " // renders as "? " couse I can't load custom fonts for now
    private val inflatedWarning = "!!!" // alternate until I make the fork with font loading

    data class ShowPriceGraphData(
        var show: ImBoolean,
        var product: Product
    )

    private var showPriceGraphList = mutableListOf<ShowPriceGraphData>()

    // updated to use the color from the config
    private fun renderProductRow(p: Product) {
        // hide enchantments
        if (hideEnchantments.get() && p.quick_status.productId.lowercase().contains("ENCHANTMENT".lowercase())) return

        ImGui.tableNextRow()
        val avg = averagePrice(p)
        val infl = isInflated(p, avg)
//        val warn = hexToImColor("#ff0000")
        val warn = colorToImColor(c.inflatedItemWarningColor)

        // Product ID
        // found the undocumented names xd
        ImGui.tableNextColumn()
        var name: String = if (displayInternalNames.get()) p.product_id else getRealName(p)
//        coloredText("#cba6f7", name)
        coloredText(colorToHex(c.productIDColor), name)
        if (ImGui.isItemClicked()) {
            // Only add if not already in the list
            if (showPriceGraphList.none { it.product.product_id == p.product_id }) {
                showPriceGraphList.add(ShowPriceGraphData(ImBoolean(true), p))
            }
        }

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
//        coloredText("#94e2d5", decimalFormatter.format(p.quick_status.sellPrice))
        coloredText(colorToHex(c.sellPriceColor), decimalFormatter.format(p.quick_status.sellPrice))

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
//        coloredText("#eba0ac", decimalFormatter.format(p.quick_status.buyPrice))
        coloredText(colorToHex(c.buyPriceColor), decimalFormatter.format(p.quick_status.buyPrice))

        // Difference
        ImGui.tableNextColumn()
        val difference = p.quick_status.buyPrice - p.quick_status.sellPrice
//        coloredText("#89b4fa", decimalFormatter.format(difference))
        coloredText(colorToHex(c.differenceColor), decimalFormatter.format(difference))

        if (c.showWeeklyTraffic) {
            // Weekly traffic
            ImGui.tableNextColumn()
            // these are longs, needs different formatting
//            coloredText(
//                "#fab387",
//                "Sell: ${decimalFormatter.format(p.quick_status.sellMovingWeek)}, " +
//                        "Buy: ${decimalFormatter.format(p.quick_status.buyMovingWeek)}"
//            )
            coloredText(
                colorToHex(c.weeklyTrafficColor),
                "Sell: ${decimalFormatter.format(p.quick_status.sellMovingWeek)}, " +
                        "Buy: ${decimalFormatter.format(p.quick_status.buyMovingWeek)}"
            )
        }

        if (c.showWeeklyAveragePrice) {
            // Averages
            ImGui.tableNextColumn()
//            coloredText(
//                "#f9e2af", "Sell: ${decimalFormatter.format(avg.sellAverage)}, " +
//                        "Buy: ${decimalFormatter.format(avg.buyAverage)}"
//            )
            coloredText(
                colorToHex(c.averagesColor),
                "Sell: ${decimalFormatter.format(avg.sellAverage)}, " +
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
//            prediction > 0 -> "#a6e3a1"  // Green for positive
            prediction > 0 -> colorToHex(c.positivePredictionColor)
//            prediction < 0 -> "#f38ba8"  // Red for negative
            prediction < 0 -> colorToHex(c.negativePredictionColor)
            else -> "#ffffff"  // White for N/A
        }
        coloredText(predictionColor, predictionText)
    }

    fun inflatedWarningTooltip() {
        ImGui.beginTooltip()
        ImGui.text("Price is inflated by more than ${c.shouldConsiderInflatedPercent * 100}%")
        ImGui.endTooltip()
    }

    private fun renderPriceGraphs() {
        // Create a copy of the list to avoid concurrent modification
        val toRemove = mutableListOf<ShowPriceGraphData>()

        showPriceGraphList.forEach { data ->
            if (data.show.get()) {
                ImGui.setNextWindowSize(620f, 400f, ImGuiCond.FirstUseEver)

                // Use the show boolean from ShowPriceGraphData
                if (ImGui.begin("Price Graph###${data.product.product_id}", data::show.get(), ImGuiWindowFlags.AlwaysAutoResize)) {
                    priceGraphWindow(data.product)
                }

                // If window was closed, mark for removal
                if (!data.show.get()) {
                    toRemove.add(data)
                }

                ImGui.end()
            }
        }

        // Remove closed windows from the list
        showPriceGraphList.removeAll(toRemove)
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