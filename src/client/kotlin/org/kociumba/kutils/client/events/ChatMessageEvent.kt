package org.kociumba.kutils.client.events

data class ChatMessageEvent(val message: String) : KutilsEvent() {
    companion object : KutilsEventBus<ChatMessageEvent>()
}