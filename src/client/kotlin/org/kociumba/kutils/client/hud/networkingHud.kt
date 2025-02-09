package org.kociumba.kutils.client.hud

import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import org.kociumba.kutils.client.c
import org.kociumba.kutils.client.client
import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import org.kociumba.kutils.client.imgui.coloredText
import org.kociumba.kutils.client.networking.TPSTracker
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme
// TODO: need to investigate this to see if it can get ping properly
import net.minecraft.network.packet.PingPackets

object networkingHud : Renderable {
    fun init() {
        ImGui.getIO().configWindowsMoveFromTitleBarOnly = false
    }

    override fun getName(): String? {
        return "Networking HUD"
    }

    override fun getTheme(): Theme? {
        return ImGuiKutilsTransparentTheme()
    }

    var firstRun = true
    var tracker = TPSTracker.INSTANCE

    fun getTPSColor(tps: Float): String {
        return when {
            tps >= 15 -> "#00FF00" // Green
            tps >= 10 -> "#FFFF00" // Yellow
            tps >= 5 -> "#FFA500" // Orange
            else -> "#FF0000" // Red
        }
    }

    fun getPINGColor(ping: Int): String {
        return when {
            ping < 50 -> "#00FF00" // Green
            ping < 100 -> "#FFFF00" // Yellow
            ping < 200 -> "#FFA500" // Orange
            else -> "#FF0000" // Red:
        }
    }

    override fun render() {
        if (firstRun) {
            init()
            firstRun = !firstRun
        }

        val tps = tracker.getTickRate()
        val tpsColor = getTPSColor(tps)
        val apr: Float = client.networkHandler?.connection?.averagePacketsReceived ?: 0.0f
        val aps: Float = client.networkHandler?.connection?.averagePacketsSent ?: 0.0f
//        val uuid = client.player?.uuid
//        val player = client.networkHandler?.getPlayerListEntry(uuid)
//        val ping =
//            player?.latency
//                ?: 0 // ok so hypixel spoofs ping to 1ms for whatever reason, so i think the only safe solution is to send the 1.20+ ping packets ?
//        val pingColor = getPINGColor(ping)

        ImGui.setNextWindowPos(15.0f, 45.0f, ImGuiCond.FirstUseEver)

        var windowFlags = ImGuiWindowFlags.NoDecoration or
                ImGuiWindowFlags.NoDocking or
                ImGuiWindowFlags.NoTitleBar or
                ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.AlwaysAutoResize or
                ImGuiWindowFlags.NoFocusOnAppearing or
                ImGuiWindowFlags.NoNav

        if (!c.hudIsDraggable) {
            windowFlags = windowFlags or ImGuiWindowFlags.NoInputs
        }

        if (!c.hudHasBackground) {
            windowFlags = windowFlags or ImGuiWindowFlags.NoBackground
        }

        if (ImGui.begin(
                "Networking Metrics",
                windowFlags
            )
        ) {

//            ImGui.text("ping: ")
//            ImGui.sameLine()
//            coloredText(pingColor, "$ping")

            ImGui.text("tps: ")
            ImGui.sameLine()
            coloredText(tpsColor, "%.2f%%".format(tps))

            ImGui.text("apr/aps: %.2f/%.2f".format(apr, aps))

            ImGui.end()
        }
    }
}
