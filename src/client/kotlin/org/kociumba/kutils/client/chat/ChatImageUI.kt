package org.kociumba.kutils.client.chat

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
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
        val x = mouseX.coerceIn(0f, client.window.width - width)
        val y = (mouseY - height).coerceIn(0f, client.window.height - height) // Subtract height to position bottom-left at mouse
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
                    imageCache.remove(url)
                }
            }
        }
    }

    /**
     * copy of the @BazaarUI error popup, adapted for this
     */
    fun errorPopup(e: ImImage) {
        val warn = hexToImColor("#f5c6c6")
        val link = hexToImColor("#8200ff")

        ImGui.pushStyleColor(ImGuiCol.PopupBg, warn.r, warn.g, warn.b, warn.a)
        ImGui.pushStyleColor(ImGuiCol.Text, 0f, 0f, 0f, 1f)
        ImGui.openPopup("##errorPopup")
        if (ImGui.beginPopup("##errorPopup")) {
            ImGui.text("${e.errorMessage}\nPlease report this here:")
//            ImGui.sameLine()
            ImGui.textColored(
                link.r,
                link.g,
                link.b,
                link.a,
                "https://github.com/kociumba/kutils/issues (clickable link)"
            )
            if (ImGui.isItemClicked()) {
                val url = URI("https://github.com/kociumba/kutils/issues")
                try {
                    Util.getOperatingSystem().open(url) // minecraft does some weird stuff
                } catch (e: Exception) {
                    log.error("Failed to open the github issues page $e")
                }
            }

            ImGui.pushStyleColor(ImGuiCol.Text, 1f, 1f, 1f, 1f)
            if (ImGui.button("Close")) {
                bazaarUI.error = null // reset
                ImGui.closeCurrentPopup()
            }
            ImGui.popStyleColor()
            ImGui.endPopup()
        }
        ImGui.popStyleColor()
        ImGui.popStyleColor()
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
                            val maxWidth = client.window.width * 0.5f
                            val maxHeight = client.window.height * 0.5f
                            val scale = minOf(
                                1f,
                                maxWidth / img.width,
                                maxHeight / img.height
                            )

                            val finalWidth = img.width * scale
                            val finalHeight = img.height * scale

                            val (x, y) = calculatePositionNextToMouse(mouseX, mouseY, finalWidth, finalHeight)

                            // offset the image a bit from the mouse
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

                    // TODO: unfuck the error handling here
                    LoadingState.ERROR -> {
                        errorPopup(img)
                    }

                    else -> {} // Handle IDLE state
                }
            }
        }

        ImGui.end()
    }
}