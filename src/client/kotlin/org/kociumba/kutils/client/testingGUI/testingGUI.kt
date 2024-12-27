package org.kociumba.kutils.client.testingGUI

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import org.kociumba.kutils.client.imgui.ImGuiKutilsTheme
import org.kociumba.kutils.client.imgui.ImImage
import org.kociumba.kutils.client.imgui.LoadingState
import org.kociumba.kutils.client.imgui.spinner
import org.kociumba.kutils.log
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme

object testingGUI : Renderable {
    override fun getName(): String? { return "testingGUI" }

    override fun getTheme(): Theme? { return ImGuiKutilsTheme() }

    var input = ImString("", 256)
    var texture: ImImage? = null

    override fun render() {
        ImGui.begin("testingGUI", ImGuiWindowFlags.AlwaysAutoResize)

//        ImGui.image()
        ImGui.text("image path")
        ImGui.inputText("##input", input)
        if (ImGui.button("display image")) {
            texture?.destroyTexture()
            texture = ImImage().apply{
                loadImage(input.get())
            }
            log.info("Loaded texture: valid=${texture?.isValid}, glID=${texture?.glID}, prefix=${texture?.prefix}, width=${texture?.width}, height=${texture?.height}")
        }
        if (ImGui.button("display image from URL")) {
            texture?.destroyTexture()
            texture = ImImage().apply{
                loadImageFromURL(input.get()) { success ->
                    if (success) {
                        log.info("Loaded texture: valid=${texture?.isValid}, glID=${texture?.glID}, prefix=${texture?.prefix}, width=${texture?.width}, height=${texture?.height}")
                    }
                }
            }
        }
        if (ImGui.button("display SVG from file")) {
            texture?.destroyTexture()
            texture = ImImage().apply{
                loadSVGFromURL(input.get())
            }
            log.info("Loaded texture: valid=${texture?.isValid}, glID=${texture?.glID}, prefix=${texture?.prefix}, width=${texture?.width}, height=${texture?.height}")
        }

        texture?.let{ img ->
            when (img.loadingState) {
                LoadingState.IDLE -> {
                    return@let
                }
                LoadingState.LOADING -> {
                    ImGui.sameLine()
                    val buttonHeight = ImGui.getFrameHeight()
                    val radius = buttonHeight / 2 - 1f  // 1 looks the best
                    val thickness = buttonHeight / 7f   // 7 looks the best
                    spinner("##s", radius, thickness, ImGui.getColorU32(1f, 1f, 1f, 1f))
                }
                LoadingState.LOADED -> {
                    if (img.isValid) {
                        ImGui.image(img.glID, img.width.toFloat(), img.height.toFloat())
                    }
                }
                LoadingState.ERROR -> {
                    ImGui.text("error: ${img.errorMessage}")
                }
            }
        }

        ImGui.end()
    }

//    fun getAbstractTexture(imagePath: String, texturePrefix: String): AbstractTexture {
//        var image = NativeImage.read(convertToPng(FileInputStream(imagePath)))
//        var texture = NativeImageBackedTexture(image)
//        val client = MinecraftClient.getInstance()
//        var txID = client.textureManager.registerDynamicTexture("test_image", texture)
//        client.textureManager.bindTexture(txID)
//        return client.textureManager.getTexture(txID)
//    }

}