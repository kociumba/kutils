package org.kociumba.kutils.client.imgui

import gg.essential.universal.UMinecraft
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

/**
 * Font setup, can't do it in the onModInitialize couse the imgui context is not created yet
 *
 * my solution is to have an object here that only executes the first time it is called
 * and then any part of the mod that uses imgui can call it
 *
 * this is completely fucked, I have to make a custom for of the imgui-mc repo
 * to be able to load custom fonts, 💀
 */
//object ImFontSetup  {
//    private var once = false
//
//    fun setup() {
//        if (!once) {
//            once = true
//            var io = ImGui.getIO()
//            var font = io.fonts.addFontFromFileTTF("assets/fonts/CascadiaCode.ttf", 16f)
//        }
//    }
//}

/**
 * utils for sizing and positioning imgui windows using the coordinates and size of the minecraft window
 * instead of the whole monitor minecraft is displaying on
 *
 * TODO: needs more work couse in theory it's fine but does not work
 */
fun getRelativeX(x: Float): Float {
    return x - UMinecraft.getMinecraft().window.x
}

fun getRelativeY(y: Float): Float {
    return y - UMinecraft.getMinecraft().window.y
}

fun getRelativeWidth(width: Float): Float {
    return width / UMinecraft.getMinecraft().window.width
}

fun getRelativeHeight(height: Float): Float {
    return height / UMinecraft.getMinecraft().window.height
}

fun setNextWindowPositionRelative(x: Float, y: Float, flags: Int) {
    ImGui.setNextWindowPos(getRelativeX(x), getRelativeY(y), flags)
}

fun setNextWindowSizeRelative(width: Float, height: Float, flags: Int) {
    ImGui.setNextWindowSize(getRelativeWidth(width), getRelativeHeight(height), flags)
}