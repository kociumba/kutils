package org.kociumba.kutils.client

import gg.essential.universal.UScreen
import gg.essential.universal.utils.MCMinecraft
import imgui.ImFont
import imgui.ImGui
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.kociumba.kutils.client.bazaar.bazaarUI
import org.kociumba.kutils.client.hud.hud
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

            if (!loadedWindow && c.customWindowTitle != "" && MinecraftClient.getInstance().window != null) {
                loadedWindow = true
                WindowTitleListener.notifyWindowChanged(c.customWindowTitle)
            }

            if (!loadedOptions && c.shouldUseFullbright && MinecraftClient.getInstance().options != null) {
                loadedOptions = true
                MCMinecraft.getInstance().options.gamma.value = 1000.0
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

        if (c.displayHud) {
            Imguimc.pushRenderable(hud)
        }

        log.info("kutils initial setup done")
    }
}