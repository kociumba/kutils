package org.kociumba.kutils.client.chat

import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.text.HoverEvent
import net.minecraft.text.OrderedText
import net.minecraft.text.Text
import org.kociumba.kutils.client.client
import org.kociumba.kutils.client.events.GetMessageAtEvent
import org.kociumba.kutils.log
import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import org.kociumba.kutils.client.imgui.ImImage
import org.kociumba.kutils.client.imgui.LoadingState
import org.kociumba.kutils.client.imgui.spinner
import java.util.concurrent.ConcurrentHashMap
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme

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
    private val imageExtensions = listOf("jpg", "png", "gif", "jpeg", "svg")
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
//                    log.info("Not a chat screen, resetting hovered link")
                    imageCache.clear()
                    return@register
                }

                // Trigger style check to fire our event
                client.inGameHud.chatHud.getTextStyleAt(mouseX.toDouble(), mouseY.toDouble())

                // Get the text content from our event
                val text = currentContent?.string ?: run {
                    hoveredLink = null
//                    log.info("No text content found")
                    return@register
                }

//                log.info("Message content: $text")

                hoveredLink = urlRegex.find(text)?.value
//                log.info("Found URL: $hoveredLink")

                hoveredLink?.let { url ->
//                    if (isImageUrl(url)) {
//                        log.info("Loading image from $url")
                        loadImage(url)
//                    }
                }
            }
        }
    }

    private fun isImageUrl(url: String): Boolean {
        return url.substringAfterLast('.').lowercase() in imageExtensions
    }

    private fun loadImage(url: String) {
//        if (!isImageUrl(url)) return
        if (imageCache.containsKey(url)) return

        val image = ImImage()
        imageCache[url] = image

        if (url.endsWith(".svg", ignoreCase = true)) {
            image.loadSVGFromURL(url)
        } else {
            image.loadImageFromURL(url) { success ->
                if (!success) {
                    log.error("Failed to load image from url $url")
                    imageCache.remove(url)
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
            client.window.width.toFloat(),
            client.window.height.toFloat(),
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
                when (img.loadingState) {
                    LoadingState.LOADING -> {
                        ImGui.sameLine()
                        val buttonHeight = ImGui.getFrameHeight()
                        spinner(
                            "##loading",
                            buttonHeight / 2 - 1f,
                            buttonHeight / 7f,
                            ImGui.getColorU32(1f, 1f, 1f, 1f)
                        )
                    }

                    LoadingState.LOADED -> {
                        if (img.isValid) {
                            val mouseX = client.mouse.x.toFloat()
                            val mouseY = client.mouse.y.toFloat()

                            val maxWidth = client.window.width * 0.5f
                            val maxHeight = client.window.height * 0.5f
                            val scale = minOf(
                                1f,
                                maxWidth / img.width,
                                maxHeight / img.height
                            )

                            val finalWidth = img.width * scale
                            val finalHeight = img.height * scale

                            val x = mouseX.coerceIn(0f, client.window.width - finalWidth)
                            val y = mouseY.coerceIn(0f, client.window.height - finalHeight)

                            ImGui.setCursorPos(x, y)

                            if (client.currentScreen is ChatScreen) {
                                ImGui.image(img.glID, finalWidth, finalHeight)
                            }
                        }
                    }

                    LoadingState.ERROR -> {
                        ImGui.text("Error: ${img.errorMessage}")
                    }

                    else -> {} // Handle IDLE state
                }
            }
        }

        ImGui.end()
    }
}