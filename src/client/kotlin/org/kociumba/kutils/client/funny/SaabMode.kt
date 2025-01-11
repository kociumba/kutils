package org.kociumba.kutils.client.funny

import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import org.kociumba.kutils.client.c
import org.kociumba.kutils.client.client
import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import org.kociumba.kutils.client.imgui.ImImage
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme
import kotlin.math.floor
import kotlin.math.max

class SaabMode: Renderable {
    override fun getName(): String? {
        return "Saab Mode"
    }

    override fun getTheme(): Theme? {
        return ImGuiKutilsTransparentTheme()
    }

    lateinit var saab: ImImage
    var ready = false

    private val velocity = 20f      // Base velocity
    private var timeOffset = 0f    // Time offset for initial position
    private var startTime = System.currentTimeMillis()
    private val initialPos = floatArrayOf(0f, 0f)  // Initial x,y position

    private var scaledImageWidth = 0f
    private var scaledImageHeight = 0f

    private var lastWindowWidth = 0f
    private var lastWindowHeight = 0f

    private fun initializeMovement() {
        lastWindowWidth = client.window.scaledWidth.toFloat()
        lastWindowHeight = client.window.scaledHeight.toFloat()

        initialPos[0] = (Math.random() * client.window.scaledWidth).toFloat()
        initialPos[1] = (Math.random() * client.window.scaledHeight).toFloat()
        timeOffset = (Math.random() * 1000).toFloat()
        startTime = System.currentTimeMillis()
    }

    // ama add an image randomizer here later
    fun initialize() {
        saab = ImImage().loadImageFromURL("https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/saab.jpg") { success ->
            ready = success
            if (success) {
                initializeMovement()
            }
        }
    }

    override fun render() {
        val windowWidth = max(1f, client.window.framebufferWidth.toFloat())
        val windowHeight = max(1f, client.window.framebufferHeight.toFloat())

        ImGui.setNextWindowPos(
            client.window.x.toFloat(),
            client.window.y.toFloat(),
            ImGuiCond.Always
        )

        ImGui.setNextWindowSize(
            windowWidth,
            windowHeight,
            ImGuiCond.Always
        )

        ImGui.begin(
            "Saab Mode",
            ImGuiWindowFlags.NoBackground or
                    ImGuiWindowFlags.NoDecoration or
                    ImGuiWindowFlags.NoInputs or
                    ImGuiWindowFlags.NoTitleBar or
                    ImGuiWindowFlags.NoFocusOnAppearing or
                    ImGuiWindowFlags.NoNav or
                    ImGuiWindowFlags.NoMove or
                    ImGuiWindowFlags.NoScrollbar or
                    ImGuiWindowFlags.NoMouseInputs or
                    ImGuiWindowFlags.NoBringToFrontOnFocus
        )

        if (ready && ::saab.isInitialized && c.saabMode) {
            val windowWidth = max(1f, client.window.framebufferWidth.toFloat())
            val windowHeight = max(1f, client.window.framebufferHeight.toFloat())

            if (windowWidth != lastWindowWidth || windowHeight != lastWindowHeight) {
                initialPos[0] = (initialPos[0] / lastWindowWidth) * windowWidth
                initialPos[1] = (initialPos[1] / lastWindowHeight) * windowHeight

                lastWindowWidth = windowWidth
                lastWindowHeight = windowHeight
            }

            val targetHeight = windowHeight * 0.25f

            val aspectRatio = saab.width.toFloat() / saab.height.toFloat()
            scaledImageHeight = targetHeight
            scaledImageWidth = targetHeight * aspectRatio

            val currentTime = (System.currentTimeMillis() - startTime) * 0.01f // speed of animation
            val f = (currentTime + timeOffset) * velocity

            val boundsWidth = windowWidth - scaledImageWidth
            val boundsHeight = windowHeight - scaledImageHeight

            val timeX = initialPos[0] + f
            val timeY = initialPos[1] + f

            val cycleX = floor(timeX / boundsWidth).toInt()
            val cycleY = floor(timeY / boundsHeight).toInt()

            val posInCycleX = timeX % boundsWidth
            val posInCycleY = timeY % boundsHeight

            val x = if ((cycleX and 1) == 1) boundsWidth - posInCycleX else posInCycleX
            val y = if ((cycleY and 1) == 1) boundsHeight - posInCycleY else posInCycleY

            ImGui.setCursorPos(x, y)
            ImGui.image(saab.glID, scaledImageWidth, scaledImageHeight)
        }


        ImGui.end()
    }
}