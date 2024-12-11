package org.kociumba.kutils.client

import java.awt.Color

interface OverlayTextureListener {
    companion object {
        private val listeners = mutableListOf<OverlayTextureListener>()

        fun register(listener: OverlayTextureListener) {
            listeners.add(listener)
        }

        fun notifyColorChanged(newColor: Color) {
            listeners.forEach { it.onColorChanged(newColor) }
        }
    }

    fun onColorChanged(newColor: Color)
}

interface WindowTitleListener {
    companion object {
        private val listeners = mutableListOf<WindowTitleListener>()

        fun register(listener: WindowTitleListener) {
            listeners.add(listener)
        }

        fun notifyWindowChanged(newTitle: String) {
            listeners.forEach { it.onWindowTitleChanged(newTitle) }
        }
    }

    fun onWindowTitleChanged(newTitle: String)
}