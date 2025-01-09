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
import imgui.type.ImString
import org.kociumba.kutils.client.ENTER
import org.lwjgl.glfw.GLFW

object LuaEditor: ImGuiScreen(Text.literal("Lua Editor"), true) {
    private lateinit var scriptManager: ModuleManager
    private var selectedScript: String? = null
    private lateinit var alreadyInitialisedField: Field
    private val enabledStates = mutableMapOf<String, ImBoolean>()
    private val searchQuery = ImString("", 256)
    private val newScriptName = ImString(64)
    private var showNewScriptPopup = false
    private var scriptToRename: String? = null
    private var showRenamePopup = false
    private val renameScriptInput = ImString(64)

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
    //  - [ ] Add/Remove/Rename/Search scripts
    //  - [x] Keyboard shortcuts
    //  labels: enhancement
    private fun renderScriptList() {
        ImGui.setWindowSize(300f, 400f, ImGuiCond.FirstUseEver)
        ImGui.setWindowPos(50f, 50f, ImGuiCond.FirstUseEver)

        // Add script button and search bar
        if (ImGui.button("New Script") || 
            (ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) && ImGui.isKeyPressed(GLFW.GLFW_KEY_N))) {
            showNewScriptPopup = true
        }
        ImGui.sameLine()
        ImGui.inputText("##search", searchQuery)

        ImGui.separator()

        // Clean up any removed scripts from the states map
        enabledStates.keys.toList().forEach { fileName ->
            if (!scriptManager.getScriptMetadata().containsKey(fileName)) {
                enabledStates.remove(fileName)
            }
        }

        // Filter scripts based on search query
        scriptManager.getScriptMetadata()
            .filter { (fileName, metadata) ->
                searchQuery.get().isEmpty() || 
                metadata.displayName.contains(searchQuery.get(), ignoreCase = true)
            }
            .forEach { (fileName, metadata) ->
                ImGui.pushID(fileName)

                // Get or create the ImBoolean state for this script
                val isEnabled = enabledStates.getOrPut(fileName) { ImBoolean(metadata.isEnabled) }
                // Update the ImBoolean if the metadata changed (e.g. from a reload)
                if (isEnabled.get() != metadata.isEnabled) {
                    isEnabled.set(metadata.isEnabled)
                }

                if (ImGui.checkbox("##enabled", isEnabled)) {
                    if (isEnabled.get()) scriptManager.enableScript(fileName)
                    else scriptManager.disableScript(fileName)
                }

                ImGui.sameLine()
                if (ImGui.selectable(metadata.displayName, selectedScript == fileName)) {
                    selectedScript = fileName
                    editor.text = scriptManager.getScriptContent(fileName)
                }

                // Context menu for additional actions
                if (ImGui.beginPopupContextItem()) {
                    if (ImGui.menuItem("Edit")) {
                        selectedScript = fileName
                        editor.text = scriptManager.getScriptContent(fileName)
                    }
                    if (ImGui.menuItem("Reload")) {
                        scriptManager.reloadScript(fileName)
                    }
                    if (ImGui.menuItem("Rename")) {
                        scriptToRename = fileName
                        renameScriptInput.set(metadata.displayName)
                        showRenamePopup = true
                    }
                    ImGui.separator()
                    if (ImGui.menuItem("Delete")) {
                        scriptManager.deleteScript(fileName)
                        if (selectedScript == fileName) {
                            selectedScript = null
                            editor.text = ""
                        }
                    }
                    ImGui.endPopup()
                }

                ImGui.popID()
            }

        renderNewScriptPopup()
        
        // Open rename popup if flag is set
        if (showRenamePopup) {
            ImGui.openPopup("Rename Script")
            showRenamePopup = false
        }
        renderRenamePopup()
    }

    private fun renderScriptEditor() {
        ImGui.setWindowSize(600f, 500f, ImGuiCond.FirstUseEver)
        ImGui.setWindowPos(470f, 50f, ImGuiCond.FirstUseEver)

        // Handle Ctrl+S for saving
        if (selectedScript != null && ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) && ImGui.isKeyPressed(GLFW.GLFW_KEY_S)) {
            val currentScript = selectedScript!!
            val currentContent = editor.text
            if (currentContent != scriptManager.getScriptContent(currentScript)) {
                scriptManager.saveScriptContent(currentScript, currentContent)
            }
        }

        ImGui.beginChild("editor", 0f, -30f, true)

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

    private fun renderNewScriptPopup() {
        if (showNewScriptPopup) {
            ImGui.openPopup("New Script")
        }

        if (ImGui.beginPopupModal("New Script", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Enter script name:")
            ImGui.inputText("##scriptname", newScriptName)

            fun createNewScript() {
                val name = newScriptName.get().trim()
                if (name.isNotBlank()) {
                    val fileName = if (name.endsWith(".lua")) name else "$name.lua"
                    scriptManager.createScript(fileName)
                    selectedScript = fileName
                    editor.text = scriptManager.getScriptContent(fileName)
                    newScriptName.set("")
                    showNewScriptPopup = false
                    ImGui.closeCurrentPopup()
                }
            }

            if (ImGui.button("Create")) createNewScript()
            if (ImGui.isKeyPressed(ENTER)) createNewScript()

            ImGui.sameLine()
            if (ImGui.button("Cancel")) {
                newScriptName.set("")
                showNewScriptPopup = false
                ImGui.closeCurrentPopup()
            }

            ImGui.endPopup()
        }
    }

    private fun renderRenamePopup() {
        scriptToRename?.let { currentName ->
            if (ImGui.beginPopupModal("Rename Script", ImGuiWindowFlags.AlwaysAutoResize)) {
                ImGui.text("Enter new name:")
                ImGui.inputText("##newname", renameScriptInput)

                fun renameScript() {
                    val newName = renameScriptInput.get().trim()
                    if (newName.isNotBlank()) {
                        val fileName = if (newName.endsWith(".lua")) newName else "$newName.lua"
                        scriptManager.renameScript(currentName, fileName)
                        if (selectedScript == currentName) {
                            selectedScript = fileName
                        }
                        scriptToRename = null
                        renameScriptInput.set("")
                        ImGui.closeCurrentPopup()
                    }
                }

                if (ImGui.button("Rename")) renameScript()
                if (ImGui.isKeyPressed(ENTER)) renameScript()

                ImGui.sameLine()
                if (ImGui.button("Cancel")) {
                    scriptToRename = null
                    renameScriptInput.set("")
                    ImGui.closeCurrentPopup()
                }

                ImGui.endPopup()
            }
        }
    }
}