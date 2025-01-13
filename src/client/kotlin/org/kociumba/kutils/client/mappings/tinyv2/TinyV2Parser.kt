package org.kociumba.kutils.client.mappings.tinyv2

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

object TinyV2Parser {
    private const val HEADER_MARKER = "tiny"
    private const val INDENT_CHAR = '\t'
    private const val ESCAPED_NAMES_PROPERTY = "escaped-names"
    private const val ESCAPED = "\\nr0t"
    private const val TO_ESCAPE = "\\\n\\r\\0\\t"

    @Throws(IOException::class, MappingParseException::class)
    fun visit(file: File, visitor: TinyVisitor) {
        BufferedReader(FileReader(file)).use { reader ->
            val meta = readMetadata(reader)
            val namespaceCount = meta.namespaces.size
            val escapedNames = meta.properties.containsKey(ESCAPED_NAMES_PROPERTY)
            visitor.start(meta)

            var line: String?
            var lastIndent = -1
            val stack = Array(TinyState.entries.size) { TinyState.CLASS }

            while (reader.readLine().also { line = it } != null) {
                val currentIndent = countIndent(line!!)
                if (currentIndent > lastIndent + 1) {
                    throw IllegalArgumentException("Broken indent! Maximum ${lastIndent + 1}, actual $currentIndent")
                }
                if (currentIndent <= lastIndent) {
                    visitor.pop(lastIndent - currentIndent + 1)
                }
                lastIndent = currentIndent

                val parts = line.split(INDENT_CHAR)
                val currentState = TinyState.get(currentIndent, parts[currentIndent])

                if (!currentState.checkPartCount(currentIndent, parts.size, namespaceCount)) {
                    throw IllegalArgumentException("Wrong number of parts for definition of a $currentState!")
                }
                if (!currentState.checkStack(stack, currentIndent)) {
                    throw IllegalStateException("Invalid stack ${stack.contentToString()} for a $currentState at position $currentIndent!")
                }
                stack[currentIndent] = currentState
                currentState.visit(visitor, parts, currentIndent, escapedNames)
            }
            if (lastIndent > -1) {
                visitor.pop(lastIndent + 1)
            }
        }
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun readMetadata(reader: BufferedReader): TinyMetadata {
        val firstLine = reader.readLine() ?: throw IllegalArgumentException("Empty reader!")
        val parts = firstLine.split(INDENT_CHAR)
        if (parts.size < 5 || parts[0] != HEADER_MARKER) {
            throw IllegalArgumentException("Unsupported format!")
        }

        val majorVersion = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid major version!")
        val minorVersion = parts[2].toIntOrNull() ?: throw IllegalArgumentException("Invalid minor version!")

        val properties = mutableMapOf<String, String?>()
        reader.mark(8192)
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            when (countIndent(line!!)) {
                0 -> {
                    reader.reset()
                    return TinyMetadata(majorVersion, minorVersion, parts.subList(3, parts.size), properties)
                }
                1 -> {
                    val elements = line.split(INDENT_CHAR)
                    properties[elements[1]] = if (elements.size == 2) null else elements[2]
                }
                else -> throw IllegalArgumentException("Invalid indent in header! Encountered \"$line\"!")
            }
            reader.mark(8192)
        }
        return TinyMetadata(majorVersion, minorVersion, parts.subList(3, parts.size), properties)
    }

    private fun countIndent(line: String): Int {
        return line.takeWhile { it == INDENT_CHAR }.length
    }

