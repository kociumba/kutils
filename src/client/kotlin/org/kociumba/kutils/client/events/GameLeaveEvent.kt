package org.kociumba.kutils.client.events

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler

data class GameLeaveEvent(val handler: ClientPlayNetworkHandler, var packet: MinecraftClient?) : KutilsEvent() {
    companion object : KutilsEventBus<GameLeaveEvent>()
}
