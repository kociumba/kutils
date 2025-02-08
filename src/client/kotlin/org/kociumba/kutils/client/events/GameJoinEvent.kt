package org.kociumba.kutils.client.events

import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket

data class GameJoinEvent(var packet: GameJoinS2CPacket) : KutilsEvent() {
    companion object : KutilsEventBus<GameJoinEvent>()
}
