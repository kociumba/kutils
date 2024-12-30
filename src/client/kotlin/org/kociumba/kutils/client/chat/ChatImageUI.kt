package org.kociumba.kutils.client.chat

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiPopupFlags
import imgui.flag.ImGuiWindowFlags
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.text.Text
import net.minecraft.util.Util
import org.kociumba.kutils.client.bazaar.bazaarUI
import org.kociumba.kutils.client.client
import org.kociumba.kutils.client.events.GetMessageAtEvent
import org.kociumba.kutils.client.imgui.*
import org.kociumba.kutils.log
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object ChatImageUI : Renderable {
    override fun getName(): String? {
        return "ChatImagePreviewUI"
    }

    override fun getTheme(): Theme? {
        return ImGuiKutilsTransparentTheme()
    }

    private var hoveredLink: String? = null
    private val imageCache = ConcurrentHashMap<String, ImImage>()

    // Configuration
    private val imageExtensions = listOf("jpg", "png", "gif", "jpeg", "webp", "svg")
    private val urlRegex = "https://[^. ]+\\.[^ ]+(\\.?( |$))".toRegex()

    fun initialize() {
        var currentContent: Text? = null

        GetMessageAtEvent.subscribe { event ->
            currentContent = event.content
        }

        ScreenEvents.AFTER_INIT.register { client, screen, scaledWidth, scaledHeight ->
            ScreenEvents.afterRender(screen).register { screen, drawContext, mouseX, mouseY, tickDelta ->
                if (screen !is ChatScreen) {
                    hoveredLink = null
                    imageCache.clear()
                    return@register
                }

                // triggers the event, very hacky but works 🤷
                client.inGameHud.chatHud.getTextStyleAt(mouseX.toDouble(), mouseY.toDouble())

                val text = currentContent?.string ?: run {
                    hoveredLink = null
                    return@register
                }

                hoveredLink = urlRegex.find(text)?.value

                hoveredLink?.let { url ->
                    if (isImageUrl(url)) {
                        loadImage(url)
                    }
                }
            }
        }
    }

    private fun isImageUrl(url: String): Boolean {
        // this is the proper check, but it limits us to links without metadata properties
//        return url.substringAfterLast('.').lowercase() in imageExtensions
        // check if contains, technically allows spoofing images, but nobody is doing that just without a reason
        imageExtensions.forEach { ext -> if (url.contains(ext)) return true }
        return false
    }

    private fun calculatePositionNextToMouse(
        mouseX: Float,
        mouseY: Float,
        width: Float,
        height: Float
    ): Pair<Float, Float> {
        var x = 0f
        var y = 0f
        try {
            x = mouseX.coerceIn(0f, client.window.width - width)
            y = (mouseY - height).coerceIn(
                0f,
                client.window.height - height
            ) // Subtract height to position bottom-left at mouse
        } catch (e: Exception) {
            log.error("Failed to calculate position for image preview", e)
        }
        return Pair(x, y)
    }

    private fun loadImage(url: String) {
        if (imageCache.containsKey(url)) return

        val image = ImImage()
        imageCache[url] = image

        if (url.substringAfterLast('.').lowercase() == "svg") {
            image.loadSVGFromURL(url)
        } else {
            image.loadImageFromURL(url) { success ->
                if (!success) {
                    log.error("Failed to load image from url $url")
//                    imageCache.remove(url)
                }
            }
        }
    }

    override fun render() {
        ImGui.setNextWindowPos(
            client.window.x.toFloat(),
            client.window.y.toFloat(),
            ImGuiCond.Always
        )

        ImGui.setNextWindowSize(
            client.window.framebufferWidth.toFloat(),
            client.window.framebufferHeight.toFloat(),
            ImGuiCond.Always
        )

        ImGui.begin(
            "ChatImagePreviewUI",
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

        hoveredLink?.let { url ->
            imageCache[url]?.let { img ->
                val mouseX = client.mouse.x.toFloat()
                val mouseY = client.mouse.y.toFloat()

                when (img.loadingState) {
                    LoadingState.LOADING -> {
                        val buttonHeight = ImGui.getFrameHeight()
                        val spinnerSize = buttonHeight * 1.5f - 1f
                        val (x, y) = calculatePositionNextToMouse(
                            mouseX,
                            mouseY,
                            spinnerSize * 2,
                            spinnerSize * 2
                        )
                        ImGui.setCursorPos(x, y)
                        spinner(
                            "##loading",
                            spinnerSize,
                            buttonHeight / 4f,
                            ImGui.getColorU32(1f, 1f, 1f, 1f)
                        )
                    }

                    LoadingState.LOADED -> {
                        if (img.isValid) {
                            // First calculate the maximum available space
                            val maxWidth = client.window.width * 0.5f  // Keep 50% limit
                            val maxHeight = client.window.height * 0.5f // Keep 50% limit

                            // Ensure max dimensions never exceed window size
                            val safeMaxWidth = minOf(maxWidth, client.window.width.toFloat())
                            val safeMaxHeight = minOf(maxHeight, client.window.height.toFloat())

                            // Calculate scale while ensuring the image fits in the window
                            val scale = minOf(
                                1f,
                                safeMaxWidth / img.width,
                                safeMaxHeight / img.height
                            )

                            val finalWidth = img.width * scale
                            val finalHeight = img.height * scale

                            // Double-check that our final dimensions don't exceed window size
                            val displayWidth = minOf(finalWidth, client.window.width.toFloat())
                            val displayHeight = minOf(finalHeight, client.window.height.toFloat())

                            val (x, y) = calculatePositionNextToMouse(mouseX, mouseY, displayWidth, displayHeight)

                            ImGui.setCursorPos(x + 10, y - 10)
//                            ImGui.setNextWindowPos(x, y, ImGuiCond.Always)

                            // put this in a window later, so the preview can exist outside minecraft
                            if (client.currentScreen is ChatScreen) {
//                                ImGui.begin("##imagePrev", ImGuiWindowFlags.NoDecoration
//                                        or ImGuiWindowFlags.NoNav
//                                        or ImGuiWindowFlags.NoInputs
//                                        or ImGuiWindowFlags.NoTitleBar
//                                        or ImGuiWindowFlags.AlwaysAutoResize
//                                )
//                                ImGui.text("size: ${finalWidth}x${finalHeight}") //can display image info when in window
                                ImGui.image(img.glID, finalWidth, finalHeight)
//                                ImGui.end()
                            }
                        }
                    }

                    // done error handling works now
                    LoadingState.ERROR -> {
                        val mouseX = client.mouse.x.toFloat()
                        val mouseY = client.mouse.y.toFloat()

                        val errorText = "Failed to load image: ${img.errorMessage}"
                        val textSize = ImGui.calcTextSize(errorText)
                        val padding = 8f

                        val (x, y) = calculatePositionNextToMouse(
                            mouseX,
                            mouseY,
                            textSize.x + padding * 2,
                            textSize.y + padding * 2
                        )

                        // Draw error background
                        ImGui.setCursorPos(x + 10, y - 10)
                        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0.8f, 0.2f, 0.2f, 0.95f)
                        ImGui.beginChild("##errorMsg", textSize.x + padding * 2, textSize.y + padding * 2, true)
                        ImGui.pushStyleColor(ImGuiCol.Text, 1f, 1f, 1f, 1f)
                        ImGui.setCursorPos(padding, padding)
                        ImGui.text(errorText)
                        ImGui.popStyleColor(2)
                        ImGui.endChild()
                    }

                    else -> {} // Handle IDLE state
                }
            }
        }

        ImGui.end()
    }
}