    private enum class TinyState(private val actualParts: Int, private val namespaced: Boolean = true) {
        CLASS(1) {
            override fun checkStack(stack: Array<TinyState>, currentIndent: Int): Boolean {
                return currentIndent == 0
            }

            override fun visit(visitor: TinyVisitor, parts: List<String>, indent: Int, escapedStrings: Boolean) {
                visitor.pushClass(makeGetter(parts, indent, escapedStrings))
            }
        },
        FIELD(2) {
            override fun checkStack(stack: Array<TinyState>, currentIndent: Int): Boolean {
                return currentIndent == 1 && stack[currentIndent - 1] == CLASS
            }

            override fun visit(visitor: TinyVisitor, parts: List<String>, indent: Int, escapedStrings: Boolean) {
                visitor.pushField(makeGetter(parts, indent, escapedStrings), unescapeOpt(parts[indent + 1], escapedStrings))
            }
        },
        METHOD(2) {
            override fun checkStack(stack: Array<TinyState>, currentIndent: Int): Boolean {
                return currentIndent == 1 && stack[currentIndent - 1] == CLASS
            }

            override fun visit(visitor: TinyVisitor, parts: List<String>, indent: Int, escapedStrings: Boolean) {
                visitor.pushMethod(makeGetter(parts, indent, escapedStrings), unescapeOpt(parts[indent + 1], escapedStrings))
            }
        },
        PARAMETER(2) {
            override fun checkStack(stack: Array<TinyState>, currentIndent: Int): Boolean {
                return currentIndent == 2 && stack[currentIndent - 1] == METHOD
            }

            override fun visit(visitor: TinyVisitor, parts: List<String>, indent: Int, escapedStrings: Boolean) {
                visitor.pushParameter(makeGetter(parts, indent, escapedStrings), parts[indent + 1].toInt())
            }
        },
        LOCAL_VARIABLE(4) {
            override fun checkStack(stack: Array<TinyState>, currentIndent: Int): Boolean {
                return currentIndent == 2 && stack[currentIndent - 1] == METHOD
            }

            override fun visit(visitor: TinyVisitor, parts: List<String>, indent: Int, escapedStrings: Boolean) {
                visitor.pushLocalVariable(makeGetter(parts, indent, escapedStrings), parts[indent + 1].toInt(), parts[indent + 2].toInt(), parts[indent + 3].toInt())
            }
        },
        COMMENT(2, false) {
            override fun checkStack(stack: Array<TinyState>, currentIndent: Int): Boolean {
                if (currentIndent == 0) return false
                return when (stack[currentIndent - 1]) {
                    CLASS, METHOD, FIELD, PARAMETER, LOCAL_VARIABLE -> true
                    else -> false
                }
            }

            override fun visit(visitor: TinyVisitor, parts: List<String>, indent: Int, escapedStrings: Boolean) {
                visitor.pushComment(unescape(parts[indent + 1]))
            }
        };

        abstract fun checkStack(stack: Array<TinyState>, currentIndent: Int): Boolean
        abstract fun visit(visitor: TinyVisitor, parts: List<String>, indent: Int, escapedStrings: Boolean)

        fun checkPartCount(indent: Int, partCount: Int, namespaceCount: Int): Boolean {
            return partCount - indent == if (namespaced) namespaceCount + actualParts else actualParts
        }

        fun makeGetter(parts: List<String>, indent: Int, escapedStrings: Boolean): MappingGetter {
            return PartGetter(indent + actualParts, parts, escapedStrings)
        }

        companion object {
            fun get(indent: Int, identifier: String): TinyState {
                return when (identifier) {
                    "c" -> if (indent == 0) CLASS else COMMENT
                    "m" -> METHOD
                    "f" -> FIELD
                    "p" -> PARAMETER
                    "v" -> LOCAL_VARIABLE
                    else -> throw IllegalArgumentException("Invalid identifier \"$identifier\"!")
                }
            }
        }
    }

    private class PartGetter(private val offset: Int, private val parts: List<String>, private val escapedStrings: Boolean) : MappingGetter {
        override fun get(namespace: Int): String {
            var index = offset + namespace
            while (parts[index].isEmpty()) index--
            return unescapeOpt(parts[index], escapedStrings)
        }

        override fun getRaw(namespace: Int): String {
            return unescapeOpt(parts[offset + namespace], escapedStrings)
        }

        override fun getRawNames(): Array<String> {
            return if (!escapedStrings) {
                parts.subList(offset, parts.size).toTypedArray()
            } else {
                Array(parts.size - offset) { i -> unescape(parts[i + offset]) }
            }
        }

        override fun getAllNames(): Array<String> {
            val ret = getRawNames()
            for (i in 1 until ret.size) {
                if (ret[i].isEmpty()) {
                    ret[i] = ret[i - 1]
                }
            }
            return ret
        }
    }

    private fun unescapeOpt(raw: String, escapedStrings: Boolean): String {
        return if (escapedStrings) unescape(raw) else raw
    }

    private fun unescape(str: String): String {
        var pos = str.indexOf('\\')
        if (pos < 0) return str

        val ret = StringBuilder(str.length - 1)
        var start = 0
        do {
            ret.append(str, start, pos)
            pos++
            val type = ESCAPED.indexOf(str[pos])
            if (type < 0) throw RuntimeException("invalid escape character: \\${str[pos]}")
            ret.append(TO_ESCAPE[type])
            start = pos + 1
        } while (str.indexOf('\\', start).also { pos = it } >= 0)
        ret.append(str, start, str.length)
        return ret.toString()
    }
}