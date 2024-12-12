package org.kociumba.kutils.client.hud

import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme
import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean
import gg.essential.universal.utils.MCMinecraft
import imgui.ImFont
import imgui.ImGui
import imgui.ImGuiIO
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import org.kociumba.kutils.client.imgui.coloredText
import org.kociumba.kutils.client.imgui.setNextWindowPositionRelative
import org.kociumba.kutils.client.largeRoboto
import org.lwjgl.opengl.GL11

/**
 * Simple imgui based system usage hud
 */
object performanceHud : Renderable {
    fun init() {
        ImGui.getIO().configWindowsMoveFromTitleBarOnly = false
    }

    override fun getName(): String? {
        return "Performance HUD"
    }

    override fun getTheme(): Theme? {
        return ImGuiKutilsTransparentTheme()
    }

    val osBean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
    val runtime: Runtime = Runtime.getRuntime()
    var firstRun = true

    fun getUsageColor(usage: Double): String {
        return when {
            usage < 25 -> "#00FF00" // Green
            usage < 50 -> "#FFFF00" // Yellow
            usage < 75 -> "#FFA500" // Orange
            else -> "#FF0000" // Red
        }
    }

    override fun render() {
        if (firstRun) {
            init()
            firstRun = !firstRun
        }

        val cpuLoad = osBean.cpuLoad * 100
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory
//        val gpuLoad = MCMinecraft.getInstance().gpuUtilizationPercentage

        val cpuColor = getUsageColor(cpuLoad)
        val memoryUsage = (usedMemory.toDouble() / maxMemory) * 100
        val memoryColor = getUsageColor(memoryUsage)
//        val gpuColor = getUsageColor(gpuLoad)

//        if (largeRoboto != null) {
//        ImGui.pushFont(largeRoboto)

        ImGui.setNextWindowPos(15.0f, 35.0f, ImGuiCond.Once)
//        setNextWindowPositionRelative(15.0f, 35.0f, ImGuiCond.Once)

        ImGui.begin(
            "Performance Metrics",
            ImGuiWindowFlags.NoDecoration
                    or ImGuiWindowFlags.NoDocking
                    or ImGuiWindowFlags.NoTitleBar
                    or ImGuiWindowFlags.NoResize
                    or ImGuiWindowFlags.AlwaysAutoResize
//                    or ImGuiWindowFlags.NoInputs
                    or ImGuiWindowFlags.NoFocusOnAppearing
                    or ImGuiWindowFlags.NoNav
        )

//        // Invisible button for dragging
//        val windowSize = ImGui.getContentRegionAvail()
//        ImGui.invisibleButton("drag", windowSize.x, windowSize.y)
//        if (ImGui.isMouseDragging(0)) {
//            val mouseDelta = ImGui.getMouseDragDelta(0)
//            ImGui.setWindowPos(ImGui.getWindowPos().x + mouseDelta.x, ImGui.getWindowPos().y + mouseDelta.y)
//            ImGui.resetMouseDragDelta(0)
//        }

        ImGui.text("CPU Load: ")
        ImGui.sameLine()
        coloredText(cpuColor,"%.2f%%".format(cpuLoad) + "%")

//        ImGui.text("GPU Load: ")
//        ImGui.sameLine()
//        coloredText(gpuColor,"%.2f%%".format(gpuLoad) + "%")

//        ImGui.text("Used Memory: %d MB".format(totalMemory))

        ImGui.text("Used Memory: ")
        ImGui.sameLine()
        coloredText(memoryColor,"%.2f%%".format(memoryUsage) + "%")

        ImGui.text("Allocated Memory: %d MB / %d MB".format(usedMemory, maxMemory))

        ImGui.end()

//        ImGui.popFont()
//        }
    }
}