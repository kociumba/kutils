package org.kociumba.kutils.client.imgui

import imgui.ImGui
import imgui.flag.ImGuiCol
import org.kociumba.kutils.client.c
import xyz.breadloaf.imguimc.interfaces.Theme

/**
 * WIP theme for the imgui uis in kutils
 */
class ImGuiKutilsThemeNoTransparent : Theme {
    override fun preRender() {
        val style = ImGui.getStyle()
        var bg = c.mainWindowBackground
        
        style.windowRounding = c.windowRounding
        style.frameRounding = 4.0f
        style.scrollbarRounding = 9.0f
        style.grabRounding = 3.0f
        style.indentSpacing = 25.0f
        style.scrollbarSize = 15.0f
        style.grabMinSize = 5.0f
        style.alpha = c.wholeWindowAlpha

        style.setColor(ImGuiCol.Text, 1.00f, 1.00f, 1.00f, 1.00f)
        style.setColor(ImGuiCol.TextDisabled, 0.50f, 0.50f, 0.50f, 1.00f)
//        style.setColor(ImGuiCol.WindowBg, 0.04f, 0.04f, 0.04f, 0.94f)
        style.setColor(ImGuiCol.WindowBg, (bg.red / 255f), (bg.green / 255f), (bg.blue / 255f), 255f)
        style.setColor(ImGuiCol.ChildBg, 0.00f, 0.00f, 0.00f, 0.00f)
        style.setColor(ImGuiCol.PopupBg, 0.08f, 0.08f, 0.08f, 1.00f)
        style.setColor(ImGuiCol.Border, 0.43f, 0.43f, 0.50f, 0.50f)
        style.setColor(ImGuiCol.BorderShadow, 0.00f, 0.00f, 0.00f, 0.00f)
        style.setColor(ImGuiCol.FrameBg, 0.15f, 0.15f, 0.15f, 0.54f)
        style.setColor(ImGuiCol.FrameBgHovered, 0.48f, 0.26f, 0.98f, 0.40f)
        style.setColor(ImGuiCol.FrameBgActive, 0.37f, 0.00f, 1.00f, 1.00f)
        style.setColor(ImGuiCol.TitleBg, 0.04f, 0.04f, 0.04f, 1.00f)
        style.setColor(ImGuiCol.TitleBgActive, 0.21f, 0.16f, 0.48f, 1.00f)
        style.setColor(ImGuiCol.TitleBgCollapsed, 0.00f, 0.00f, 0.00f, 0.51f)
        style.setColor(ImGuiCol.MenuBarBg, 0.11f, 0.11f, 0.11f, 1.00f)
        style.setColor(ImGuiCol.ScrollbarBg, 0.02f, 0.02f, 0.02f, 0.53f)
        style.setColor(ImGuiCol.ScrollbarGrab, 0.31f, 0.31f, 0.31f, 1.00f)
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 0.41f, 0.41f, 0.41f, 1.00f)
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.51f, 0.51f, 0.51f, 1.00f)
        style.setColor(ImGuiCol.CheckMark, 0.90f, 0.90f, 0.90f, 0.50f)
        style.setColor(ImGuiCol.SliderGrab, 0.51f, 0.51f, 0.51f, 1.00f)
        style.setColor(ImGuiCol.SliderGrabActive, 0.74f, 0.74f, 0.74f, 1.00f)
        style.setColor(ImGuiCol.Button, 0.15f, 0.15f, 0.15f, 0.62f)
        style.setColor(ImGuiCol.ButtonHovered, 0.28f, 0.28f, 0.28f, 1.00f)
        style.setColor(ImGuiCol.ButtonActive, 0.37f, 0.00f, 1.00f, 1.00f)
        style.setColor(ImGuiCol.Header, 0.20f, 0.20f, 0.20f, 0.52f)
        style.setColor(ImGuiCol.HeaderHovered, 0.30f, 0.30f, 0.30f, 0.80f)
        style.setColor(ImGuiCol.HeaderActive, 0.37f, 0.00f, 1.00f, 1.00f)
        style.setColor(ImGuiCol.Separator, 0.43f, 0.43f, 0.50f, 0.50f)
        style.setColor(ImGuiCol.SeparatorHovered, 0.48f, 0.26f, 0.98f, 0.78f)
        style.setColor(ImGuiCol.SeparatorActive, 0.37f, 0.00f, 1.00f, 1.00f)
        style.setColor(ImGuiCol.ResizeGrip, 0.30f, 0.30f, 0.30f, 0.17f)
        style.setColor(ImGuiCol.ResizeGripHovered, 0.48f, 0.26f, 0.98f, 0.78f)
        style.setColor(ImGuiCol.ResizeGripActive, 0.37f, 0.00f, 1.00f, 1.00f)
        style.setColor(ImGuiCol.Tab, 0.11f, 0.11f, 0.11f, 1.00f)
        style.setColor(ImGuiCol.TabHovered, 0.48f, 0.26f, 0.98f, 0.78f)
        style.setColor(ImGuiCol.TabActive, 0.37f, 0.00f, 1.00f, 1.00f)
        style.setColor(ImGuiCol.TabUnfocused, 0.07f, 0.10f, 0.15f, 0.97f)
        style.setColor(ImGuiCol.TabUnfocusedActive, 0.14f, 0.26f, 0.42f, 1.00f)
        style.setColor(ImGuiCol.DockingPreview, 0.85f, 0.85f, 0.85f, 0.28f)
        style.setColor(ImGuiCol.DockingEmptyBg, 0.20f, 0.20f, 0.20f, 1.00f)
        style.setColor(ImGuiCol.TextSelectedBg, 0.87f, 0.87f, 0.87f, 0.35f)
        style.setColor(ImGuiCol.DragDropTarget, 1.00f, 1.00f, 0.00f, 0.90f)
        style.setColor(ImGuiCol.NavHighlight, 0.48f, 0.26f, 0.98f, 0.78f)
        style.setColor(ImGuiCol.NavWindowingHighlight, 1.00f, 1.00f, 1.00f, 0.70f)
        style.setColor(ImGuiCol.NavWindowingDimBg, 0.80f, 0.80f, 0.80f, 0.20f)
        style.setColor(ImGuiCol.ModalWindowDimBg, 0.80f, 0.80f, 0.80f, 0.35f)
    }

    override fun postRender() {}
}