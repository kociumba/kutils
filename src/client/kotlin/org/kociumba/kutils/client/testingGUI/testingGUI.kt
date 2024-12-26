package org.kociumba.kutils.client.testingGUI

import imgui.ImGui
import org.kociumba.kutils.client.imgui.ImGuiKutilsTheme
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme

object testingGUI : Renderable {
    override fun getName(): String? { return "testingGUI" }

    override fun getTheme(): Theme? { return ImGuiKutilsTheme() }

    override fun render() {
        ImGui.begin("testingGUI")

//        ImGui.image()

        ImGui.end()
    }

}