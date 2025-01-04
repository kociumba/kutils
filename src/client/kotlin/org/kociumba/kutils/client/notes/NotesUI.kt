package org.kociumba.kutils.client.notes

import imgui.ImGui
import imgui.extension.texteditor.TextEditor
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import net.minecraft.text.Text
import org.kociumba.kutils.client.ENTER
import org.kociumba.kutils.client.imgui.ImGuiKutilsThemeNoTransparent
import org.kociumba.kutils.log
import org.lwjgl.glfw.GLFW
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.icons.FontAwesomeIcons
import xyz.breadloaf.imguimc.screen.ImGuiScreen
import xyz.breadloaf.imguimc.screen.ImGuiWindow
import xyz.breadloaf.imguimc.screen.WindowRenderer
import java.lang.reflect.Field

object NotesScreen : ImGuiScreen(Text.literal("notes"), true) {
    private val editor = TextEditor().apply {
        setLanguageDefinition(createMarkdownLanguageDefinition())
    }

    private var selectedNote: Note? = null
    private val searchQuery = ImString("", 256)
    private val newNoteName = ImString(64)
    private var showNewNotePopup = false

    private lateinit var alreadyInitialisedField: Field

    init {
        try {
            alreadyInitialisedField = ImGuiScreen::class.java.getDeclaredField("alreadyInitialised")
            alreadyInitialisedField.isAccessible = true
        } catch (e: Exception) {
            log.error("Failed to get alreadyInitialised field", e)
        }
    }

    private fun setAlreadyInitialised(value: Boolean) {
        try {
            alreadyInitialisedField.setBoolean(this, value)
        } catch (e: Exception) {
            log.error("Failed to set alreadyInitialised", e)
        }
    }

    override fun initImGui(): List<ImGuiWindow> {
        return listOf(
            ImGuiWindow(
                ImGuiKutilsThemeNoTransparent(),
                Text.literal("Notes Editor"),
                WindowRenderer { renderMainWindow() },
                false,
                ImGuiWindowFlags.MenuBar
            )
        )
    }

    private fun renderNewNotePopup() {
        if (showNewNotePopup) {
            ImGui.openPopup("New Note")
        }

        if (ImGui.beginPopupModal("New Note", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Enter note name:")
            ImGui.inputText("##notename", newNoteName)

            fun createNewNote() {
                val name = newNoteName.get()
                if (name.isNotBlank()) {
                    selectedNote = NoteData.createNote(name)
                    editor.text = selectedNote!!.content
                    newNoteName.set("")
                    showNewNotePopup = false
                    ImGui.closeCurrentPopup()
                }
            }

            if (ImGui.button("Create ")) createNewNote()
            if (ImGui.isKeyPressed(ENTER)) createNewNote()

            ImGui.sameLine()
            if (ImGui.button("Cancel ")) {
                newNoteName.set("")
                showNewNotePopup = false
                ImGui.closeCurrentPopup()
            }

            ImGui.endPopup()
        }
    }

    private fun renderMainWindow() {
        ImGui.setWindowSize(800f, 600f, ImGuiCond.FirstUseEver)

        if (ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) && ImGui.isKeyDown(GLFW.GLFW_KEY_N)) showNewNotePopup = true
        if (ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) && ImGui.isKeyDown(GLFW.GLFW_KEY_S)) NoteData.saveNotes()

        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("New Note " + FontAwesomeIcons.Plus, "Ctrl+N")) {
                    showNewNotePopup = true
                }
                if (ImGui.menuItem("Save All " + FontAwesomeIcons.Save, "Ctrl+S")) {
                    NoteData.saveNotes()
                }
                ImGui.endMenu()
            }
            ImGui.endMenuBar()
        }

        ImGui.beginChild("notes_list", 200f, 0f, true)
        ImGui.inputText("##search", searchQuery)

        NoteData.notes.filter {
            searchQuery.get().isEmpty() || it.title.contains(searchQuery.get(), ignoreCase = true)
        }.forEach { note ->
            if (ImGui.selectable(note.title, selectedNote == note)) {
                selectedNote = note
                editor.text = note.content.trimEnd() // no new line on every click xd
            }



            if (ImGui.beginPopupContextItem()) {
                if (ImGui.menuItem("Show as Overlay")) {
                    note.isOverlayed = !note.isOverlayed
                    if (note.isOverlayed) Imguimc.pushRenderableAfterRender(NoteOverlay(note))
                }
                if (ImGui.menuItem("Delete")) {
                    NoteData.deleteNote(note)
                    if (selectedNote == note) {
                        selectedNote = null
                        editor.text = ""
                    }
                }
                ImGui.endPopup()
            }
        }
        ImGui.endChild()

        ImGui.sameLine()

        ImGui.beginChild("editor", 0f, 0f, true)
        selectedNote?.let {
            editor.render("##editor")
            val currentContent = editor.text
            if (currentContent != it.content) {
                it.content = currentContent
            }
        } ?: ImGui.textWrapped("Select or create a note to begin editing.")
        ImGui.endChild()

        renderNewNotePopup()
    }

    fun reset() {
        this.setAlreadyInitialised(false)
    }
}