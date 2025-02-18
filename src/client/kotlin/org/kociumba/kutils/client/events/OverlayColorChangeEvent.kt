package org.kociumba.kutils.client.events

import java.awt.Color

data class OverlayColorChangeEvent(val newColor: Color) : KutilsEvent() {
    companion object : KutilsEventBus<OverlayColorChangeEvent>()
}