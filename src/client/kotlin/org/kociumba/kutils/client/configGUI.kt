package org.kociumba.kutils.client

import gg.essential.universal.UMinecraft
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
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
    // the default is close to the minecraft default look, since the core shader method is bad,
    // and it can not be disabled easily, one more reason to migrate this to a mixin

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

    @Property(
        type = PropertyType.BUTTON,
        name = "reload resource changes",
        description = "on account of me being a dumbass, and the minecraft api changing " +
                "a lot since 1.8.9, I couldn't find a non buggy way of doing damage tinting without " +
                "using core shaders as a resource, that's why you need to reload resources to apply the changes.",
        category = "rendering",
        subcategory = "entity",
        placeholder = "reload",
        hidden = true
    )
    fun reload() {
        UMinecraft.getMinecraft().reloadResourcesConcurrently()
    }

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
        name = "custom window title",
        description = "change the window title",
        category = "misc",
        subcategory = "window",
        hidden = true
    )
    var customWindowTitle: String = ""

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

//        registerListener(clazz.getDeclaredField("customWindowTitle")) { value: String ->
//            if (value.isNotEmpty()) {
////                UMinecraft.getMinecraft().window.setTitle(value)
//                mainWindow.setTitle(value)
//            }
//        }

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