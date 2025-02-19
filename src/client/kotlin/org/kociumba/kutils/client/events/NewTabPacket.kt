package org.kociumba.kutils.client.events

data class NewTabPacket(var heartbeat: Any) : KutilsEvent() {
    companion object : KutilsEventBus<NewTabPacket>()
}
