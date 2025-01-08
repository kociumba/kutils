package org.kociumba.kutils.client.lua

import imgui.ImGui
import imgui.extension.texteditor.TextEditor
import imgui.extension.texteditor.TextEditorLanguageDefinition
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiWindowFlags
import net.minecraft.text.Text
import org.kociumba.kutils.client.imgui.ImGuiKutilsThemeNoTransparent
import xyz.breadloaf.imguimc.screen.ImGuiScreen
import java.lang.reflect.Field
import org.kociumba.kutils.log
import xyz.breadloaf.imguimc.screen.ImGuiWindow
import xyz.breadloaf.imguimc.screen.WindowRenderer

object LuaEditor: ImGuiScreen(Text.literal("Lua Editor"), true) {
    private lateinit var scriptManager: ModuleManager
    private var selectedScript: String? = null
    private lateinit var alreadyInitialisedField: Field

    private val editor = TextEditor().apply {
        setLanguageDefinition(TextEditorLanguageDefinition.lua())
    }

    init {
        try {
            alreadyInitialisedField = ImGuiScreen::class.java.getDeclaredField("alreadyInitialised")
            alreadyInitialisedField.isAccessible = true
        } catch (e: Exception) {
            log.error("Failed to get alreadyInitialised field", e)
        }
    }

    fun initialize(manager: ModuleManager) {
        scriptManager = manager
    }

    private fun setAlreadyInitialised(value: Boolean) {
        try {
            alreadyInitialisedField.setBoolean(this, value)
        } catch (e: Exception) {
            log.error("Failed to set alreadyInitialised", e)
        }
    }

    fun reset() {
        this.setAlreadyInitialised(false)
    }

    override fun initImGui(): List<ImGuiWindow> {
        return listOf(
            ImGuiWindow(
                ImGuiKutilsThemeNoTransparent(),
                Text.literal("Script Manager"),
                WindowRenderer { renderScriptList() },
                false,
                ImGuiWindowFlags.None
            ),
            ImGuiWindow(
                ImGuiKutilsThemeNoTransparent(),
                Text.literal("Script Editor"),
                WindowRenderer { renderScriptEditor() },
                false,
                ImGuiWindowFlags.None
            )
        )
    }

    // TODO: Improve lua module management in the ui
    //  - [ ] Add/Remove/Rename scripts
    //  - [ ] Keyboard shortcuts
    //  labels: enhancement
    private fun renderScriptList() {
        scriptManager.getScriptMetadata().forEach { (fileName, metadata) ->
            ImGui.pushID(fileName)

            ImGui.setWindowSize(300f, 400f, ImGuiCond.FirstUseEver)
            ImGui.setWindowPos(50f, 50f, ImGuiCond.FirstUseEver)

            if (ImGui.checkbox("##enabled", metadata.isEnabled)) {
                if (metadata.isEnabled) scriptManager.enableScript(fileName)
                else scriptManager.disableScript(fileName)
            }

            ImGui.sameLine()
            if (ImGui.button("Reload")) {
                scriptManager.reloadScript(fileName)
            }

            ImGui.sameLine()
            if (ImGui.button("Edit")) {
                selectedScript = fileName
                editor.text = scriptManager.getScriptContent(fileName)
            }

            ImGui.sameLine()
            ImGui.text(metadata.displayName)

            ImGui.popID()
        }
    }

    private fun renderScriptEditor() {
        ImGui.setWindowSize(600f, 500f, ImGuiCond.FirstUseEver)
        ImGui.setWindowPos(470f, 50f, ImGuiCond.FirstUseEver)

        ImGui.beginChild("editor", 0f, 0f, true)

        // Begin disabled group if no script is selected
        if (selectedScript == null) {
            ImGui.beginDisabled()
        }

        editor.render("##editor")

        // If we have a selected script, handle content changes
        selectedScript?.let { currentScript ->
            val currentContent = editor.text
            if (currentContent != scriptManager.getScriptContent(currentScript)) {
                scriptManager.saveScriptContent(currentScript, currentContent)
            }
        }

        if (selectedScript == null) {
            ImGui.endDisabled()
        }

        ImGui.endChild()

        // Control buttons
        ImGui.separator()
        if (selectedScript != null) {
            if (ImGui.button("Close")) {
                selectedScript = null
            }
        }
    }
}