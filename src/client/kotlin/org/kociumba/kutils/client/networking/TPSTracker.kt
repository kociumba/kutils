package org.kociumba.kutils.client.networking

import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket
import org.kociumba.kutils.client.events.GameJoinEvent
import org.kociumba.kutils.client.events.PacketReceiveEvent
import java.util.Arrays

class TPSTracker {

    companion object {
        val INSTANCE = TPSTracker()
    }

    private val tickRates = FloatArray(20)
    private var nextIndex = 0
    private var timeLastTimeUpdate: Long = -1
    private var timeGameJoined: Long = 0

    var serverProvidedTps = -1f

    init {
        PacketReceiveEvent.subscribe { event ->
            if (event.packet is WorldTimeUpdateS2CPacket) {
                val now = System.currentTimeMillis()
                val timeElapsed = (now - timeLastTimeUpdate) / 1000.0f
                tickRates[nextIndex] = clamp(20.0f / timeElapsed, 0.0f, 20.0f)
                nextIndex = (nextIndex + 1) % tickRates.size
                timeLastTimeUpdate = now
            }
        }

        GameJoinEvent.subscribe {
            serverProvidedTps = -1f
            Arrays.fill(tickRates, 0f)
            nextIndex = 0
            timeGameJoined = System.currentTimeMillis()
            timeLastTimeUpdate = timeGameJoined
        }
    }

    fun getTickRate(): Float {
        val minecraft = MinecraftClient.getInstance()
        if (serverProvidedTps != -1f && !minecraft.world?.isClient!!) {
            return serverProvidedTps
        }
        if (minecraft.player == null) return 0f
        if (System.currentTimeMillis() - timeGameJoined < 4000) return 20f

        var numTicks = 0
        var sumTickRates = 0.0f
        for (tickRate in tickRates) {
            if (tickRate > 0) {
                sumTickRates += tickRate
                numTicks++
            }
        }
        return sumTickRates / numTicks
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return if (value < min) min else Math.min(value, max)
    }
}