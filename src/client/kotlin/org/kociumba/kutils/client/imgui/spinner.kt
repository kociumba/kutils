package org.kociumba.kutils.client.imgui

import imgui.ImGui
import imgui.ImVec2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Scalable spinner animation, without using gifs or images
 */
fun spinner(label: String, radius: Float, thickness: Float, color: Int) {
    val pos = ImGui.getCursorScreenPos()
    val size = ImVec2(radius * 2, radius * 2)

    ImGui.dummy(size.x, size.y)

    val drawList = ImGui.getWindowDrawList()
    drawList.pathClear()

    val numSegments = 30
    val start = abs(sin(ImGui.getTime() * 1.8f) * (numSegments - 5))

    val aMin = Math.PI.toFloat() * 2.0f * start / numSegments
    val aMax = Math.PI.toFloat() * 2.0f * (numSegments - 3) / numSegments

    val centre = ImVec2(
        pos.x + size.x * 0.5f,
        pos.y + size.y * 0.5f
    )

    for (i in 0..numSegments) {
        val a = aMin + (i / numSegments.toFloat()) * (aMax - aMin)
        val time = ImGui.getTime() * 8
        drawList.pathLineTo(
            centre.x + cos(a + time).toFloat() * radius,
            centre.y + sin(a + time).toFloat() * radius
        )
    }

    drawList.pathStroke(color, 0, thickness)
}