package org.kociumba.kutils.client

import gg.essential.universal.utils.MCMinecraft
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import imgui.ImGui
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import org.kociumba.kutils.client.bazaar.WeightEdit
import org.kociumba.kutils.client.chat.ChatImageUI
import org.kociumba.kutils.client.events.OverlayColorChangeEvent
import org.kociumba.kutils.client.events.WindowTitleChangedEvent
import org.kociumba.kutils.client.hud.hud
import org.kociumba.kutils.client.hud.networkingHud
import org.kociumba.kutils.client.hud.performanceHud
import xyz.breadloaf.imguimc.Imguimc
import java.awt.Color
import java.io.File

/**
 * The best color presets I could think of
 *
 * They are in fact so good I'm not going to enable them in the public build 💀
 */
enum class DamageTintPresets(val color: Color) {
    PissYellow(Color(255, 242, 78)),
    ShitBrown(Color(82, 50, 15)),
    TittyMilk(Color(242, 223, 228)),
    PussyPink(Color(227, 153, 143)),
    WeedGreen(Color(1, 94, 7)),
    MethBlue(Color(140, 200, 222)),
}

@Environment(EnvType.CLIENT)
class ConfigGUI : Vigilant(File("./config/kutils.toml")) {
    @Property(
        type = PropertyType.SWITCH,
        name = "custom damage tint",
        description = "toggle custom damage tint coloring",
        category = "rendering",
        subcategory = "entity",
    )
    var shouldTintDamage: Boolean = true

    @Property(
        type = PropertyType.COLOR,
        name = "custom damage tint color",
        description = "change the color of the damage tint",
        category = "rendering",
        subcategory = "entity",
        allowAlpha = true
    )
    var damageTintColor: Color = Color(255, 0, 0, 77)

    @Property(
        type = PropertyType.SELECTOR,
        name = "damage tint presets",
        description = "change the color of the damage tint",
        category = "rendering",
        subcategory = "entity",
        options = ["PissYellow", "ShitBrown", "TittyMilk", "PussyPink", "WeedGreen", "MethBlue"],
        hidden = true
    )
    var damageTintPresets: Int = DamageTintPresets.PissYellow.ordinal

    /*
     * Gonna have to re add this later as an easter egg
     */
    @Property(
        type = PropertyType.SWITCH,
        name = "custom water tint",
        description = "toggle custom water coloring",
        category = "rendering",
        subcategory = "world",
        hidden = true
    )
    var shouldColorWater: Boolean = false

    @Property(
        type = PropertyType.COLOR,
        name = "custom water tint color",
        description = "change the color of the water",
        category = "rendering",
        subcategory = "world",
        allowAlpha = true,
        hidden = true
    )
    var waterColor: Color = Color(0, 0, 255, 77)

    @Property(
        type = PropertyType.SWITCH,
        name = "change time",
        description = "toggle time change",
        category = "rendering",
        subcategory = "world",
    )
    var shouldChangeTime: Boolean = false

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "user time",
        description = "world time the changer should use",
        category = "rendering",
        subcategory = "world",
        minF = 0.0f,
        maxF = 24000.0f
    )
    var userTime: Float = 0.0f

    @Property(
        type = PropertyType.SWITCH,
        name = "always sprint",
        description = "toggle always sprint",
        category = "player",
        subcategory = "movement"
    )
    var shouldAlwaysSprint: Boolean = false

    @Property(
        type = PropertyType.TEXT,
        name = "custom window title text",
        description = "⚠ window title updates when you close the settings menu",
        category = "misc",
        subcategory = "window",
    )
    var customWindowTitle: String = ""

    /*
     * not worth keeping for now, couse I don't wanna parse hypixel data to get skyblock stats
     */
    @Property(
        type = PropertyType.SWITCH,
        name = "hud",
        description = "health/armor/damage hud (WIP)",
        category = "rendering",
        subcategory = "utils",
        hidden = true
    )
    var displayHud: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "performance hud",
        description = "display a hud with cpu and memory usage",
        category = "rendering",
        subcategory = "utils",
    )
    var displayPerformanceHud: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "networking hud",
        description = "display a hud with info about the current server and client networking, like TPS",
        category = "rendering",
        subcategory = "utils",
    )
    var displayNetworkingHud: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "disable block breaking particles",
        description = "prevents block breaking particles from being rendered",
        category = "rendering",
        subcategory = "particles",
    )
    var disableBlockBreakParticle: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "toggle fullbright",
        description = "this is just high gamma, does not remove lighting 🤷",
        category = "rendering",
        subcategory = "utils",
    )
    var shouldUseFullbright: Boolean = false

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "font scale",
        description = "change the font size used in all of the hud elements",
        category = "gui",
        subcategory = "kutils ui",
        maxF = 2.0f,
        minF = 0.1f,
    )
    var fontScale: Float = 1.0f

    @Property(
        type = PropertyType.SWITCH,
        name = "show weekly traffic",
        description = "show weekly bazaar traffic in the bazaar ui",
        category = "gui",
        subcategory = "kutils ui",
    )
    var showWeeklyTraffic: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "show weekly average price",
        description = "show weekly average price in the bazaar ui",
        category = "gui",
        subcategory = "kutils ui",
    )
    var showWeeklyAveragePrice: Boolean = true

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "inflated percent",
        description = "change at which point the bazaar items should be considered inflated" +
                " the percent is how many percent higher is the current sell/buy price than the average from last 7 days",
        category = "gui",
        subcategory = "kutils ui",
    )
    var shouldConsiderInflatedPercent: Float = 0.2f

    @Property(
        type = PropertyType.SWITCH,
        name = "hud has background",
        description = "toggle the background of the hud",
        category = "gui",
        subcategory = "kutils ui",
    )
    var hudHasBackground: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "hud is draggable",
        description = "toggle the draggability of the hud",
        category = "gui",
        subcategory = "kutils ui",
    )
    var hudIsDraggable: Boolean = true

    /**
     * hidden couse of the theme section
     */
    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "main theme background opacity",
        description = "change the opacity of the main theme background",
        category = "gui",
        subcategory = "theme",
        hidden = true
    )
    var mainThemeBackgroundOpacity: Float = 1.0f

    @Property(
        type = PropertyType.SWITCH,
        name = "remove selfie camera",
        description = "removes the selfie camera when using F5",
        category = "player",
        subcategory = "camera",
    )
    var removeSelfieCamera: Boolean = true

    /**
     * Theme customisation
     *
     * user colors and values
     */

    /**
     * options related to the imgui theme
     */
    @Property(
        type = PropertyType.COLOR,
        name = "window background",
        description = "change the background color of the main bazaar window",
        category = "gui",
        subcategory = "theme",
        allowAlpha = true
    )
    var mainWindowBackground: Color = Color(0.05f, 0.05f, 0.05f, 1.0f)

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "window rounding",
        description = "change the rounding of the window corners",
        category = "gui",
        subcategory = "theme",
        maxF = 25.0f,
        minF = 0.0f
    )
    var windowRounding: Float = 5.0f

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "general opacity",
        description = "change the opacity of the window and everything in it",
        category = "gui",
        subcategory = "theme",
    )
    var wholeWindowAlpha: Float = 1.0f
