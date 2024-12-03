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