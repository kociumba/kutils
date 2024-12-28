package org.kociumba.kutils.client.chat

import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import net.minecraft.text.HoverEvent
import org.kociumba.kutils.client.client
import org.kociumba.kutils.log
import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import org.kociumba.kutils.client.imgui.ImImage
import org.kociumba.kutils.client.imgui.LoadingState
import org.kociumba.kutils.client.imgui.spinner
import org.kociumba.kutils.client.testingGUI.testingGUI
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme

object ChatImageUI : Renderable {
    override fun getName(): String? { return "ChatImagePreviewUI" }

    override fun getTheme(): Theme? { return ImGuiKutilsTransparentTheme() }

    var currentLink: String? = null
    var texture: ImImage? = null

    override fun render() {
        // spawns a no-background, no interaction imgui window
        // aligned with the bottom left corner to the mouse position
        if (checkHoverLink()) {
            currentLink?.let { link ->
                testingGUI.texture = ImImage().apply{
                    loadImageFromURL(link) { success ->
                        if (!success) {
                            log.error("Failed to load image from url $link")
                        }
                    }
                }
            }
        }

        ImGui.setNextWindowPos(client.window.x.toFloat(), client.window.y.toFloat(), ImGuiCond.Always)
        ImGui.setNextWindowSize(client.window.width.toFloat(), client.window.height.toFloat(), ImGuiCond.Always)

        ImGui.begin("ChatImagePreviewUI",
            ImGuiWindowFlags.NoBackground or
                ImGuiWindowFlags.NoDecoration or
                ImGuiWindowFlags.NoInputs or
                ImGuiWindowFlags.NoTitleBar or
                ImGuiWindowFlags.NoFocusOnAppearing or
                ImGuiWindowFlags.NoNav or
                ImGuiWindowFlags.NoMove or
                ImGuiWindowFlags.NoScrollbar or
                ImGuiWindowFlags.NoMouseInputs or
                ImGuiWindowFlags.NoBringToFrontOnFocus
        )

        texture?.let{ img ->
            when (img.loadingState) {
                LoadingState.IDLE -> {
                    return@let
                }
                LoadingState.LOADING -> {
                    ImGui.sameLine()
                    val buttonHeight = ImGui.getFrameHeight()
                    val radius = buttonHeight / 2 - 1f  // 1 looks the best
                    val thickness = buttonHeight / 7f   // 7 looks the best
                    spinner("##s", radius, thickness, ImGui.getColorU32(1f, 1f, 1f, 1f))
                }
                LoadingState.LOADED -> {
                    if (img.isValid) {
                        ImGui.image(img.glID, img.width.toFloat(), img.height.toFloat())
                    }
                }
                LoadingState.ERROR -> {
                    ImGui.text("error: ${img.errorMessage}")
                }
            }
        }

        ImGui.end()
    }

    fun checkHoverLink() : Boolean {
        val hoveredComponent = client.inGameHud.chatHud.getTextStyleAt(client.mouse.x, client.mouse.y)
        val hoverEvent = hoveredComponent?.hoverEvent ?: return false
        val value = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT) ?: return false
        currentLink = urlRegex.find(value.string)?.value ?: return false
        log.info("Hovered link: $currentLink")
        return true
    }
}