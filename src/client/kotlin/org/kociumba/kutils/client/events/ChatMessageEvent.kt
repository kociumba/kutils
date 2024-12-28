package org.kociumba.kutils.client.events

abstract class KutilsEvent

data class ChatMessageEvent(val message: String) : KutilsEvent() {
    companion object : KutilsEventBus<ChatMessageEvent>()
}