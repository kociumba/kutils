package org.kociumba.kmod.client

import gg.essential.universal.UMinecraft
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import java.awt.Color
import java.io.File

class ConfigGUI : Vigilant(File("./config/kmod.toml")) {
    @Property(
        type = PropertyType.SWITCH,
        name = "custom damage tint",
        description = "toggle custom damage tint coloring",
        category = "rendering",
        hidden = true,
    )
    var damageTintSwitch = true

    @Property(
        type = PropertyType.COLOR,
        name = "custom damage tint color",
        description = "change the color of the damage tint",
        category = "rendering",
        subcategory = "damage tint",
        allowAlpha = true
    )
    var damageTintColor: Color = Color(255, 0, 0, 77)

    @Property(
        type = PropertyType.BUTTON,
        name = "reload resource changes",
        description = "on account of me being a dumbass, and the minecraft api changing " +
                "a lot since 1.8.9, I couldn't find a non buggy way of doing damage tinting without " +
                "using core shaders as a resource, that's why you need to reload resources to apply the changes.",
        category = "rendering",
        subcategory = "damage tint",
        placeholder = "reload"
    )
    fun reload() {
        UMinecraft.getMinecraft().reloadResourcesConcurrently()
    }

    @Property(
        type = PropertyType.CHECKBOX,
        name = "change time",
        description = "toggle time change",
        category = "world",
        subcategory = "time",
    )
    var shouldChangeTime: Boolean = false

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "user time",
        description = "world time the changer should use",
        category = "world",
        subcategory = "time",
        minF = 0.0f,
        maxF = 24000.0f
    )
    var userTime: Float = 0.0f

    init {
        initialize()
    }
}