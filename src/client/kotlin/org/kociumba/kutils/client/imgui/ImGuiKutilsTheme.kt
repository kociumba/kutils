package org.kociumba.kutils.client.imgui

import imgui.ImGui
import imgui.flag.ImGuiCol
import xyz.breadloaf.imguimc.interfaces.Theme

/**
 * WIP theme for the imgui uis in kutils
 */
class ImGuiKutilsTheme: Theme {
    override fun preRender() {
        val style = ImGui.getStyle()

        // Style settings
//        style.windowPadding = ImVec2(15f, 15f)
        style.windowRounding = 5.0f
//        style.framePadding = ImVec2(5f, 5f)
        style.frameRounding = 4.0f
//        style.itemSpacing = ImVec2(12f, 8f)
//        style.itemInnerSpacing = ImVec2(8f, 6f)
        style.indentSpacing = 25.0f
        style.scrollbarSize = 15.0f
        style.scrollbarRounding = 9.0f
        style.grabMinSize = 5.0f
        style.grabRounding = 3.0f

        // Colors
        style.setColor(ImGuiCol.Text, 0.80f, 0.80f, 0.83f, 1.00f)
        style.setColor(ImGuiCol.TextDisabled, 0.24f, 0.23f, 0.29f, 1.00f)
        style.setColor(ImGuiCol.WindowBg, 0.06f, 0.05f, 0.07f, 1.00f)
        style.setColor(ImGuiCol.ChildBg, 0.07f, 0.07f, 0.09f, 1.00f)
        style.setColor(ImGuiCol.PopupBg, 0.07f, 0.07f, 0.09f, 1.00f)
        style.setColor(ImGuiCol.Border, 0.80f, 0.80f, 0.83f, 0.88f)
        style.setColor(ImGuiCol.BorderShadow, 0.92f, 0.91f, 0.88f, 0.00f)
        style.setColor(ImGuiCol.FrameBg, 0.10f, 0.09f, 0.12f, 1.00f)
        style.setColor(ImGuiCol.FrameBgHovered, 0.24f, 0.23f, 0.29f, 1.00f)
        style.setColor(ImGuiCol.FrameBgActive, 0.56f, 0.56f, 0.58f, 1.00f)
        style.setColor(ImGuiCol.TitleBg, 0.10f, 0.09f, 0.12f, 1.00f)
        style.setColor(ImGuiCol.TitleBgCollapsed, 1.00f, 0.98f, 0.95f, 0.75f)
        style.setColor(ImGuiCol.TitleBgActive, 0.07f, 0.07f, 0.09f, 1.00f)
        style.setColor(ImGuiCol.MenuBarBg, 0.10f, 0.09f, 0.12f, 1.00f)
        style.setColor(ImGuiCol.ScrollbarBg, 0.10f, 0.09f, 0.12f, 1.00f)
        style.setColor(ImGuiCol.ScrollbarGrab, 0.80f, 0.80f, 0.83f, 0.31f)
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 0.56f, 0.56f, 0.58f, 1.00f)
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.06f, 0.05f, 0.07f, 1.00f)
        style.setColor(ImGuiCol.CheckMark, 0.80f, 0.80f, 0.83f, 0.31f)
        style.setColor(ImGuiCol.SliderGrab, 0.80f, 0.80f, 0.83f, 0.31f)
        style.setColor(ImGuiCol.SliderGrabActive, 0.06f, 0.05f, 0.07f, 1.00f)
        style.setColor(ImGuiCol.Button, 0.10f, 0.09f, 0.12f, 1.00f)
        style.setColor(ImGuiCol.ButtonHovered, 0.24f, 0.23f, 0.29f, 1.00f)
        style.setColor(ImGuiCol.ButtonActive, 0.56f, 0.56f, 0.58f, 1.00f)
        style.setColor(ImGuiCol.Header, 0.10f, 0.09f, 0.12f, 1.00f)
        style.setColor(ImGuiCol.HeaderHovered, 0.56f, 0.56f, 0.58f, 1.00f)
        style.setColor(ImGuiCol.HeaderActive, 0.06f, 0.05f, 0.07f, 1.00f)
        style.setColor(ImGuiCol.Separator, 0.56f, 0.56f, 0.58f, 1.00f)
        style.setColor(ImGuiCol.SeparatorHovered, 0.24f, 0.23f, 0.29f, 1.00f)
        style.setColor(ImGuiCol.SeparatorActive, 0.56f, 0.56f, 0.58f, 1.00f)
        style.setColor(ImGuiCol.ResizeGrip, 0.00f, 0.00f, 0.00f, 0.00f)
        style.setColor(ImGuiCol.ResizeGripHovered, 0.56f, 0.56f, 0.58f, 1.00f)
        style.setColor(ImGuiCol.ResizeGripActive, 0.06f, 0.05f, 0.07f, 1.00f)
        style.setColor(ImGuiCol.PlotLines, 0.40f, 0.39f, 0.38f, 0.63f)
        style.setColor(ImGuiCol.PlotLinesHovered, 0.25f, 1.00f, 0.00f, 1.00f)
        style.setColor(ImGuiCol.PlotHistogram, 0.40f, 0.39f, 0.38f, 0.63f)
        style.setColor(ImGuiCol.PlotHistogramHovered, 0.25f, 1.00f, 0.00f, 1.00f)
        style.setColor(ImGuiCol.TextSelectedBg, 0.25f, 1.00f, 0.00f, 0.43f)
        style.setColor(ImGuiCol.ModalWindowDimBg, 1.00f, 0.98f, 0.95f, 0.73f)
    }

    override fun postRender() {}
}