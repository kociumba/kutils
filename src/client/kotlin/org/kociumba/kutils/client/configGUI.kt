package org.kociumba.kutils.client

import gg.essential.universal.UMinecraft
import gg.essential.universal.utils.MCMinecraft
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import imgui.ImGui
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import org.kociumba.kutils.client.hud.hud
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
    var shouldTintDamage = true

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

    @Property(
        type = PropertyType.SWITCH,
        name = "hud",
        description = "health/armor/damage hud (WIP)",
        category = "rendering",
        subcategory = "utils",
    )
    var displayHud: Boolean = true

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
        minF = 0.1f
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
    var shouldConsiderInflatedPercent = 0.2f

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

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "main theme background opacity",
        description = "change the opacity of the main theme background",
        category = "gui",
        subcategory = "theme",
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

    init {
        initialize()

        val clazz = javaClass
        registerListener(clazz.getDeclaredField("damageTintColor")) { color: Color ->
            OverlayTextureListener.notifyColorChanged(color)
        }

        registerListener(clazz.getDeclaredField("shouldTintDamage")) { value: Boolean ->
            if (value) {
                OverlayTextureListener.notifyColorChanged(this.damageTintColor)
            } else {
                OverlayTextureListener.notifyColorChanged(Color(255, 0, 0, 77)) // more or less default
            }
        }

        registerListener(clazz.getDeclaredField("damageTintPresets")) { value: Int ->
            OverlayTextureListener.notifyColorChanged(DamageTintPresets.entries[value].color)
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

        registerListener(clazz.getDeclaredField("displayHud")) { value: Boolean ->
            if (value) {
                Imguimc.pushRenderable(hud)
            } else {
                Imguimc.pullRenderable(hud)
            }
        }

        registerListener(clazz.getDeclaredField("customWindowTitle")) { value: String ->
            if (value.isNotEmpty()) {
                WindowTitleListener.notifyWindowChanged(value)
            } else (
                WindowTitleListener.notifyWindowChanged("")
            )
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

        addDependency(clazz.getDeclaredField("damageTintColor"), clazz.getDeclaredField("shouldTintDamage"))
        addDependency(clazz.getDeclaredField("userTime"), clazz.getDeclaredField("shouldChangeTime"))

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
    }
}