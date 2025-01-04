package org.kociumba.kutils.client.notes

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.Window
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.markdown.MarkdownComponent
import gg.essential.elementa.markdown.MarkdownConfig
import gg.essential.universal.UMatrixStack
import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import org.kociumba.kutils.client.client
import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import org.lwjgl.glfw.GLFW
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme

class NoteOverlay(private val note: Note) : Renderable {
    private var flags =
//        ImGuiWindowFlags.NoDecoration or
            ImGuiWindowFlags.NoFocusOnAppearing or
            ImGuiWindowFlags.NoNav

    private var locked = false

    override fun getName(): String = note.title

    override fun getTheme(): Theme? = ImGuiKutilsTransparentTheme()

    override fun render() {
        if (!note.isOverlayed) {
            // Automatically remove the overlay if the note is no longer overlayed
            Imguimc.pullRenderableAfterRender(this)
            return
        }

        flags = if (locked) {
            flags or ImGuiWindowFlags.NoBackground or
                    ImGuiWindowFlags.NoCollapse or
                    ImGuiWindowFlags.NoMove or
                    ImGuiWindowFlags.NoDecoration
        } else {
            flags and ImGuiWindowFlags.NoBackground.inv() and
                    ImGuiWindowFlags.NoCollapse.inv() and
                    ImGuiWindowFlags.NoMove.inv() and
                    ImGuiWindowFlags.NoDecoration.inv()
        }

        ImGui.setNextWindowSizeConstraints(200f, 100f, client.window.width.toFloat(), client.window.height.toFloat())

        ImGui.begin(name, flags)

        if (ImGui.beginPopupContextWindow("right_click_menu", GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            if (ImGui.menuItem("close the note")) {
                note.isOverlayed = false
                Imguimc.pullRenderableAfterRender(this)
            }
            if (ImGui.menuItem(if (locked) "unlock the note" else "lock the note")) {
                locked = !locked
            }
            ImGui.endPopup()
        }

        ImGui.textWrapped(note.content)

        ImGui.end()
    }
}

/**
 * Experiments for rendering markdown with elementa
 *
 * very buggy, would need a shit ton of effort for this small feature
 */
class TestOverlay() {
    fun renderElementaOverlay() {
        val note = NoteData.notes[0]
        val window = Window(ElementaVersion.V7)
        val mdConfig = MarkdownConfig()
        mdConfig.codeBlockConfig.enabled
        mdConfig.headerConfig.enabled

        MarkdownComponent(
            note.content.trimIndent(),
            mdConfig,
        ).constrain {
            x = 5.pixels
            y = 5.pixels
            width = (client.window.width / 2).pixels
            height = (client.window.height / 2).pixels
        } childOf window

        Inspector(window).constrain {
            x = 10.pixels(true)
            y = 10.pixels(true)
        } childOf window

        window.draw(UMatrixStack())
    }
}