package org.kociumba.kutils.client.hud

import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import net.minecraft.client.MinecraftClient
import org.kociumba.kutils.client.c
import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import org.kociumba.kutils.client.imgui.coloredText
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme

/**
 * Turns out you can just disable all features of an imgui window,
 * fuck it we ball
 *
 * This needs a "normal gameplay" and a "skyblock" mode
 * couse health is not reported normally by hypixel on skyblock
 */
object hud : Renderable {
    fun init() {
        ImGui.getIO().configWindowsMoveFromTitleBarOnly = false
    }

    override fun getName(): String? {
        return "kutils hud"
    }

    override fun getTheme(): Theme? {
        return ImGuiKutilsTransparentTheme()
    }

    var firstRun = true
    val mc = MinecraftClient.getInstance()

    var health: Float = 0.0f
    var healthMax: Float = 0.0f

    fun getPercentageColor(percent: Double): String {
        return when {
            percent < 25 -> "#FF0000" // Red
            percent < 50 -> "#FFA500" // Orange
            percent < 75 -> "#FFFF00" // Yellow
            else -> "#00FF00" // Green
        }
    }

    override fun render() {
        if (firstRun) {
            init()
            firstRun = !firstRun
        }

//        if (!(mc.isInSingleplayer || mc.isConnectedToLocalServer) || mc.currentScreen != null) return

        ImGui.setNextWindowPos(15.0f, 35.0f, ImGuiCond.Once)

        var windowFlags = ImGuiWindowFlags.NoDecoration or
                ImGuiWindowFlags.NoDocking or
                ImGuiWindowFlags.NoTitleBar or
                ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.AlwaysAutoResize or
                ImGuiWindowFlags.NoFocusOnAppearing or
                ImGuiWindowFlags.NoNav

        if (!c.hudIsDraggable) {
            windowFlags = windowFlags or ImGuiWindowFlags.NoInputs
        }

        if (!c.hudHasBackground) {
            windowFlags = windowFlags or ImGuiWindowFlags.NoBackground
        }

        ImGui.begin(
            "HUD",
            windowFlags
        )

        health = mc.player?.health ?: 0.0f
        healthMax = mc.player?.maxHealth ?: 0.0f

        ImGui.text("Health: ")
        ImGui.sameLine()
        coloredText(getPercentageColor(((health / healthMax).toDouble() * 100)), "%.2f%% HP".format(health))
        ImGui.sameLine()
        ImGui.text("/")
        ImGui.sameLine()
        coloredText("#e66581", "$healthMax HP(max)")


        ImGui.end()
    }
}