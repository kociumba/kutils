package org.kociumba.kutils.client.hud

import com.sun.management.OperatingSystemMXBean
import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import org.kociumba.kutils.client.c
import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import org.kociumba.kutils.client.imgui.coloredText
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme
import java.lang.management.ManagementFactory

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

        ImGui.setNextWindowPos(15.0f, 35.0f, ImGuiCond.FirstUseEver)
//        setNextWindowPositionRelative(15.0f, 35.0f, ImGuiCond.Once)

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
                "Performance Metrics",
                windowFlags
            )
        ) {

            ImGui.text("CPU Load: ")
            ImGui.sameLine()
            coloredText(cpuColor, "%.2f%%".format(cpuLoad) + "%")

//        ImGui.text("GPU Load: ")
//        ImGui.sameLine()
//        coloredText(gpuColor,"%.2f%%".format(gpuLoad) + "%")

//        ImGui.text("Used Memory: %d MB".format(totalMemory))

            ImGui.text("Used Memory: ")
            ImGui.sameLine()
            coloredText(memoryColor, "%.2f%%".format(memoryUsage) + "%")

            ImGui.text("Allocated Memory: %d MB / %d MB".format(usedMemory, maxMemory))

            ImGui.end()
        }

//        ImGui.popFont()
//        }
    }
}