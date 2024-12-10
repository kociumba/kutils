package org.kociumba.kutils.client.imgui

import imgui.ImGui
import java.awt.Color

/**
 * Simply renders a colored ImGui text from a hex color
 */
fun coloredText(hexColor: String, text: String) {
    val color = hexToImColor(hexColor)
    ImGui.textColored(
        color.r,
        color.g,
        color.b,
        color.a,
        text
    )
}

/**
 * ImColor is a data class holding the components of a color in the range [0, 1],
 * instead of the typical [0, 255]
 */
data class ImColor(val r: Float, val g: Float, val b: Float, val a: Float)


/**
 * Converts a hex string into ImColor for use in ImGui related rendering
 */
fun hexToImColor(hexColor: String): ImColor {
    val color = Color.decode(hexColor)
    return ImColor(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
}