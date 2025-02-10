package org.kociumba.kutils.client.events

import net.minecraft.network.packet.Packet

data class PacketReceiveEvent(val packet: Packet<*>) : KutilsEvent() {
    companion object : KutilsEventBus<PacketReceiveEvent>()
}
