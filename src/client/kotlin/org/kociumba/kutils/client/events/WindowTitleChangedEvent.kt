package org.kociumba.kutils.client.events

data class WindowTitleChangedEvent(val newTitle: String) : KutilsEvent() {
    companion object : KutilsEventBus<WindowTitleChangedEvent>()
}