//    var childBackground = Color(0.07f, 0.07f, 0.09f, 1.00f)

    /**
     * options related to the bazaar display itself
     */
    @Property(
        type = PropertyType.COLOR,
        name = "product ID color",
        description = "color of the product ID",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var productIDColor: Color = Color.decode("#cba6f7")

    @Property(
        type = PropertyType.COLOR,
        name = "sell price color",
        description = "color of the sell price",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var sellPriceColor: Color = Color.decode("#94e2d5")

    @Property(
        type = PropertyType.COLOR,
        name = "buy price color",
        description = "color of the buy price",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var buyPriceColor: Color = Color.decode("#eba0ac")

    @Property(
        type = PropertyType.COLOR,
        name = "difference color",
        description = "color of the difference",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var differenceColor: Color = Color.decode("#89b4fa")

    @Property(
        type = PropertyType.COLOR,
        name = "weekly traffic color",
        description = "color of the weekly traffic",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var weeklyTrafficColor: Color = Color.decode("#fab387")

    @Property(
        type = PropertyType.COLOR,
        name = "averages color",
        description = "color of the averages",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var averagesColor: Color = Color.decode("#f9e2af")

    @Property(
        type = PropertyType.COLOR,
        name = "positive prediction color",
        description = "color of the positive prediction",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var positivePredictionColor: Color = Color.decode("#a6e3a1")

    @Property(
        type = PropertyType.COLOR,
        name = "negative prediction color",
        description = "color of the negative prediction",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var negativePredictionColor: Color = Color.decode("#f38ba8")

    @Property(
        type = PropertyType.COLOR,
        name = "inflated item warning color",
        description = "color of the inflated item warning",
        category = "gui",
        subcategory = "theme",
        allowAlpha = false
    )
    var inflatedItemWarningColor: Color = Color.decode("#ff0000")

    @Property(
        type = PropertyType.SWITCH,
        name = "preview images in chat",
        description = "toggle if kutils should show a preview of images in chat, when hovering over them",
        category = "chat",
        subcategory = "addons",
    )
    var shouldPreviewChatImages: Boolean = true

    @Property(
        type = PropertyType.BUTTON,
        name = "edit prediction weights",
        description = "edit the weights of the prediction model",
        category = "gui",
        subcategory = "internal",
        placeholder = "open editor",
    )
    fun editWeights() {
        if (!WeightEdit.rendered) {
            WeightEdit.loadWeights()
            WeightEdit.rendered = true
            Imguimc.pushRenderable(WeightEdit)
        } else {
            Imguimc.pullRenderable(WeightEdit)
            WeightEdit.rendered = false
            WeightEdit.saveWeights()
        }
    }

    @Property(
        type = PropertyType.SWITCH,
        name = "saab mode",
        description = "???",
        category = "gui",
        subcategory = "???"
    )
    var saabMode: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "submit signs with enter",
        description = "you can still use shift+enter to write new lines",
        category = "gui",
        subcategory = "signs"
    )
    var shouldSubmitSignsWithEnter: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "use custom xp orbs",
        description = "use custom xp orb renderer that makes them emissive and allows for custom colors",
        category = "rendering",
        subcategory = "entity",
    )
    var shouldUsecustomXpOrbs = true

    @Property(
        type = PropertyType.COLOR,
        name = "custom xp orb color",
        description = "the color to use when the custom xp orb renderin is being used",
        category = "rendering",
        subcategory = "entity",
    )
    var customXpOrbColor: Color = Color(9, 137, 9, 255)

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "custom xp orb size",
        description = "the size of the custom xp orbs",
        category = "rendering",
        subcategory = "entity",
        minF = 0.0f,
        maxF = 2.0f,
    )
    var customXpOrbSize: Float = 1.0f

    init {
        initialize()

        val clazz = javaClass
        registerListener(clazz.getDeclaredField("damageTintColor")) { color: Color ->
            OverlayColorChangeEvent.publish(OverlayColorChangeEvent(color))
        }

        registerListener(clazz.getDeclaredField("shouldTintDamage")) { value: Boolean ->
            if (value) {
                OverlayColorChangeEvent.publish(OverlayColorChangeEvent(this.damageTintColor))
            } else {
                OverlayColorChangeEvent.publish(OverlayColorChangeEvent(Color(255, 0, 0, 77))) // more or less default
            }
        }

        registerListener(clazz.getDeclaredField("damageTintPresets")) { value: Int ->
            OverlayColorChangeEvent.publish(OverlayColorChangeEvent(DamageTintPresets.entries[value].color))
            damageTintColor = DamageTintPresets.entries[value].color
        }

        registerListener(clazz.getDeclaredField("displayHud")) { value: Boolean ->
            if (value) {
                Imguimc.pushRenderable(hud)
            } else {
                Imguimc.pullRenderable(hud)
            }
        }

        registerListener(clazz.getDeclaredField("displayPerformanceHud")) { value: Boolean ->
            if (value) {
                Imguimc.pushRenderable(performanceHud)
            } else {
                Imguimc.pullRenderable(performanceHud)
            }
        }

        registerListener(clazz.getDeclaredField("displayNetworkingHud")) { value: Boolean ->
            if (value) {
                Imguimc.pushRenderable(networkingHud)
            } else {
                Imguimc.pullRenderable(networkingHud)
            }
        }

        registerListener(clazz.getDeclaredField("displayHud")) { value: Boolean ->
            if (value) {
                Imguimc.pushRenderable(hud)
            } else {
                Imguimc.pullRenderable(hud)
            }
        }

        registerListener(clazz.getDeclaredField("customWindowTitle")) { value: String ->
            if (value.isNotEmpty()) {
                WindowTitleChangedEvent.publish(WindowTitleChangedEvent(value))
            } else (
                    WindowTitleChangedEvent.publish(
                        WindowTitleChangedEvent("")
                    ))
        }

        registerListener(clazz.getDeclaredField("shouldUseFullbright")) { value: Boolean ->
            if (value && MinecraftClient.getInstance().options != null) {
                MCMinecraft.getInstance().options.gamma.value = 100.0
            } else {
                MCMinecraft.getInstance().options.gamma.value = 1.0
            }
        }

        registerListener(clazz.getDeclaredField("fontScale")) { value: Float ->
            ImGui.getIO().fontGlobalScale = value
        }

        registerListener(clazz.getDeclaredField("shouldPreviewChatImages")) { value: Boolean ->
            if (value) {
                ChatImageUI.initialize()
                Imguimc.pushRenderable(ChatImageUI)
            } else {
                Imguimc.pullRenderable(ChatImageUI)
            }
        }

        registerListener(clazz.getDeclaredField("saabMode")) { value: Boolean ->
            if (value) {
                Imguimc.pushRenderable(saab)
            } else {
                Imguimc.pullRenderable(saab)
            }
        }

        addDependency(clazz.getDeclaredField("damageTintColor"), clazz.getDeclaredField("shouldTintDamage"))
        addDependency(clazz.getDeclaredField("userTime"), clazz.getDeclaredField("shouldChangeTime"))
        addDependency(clazz.getDeclaredField("customXpOrbColor"), clazz.getDeclaredField("shouldUsecustomXpOrbs"))
        addDependency(clazz.getDeclaredField("customXpOrbSize"), clazz.getDeclaredField("shouldUsecustomXpOrbs"))

        setCategoryDescription(
            "rendering",
            "options related to rendering"
        )

        setSubcategoryDescription(
            "rendering",
            "entity",
            "options related to entity and player rendering"
        )

        setSubcategoryDescription(
            "rendering",
            "world",
            "options related to world client side rendering and properties"
        )

        setSubcategoryDescription(
            "gui",
            "internal",
            "edit the prediction weights for bazaar ui, they should always add up roughly to 1"
        )
    }
}