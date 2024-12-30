package org.kociumba.kutils.client

import gg.essential.universal.UScreen
import gg.essential.universal.utils.MCMinecraft
import imgui.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.kociumba.kutils.client.bazaar.bazaarUI
import org.kociumba.kutils.client.chat.ChatImageUI
import org.kociumba.kutils.client.chat.registerChatMessageHandler
import org.kociumba.kutils.client.hud.hud
import org.kociumba.kutils.client.hud.performanceHud
import org.kociumba.kutils.log
import org.lwjgl.glfw.GLFW
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.imguiInternal.ImguiLoader
import xyz.breadloaf.imguimc.imguiInternal.InitCallback
import java.nio.file.Files
import java.nio.file.StandardCopyOption

var c: ConfigGUI = ConfigGUI()
var displayingCalc = false
var displayTest = false
var client: MinecraftClient = MinecraftClient.getInstance()
var chatHud: ChatHud? = null

//val mainWindow = UMinecraft.getMinecraft().window
var largeRoboto: ImFont? = null

@Environment(EnvType.CLIENT)
class KutilsClient : ClientModInitializer {
    var loadedWindow = false
    var loadedOptions = false

    override fun onInitializeClient() {

        val open: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "Open kutils Menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "kutils"
            )
        )

        val bazaar: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "Open bazaar info (experimental WIP)",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_INSERT,
                "kutils"
            )
        )

        val openCalc: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "Open Calculator (experimental WIP)",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "kutils"
            )
        )

//        val testingKey: KeyBinding = KeyBindingHelper.registerKeyBinding(
//            KeyBinding(
//                "Open testing GUI",
//                InputUtil.Type.KEYSYM,
//                GLFW.GLFW_KEY_G,
//                "kutils"
//            )
//        )

        // Use once, minimize performance impact
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (open.wasPressed()) {
                UScreen.displayScreen(c.gui())
                log.info("kutils config opened")
            }

            while (bazaar.wasPressed()) {
                if (client.currentScreen !is bazaarUI) {
                    bazaarUI.reset()  // Reset some stuff to get around binding limitations
                    UScreen.displayScreen(bazaarUI)
                }
            }

            while (openCalc.wasPressed()) {
                if (!displayingCalc) {
                    displayingCalc = true
                    MinecraftClient.getInstance().gameRenderer.renderBlur(1f)
                    ImCalcUI.reset()
                    Imguimc.pushRenderable(ImCalcUI)
                } else {
                    displayingCalc = false
                    Imguimc.pullRenderable(ImCalcUI)
                }
            }

//            while (testingKey.wasPressed()) {
//                if (!displayTest) {
//                    displayTest = true
//                    Imguimc.pushRenderable(testingGUI)
//                } else {
//                    displayTest = false
//                    Imguimc.pullRenderable(testingGUI)
//                }
//            }

            if (!loadedWindow && c.customWindowTitle != "" && MinecraftClient.getInstance().window != null) {
                loadedWindow = true
                WindowTitleListener.notifyWindowChanged(c.customWindowTitle)
            }

            if (!loadedOptions && c.shouldUseFullbright && MinecraftClient.getInstance().options != null) {
                loadedOptions = true
                MCMinecraft.getInstance().options.gamma.value = 1000.0
            }

            if (chatHud == null) {
                try {
                    chatHud = client.inGameHud.chatHud
                } catch (e: Exception) {
                    log.error("Failed to get chat hud", e)
                }
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            dispatcher.register(ClientCommandManager.literal("kutils").executes { context ->
                var client = MinecraftClient.getInstance()
                client.send {
                    client.setScreenAndRender(c.gui())
                    log.info("kutils config opened by command")
                }
                0 // need this to return something
            })
        }

        if (MinecraftClient.getInstance().isFinishedLoading && c.fontScale != 1.0f) {
            ImGui.getIO().fontGlobalScale = c.fontScale
        }

        if (c.displayPerformanceHud) {
            Imguimc.pushRenderable(performanceHud)
        }

        if (c.displayHud) {
            Imguimc.pushRenderable(hud)
        }

        // no fucking way we have font loading 😎
        val tempDir = Files.createTempDirectory("imgui_fonts")
        val fontPath = tempDir.resolve("CascadiaCode-Regular.ttf")

        // have to unpack the font from the jar, couse imgui can't see inside the jar
        KutilsClient::class.java.getResourceAsStream("/assets/fonts/CascadiaCode-Regular.ttf").use { input ->
            Files.copy(input, fontPath, StandardCopyOption.REPLACE_EXISTING)
            log.info("font extracted to $fontPath")
        }

        /**
         * TODO: actually find a font that works here.
         *  labels: imgui issue
         */
        var fontLoader = InitCallback { io: ImGuiIO, fontAtlas: ImFontAtlas, fontConfig: ImFontConfig ->
            // loads custom font
            fontConfig.oversampleH = 16
            fontConfig.oversampleV = 16
//            io.setFontDefault(fontAtlas.addFontFromFileTTF(fontPath.toAbsolutePath().toString(), 16f, fontConfig))
            fontAtlas.addFontFromFileTTF(fontPath.toAbsolutePath().toString(), 14f, fontConfig)

            // additional init stuff
            io.fontAllowUserScaling = true
            io.configWindowsMoveFromTitleBarOnly = false
            log.info("custom font loaded successfully")
        }
        ImguiLoader.initCallback = fontLoader

        ImguiLoader.iniFileName = "config/imgui/kutils.ini"

        registerChatMessageHandler()

        if (c.shouldPreviewChatImages) {
            Imguimc.pushRenderable(ChatImageUI)
            ChatImageUI.initialize()
        }

        log.info("kutils initial setup done")
    }
}