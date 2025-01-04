package org.kociumba.kutils.client.notes

import imgui.extension.texteditor.TextEditorLanguageDefinition

fun createMarkdownLanguageDefinition(): TextEditorLanguageDefinition {
    val markdown = TextEditorLanguageDefinition()

    // Set language name
    markdown.setName("Markdown")

    // Set token regex strings for Markdown syntax highlighting
    markdown.setTokenRegexStrings(
        mapOf(
            "#+.*" to 1, // Headers

            "\\*\\*[^\\*]+\\*\\*" to 2, // Bold
            "\\*[^\\*]+\\*" to 3, // Italic
            "~~[^~]+~~" to 4, // Strikethrough

            "!\\[.*\\]\\(.*\\)" to 5, // Images
            "\\[.*\\]\\(.*\\)" to 6, // Links

            "`[^`]+`" to 7, // Inline code
            "```.*```" to 8, // Code blocks

            "^> .*" to 9, // Blockquotes

            "^- .*" to 10, // Lists with dashes
            "^\\* .*" to 11, // Lists with asterisks
            "^\\d+\\. .*" to 12, // Ordered lists

            "^(-{3,}|\\*{3,}|_{3,})$" to 13, // Horizontal rules

            "\\\\." to 14 // Escaped characters
        )
    )

    // Set comment syntax
    markdown.setCommentStart("<!--") // HTML-style comments
    markdown.setCommentEnd("-->")
    markdown.setSingleLineComment("") // No single-line comments in Markdown

    // Optional settings
//    markdown.setCaseSensitive(true) // this is private for some odd reason
    markdown.setAutoIdentation(false)

    return markdown
}
