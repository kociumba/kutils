package org.kociumba.kutils.client.notes

import java.io.File

// Represents a note with a title, content, and overlay status.
data class Note(
    var title: String,
    var content: String,
    var isOverlayed: Boolean = false
) {
    var path: String = "config/kutils/notes/${title.replace(" ", "_")}.md"
        private set

    fun save() {
        val file = File(path)
        file.parentFile.mkdirs() // Ensure the directory exists.
        file.writeText(content)
    }

    fun load(): Boolean {
        val file = File(path)
        if (file.exists()) {
            content = file.readText()
            return true
        }
        return false
    }
}

object NoteData {
    private const val notesDir = "config/kutils/notes"
    val notes: MutableList<Note> = mutableListOf()

    /**
     * Add a new note and return a reference to it.
     */
    fun createNote(title: String, content: String = ""): Note {
        if (notes.any { it.title == title }) {
            throw IllegalArgumentException("A note with the title '$title' already exists.")
        }
        val note = Note(title, content)
        notes.add(note)
        return note
    }

    /**
     * Delete a note by its reference.
     */
    fun deleteNote(note: Note) {
        notes.remove(note)
        File(note.path).delete()
    }

    /**
     * Save all notes to their respective files.
     */
    fun saveNotes() {
        notes.forEach { it.save() }
    }

    /**
     * Load notes from the disk and return references.
     */
    fun loadNotes() {
        notes.clear()
        val directory = File(notesDir)
        if (directory.exists()) {
            directory.listFiles { file -> file.extension == "md" }?.forEach { file ->
                val title = file.nameWithoutExtension.replace("_", " ")
                val content = file.readText()
                notes.add(Note(title, content))
            }
        }
    }

    /**
     * Toggle the isOverlayed state of the given note.
     */
    fun toggleOverlay(note: Note) {
        val index = notes.indexOf(note)
        if (index != -1) {
            notes[index].isOverlayed = !notes[index].isOverlayed
        } else {
            throw IllegalArgumentException("Note not found in the list.")
        }
    }
}