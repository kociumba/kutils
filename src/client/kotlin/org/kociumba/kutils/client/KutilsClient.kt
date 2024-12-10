package org.kociumba.kutils.client

import gg.essential.universal.UChat
import gg.essential.universal.UMinecraft
import gg.essential.universal.UScreen
import imgui.ImFont
import imgui.ImGui
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.nbt.NbtCompound
import org.kociumba.kutils.client.bazaar.BazaarAPI
import org.kociumba.kutils.client.bazaar.bazaarUI
import org.kociumba.kutils.client.hud.performanceHud
import org.kociumba.kutils.log
import org.lwjgl.glfw.GLFW
import xyz.breadloaf.imguimc.Imguimc

var c: ConfigGUI = ConfigGUI()
var displayingCalc = false

//val mainWindow = UMinecraft.getMinecraft().window
var largeRoboto: ImFont? = null

@Environment(EnvType.CLIENT)
class KutilsClient : ClientModInitializer {

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
                GLFW.GLFW_MOUSE_BUTTON_5,
                "kutils"
            )
        )

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

        if (c.displayPerformanceHud) {
            Imguimc.pushRenderable(performanceHud)
        }

        // Font loading also seems to be fucked
//        Thread {
//            Thread.sleep(5000)
//            if (largeRoboto == null) {
//                try {
//                    val io = ImGui.getIO()
//                    val fontAtlas = io.fonts
//                    largeRoboto = fontAtlas.addFontFromFileTTF("/assets/fonts/Roboto-Regular.ttf", 24f)
//                } catch (e: Exception) {
//                    log.error("Failed to load font", e)
//                }
//            }
//        }.start()

        // Seems like imgui saving is fucked in java/imgui
//        Runtime.getRuntime().addShutdownHook(Thread {
//            ImGui.saveIniSettingsToDisk("config/kutils.ini")
//        })
//
//        ImGui.loadIniSettingsFromDisk("config/kutils.ini")

        log.info("kutils fully loaded")
    }
}