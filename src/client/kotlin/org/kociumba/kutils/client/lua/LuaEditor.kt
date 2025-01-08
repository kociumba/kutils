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
import imgui.type.ImBoolean

object LuaEditor: ImGuiScreen(Text.literal("Lua Editor"), true) {
    private lateinit var scriptManager: ModuleManager
    private var selectedScript: String? = null
    private lateinit var alreadyInitialisedField: Field
    private val enabledStates = mutableMapOf<String, ImBoolean>()

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
    //  - [x] Make the checkbox work
    //  - [ ] Add/Remove/Rename scripts
    //  - [ ] Keyboard shortcuts
    //  labels: enhancement
    private fun renderScriptList() {
        // Clean up any removed scripts from the states map
        enabledStates.keys.toList().forEach { fileName ->
            if (!scriptManager.getScriptMetadata().containsKey(fileName)) {
                enabledStates.remove(fileName)
            }
        }

        scriptManager.getScriptMetadata().forEach { (fileName, metadata) ->
            ImGui.pushID(fileName)

            ImGui.setWindowSize(300f, 400f, ImGuiCond.FirstUseEver)
            ImGui.setWindowPos(50f, 50f, ImGuiCond.FirstUseEver)

            // Get or create the ImBoolean state for this script
            val isEnabled = enabledStates.getOrPut(fileName) { ImBoolean(metadata.isEnabled) }
            // Update the ImBoolean if the metadata changed (e.g. from a reload)
            if (isEnabled.get() != metadata.isEnabled) {
                isEnabled.set(metadata.isEnabled)
            }

            ImGui.text(metadata.displayName)
            ImGui.sameLine()

            if (ImGui.checkbox("##enabled", isEnabled)) {
                if (isEnabled.get()) scriptManager.enableScript(fileName)
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

            ImGui.popID()
        }
    }

    private fun renderScriptEditor() {
        ImGui.setWindowSize(600f, 500f, ImGuiCond.FirstUseEver)
        ImGui.setWindowPos(470f, 50f, ImGuiCond.FirstUseEver)

        ImGui.beginChild("editor", 0f, -30f, true)  // Reserve space for buttons at bottom

        // Begin disabled group if no script is selected
        if (selectedScript == null) {
            ImGui.beginDisabled()
        }

        editor.render("##editor")

        if (selectedScript == null) {
            ImGui.endDisabled()
        }

        ImGui.endChild()

        // Control buttons
        ImGui.separator()
        if (selectedScript != null) {
            if (ImGui.button("Save")) {
                val currentScript = selectedScript!!
                val currentContent = editor.text
                if (currentContent != scriptManager.getScriptContent(currentScript)) {
                    scriptManager.saveScriptContent(currentScript, currentContent)
                }
            }
            ImGui.sameLine()
            if (ImGui.button("Close")) {
                selectedScript = null
            }
        }
    }
}