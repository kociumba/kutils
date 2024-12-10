package org.kociumba.kutils.client.hud

import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import net.minecraft.client.render.RenderPhase
import org.kociumba.kutils.client.imgui.ImGuiKutilsTheme
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme
import javax.imageio.ImageIO

/**
 * Turns out you can just disable all features of an imgui window,
 * fuck it we ball
 */
object hud: Renderable {
    override fun getName(): String? {
        return "kutils hud"
    }

    override fun getTheme(): Theme? {
        return ImGuiKutilsTheme()
    }

    override fun render() {
        ImGui.setNextWindowPos(15.0f, 35.0f, ImGuiCond.Once)

        ImGui.begin(
            "HUD",
            ImGuiWindowFlags.NoDecoration
                    or ImGuiWindowFlags.NoDocking
                    or ImGuiWindowFlags.NoTitleBar
                    or ImGuiWindowFlags.NoResize
                    or ImGuiWindowFlags.AlwaysAutoResize
                    or ImGuiWindowFlags.NoFocusOnAppearing
                    or ImGuiWindowFlags.NoNav
        )


        ImGui.end()
    }
}