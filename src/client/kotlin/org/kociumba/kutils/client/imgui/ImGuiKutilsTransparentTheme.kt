package org.kociumba.kutils.client.imgui

import imgui.ImGui
import imgui.flag.ImGuiCol
import xyz.breadloaf.imguimc.interfaces.Theme

class ImGuiKutilsTransparentTheme: Theme {
    override fun preRender() {
        val style = ImGui.getStyle()

        // Style settings
        style.windowRounding = 5.0f
        style.frameRounding = 4.0f
        style.indentSpacing = 25.0f
        style.scrollbarSize = 15.0f
        style.scrollbarRounding = 9.0f
        style.grabMinSize = 5.0f
        style.grabRounding = 3.0f

        // Transparent Colors
        style.setColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.00f)
        style.setColor(ImGuiCol.TextDisabled, 0.24f, 0.23f, 0.29f, 0.75f)
        style.setColor(ImGuiCol.WindowBg, 0.06f, 0.05f, 0.07f, 0.30f)
        style.setColor(ImGuiCol.ChildBg, 0.07f, 0.07f, 0.09f, 0.50f)
        style.setColor(ImGuiCol.PopupBg, 0.07f, 0.07f, 0.09f, 0.75f)
        style.setColor(ImGuiCol.Border, 0.80f, 0.80f, 0.83f, 0.10f)
        style.setColor(ImGuiCol.BorderShadow, 0.92f, 0.91f, 0.88f, 0.00f)
        style.setColor(ImGuiCol.FrameBg, 0.10f, 0.09f, 0.12f, 0.50f)
        style.setColor(ImGuiCol.FrameBgHovered, 0.24f, 0.23f, 0.29f, 0.50f)
        style.setColor(ImGuiCol.FrameBgActive, 0.56f, 0.56f, 0.58f, 0.50f)
        style.setColor(ImGuiCol.TitleBg, 0.10f, 0.09f, 0.12f, 0.50f)
        style.setColor(ImGuiCol.TitleBgCollapsed, 1.00f, 0.98f, 0.95f, 0.50f)
        style.setColor(ImGuiCol.TitleBgActive, 0.07f, 0.07f, 0.09f, 0.50f)
        style.setColor(ImGuiCol.MenuBarBg, 0.10f, 0.09f, 0.12f, 0.50f)
        style.setColor(ImGuiCol.ScrollbarBg, 0.10f, 0.09f, 0.12f, 0.50f)
        style.setColor(ImGuiCol.ScrollbarGrab, 0.80f, 0.80f, 0.83f, 0.50f)
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 0.56f, 0.56f, 0.58f, 0.50f)
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.06f, 0.05f, 0.07f, 0.50f)
        style.setColor(ImGuiCol.CheckMark, 0.80f, 0.80f, 0.83f, 0.50f)
        style.setColor(ImGuiCol.SliderGrab, 0.80f, 0.80f, 0.83f, 0.50f)
        style.setColor(ImGuiCol.SliderGrabActive, 0.06f, 0.05f, 0.07f, 0.50f)
        style.setColor(ImGuiCol.Button, 0.10f, 0.09f, 0.12f, 0.50f)
        style.setColor(ImGuiCol.ButtonHovered, 0.24f, 0.23f, 0.29f, 0.50f)
        style.setColor(ImGuiCol.ButtonActive, 0.56f, 0.56f, 0.58f, 0.50f)
        style.setColor(ImGuiCol.Header, 0.10f, 0.09f, 0.12f, 0.50f)
        style.setColor(ImGuiCol.HeaderHovered, 0.56f, 0.56f, 0.58f, 0.50f)
    }

    override fun postRender() {}
}