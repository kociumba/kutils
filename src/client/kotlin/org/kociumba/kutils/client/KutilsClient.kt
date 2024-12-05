package org.kociumba.kutils.client

import gg.essential.universal.UChat
import gg.essential.universal.UMinecraft
import gg.essential.universal.UScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.nbt.NbtCompound
import org.kociumba.kutils.client.bazaar.BazaarAPI
import org.kociumba.kutils.client.bazaar.bazaarUI
import org.kociumba.kutils.log
import org.lwjgl.glfw.GLFW

var c: ConfigGUI = ConfigGUI()
val calculatorState = CalculatorState()
//val mainWindow = UMinecraft.getMinecraft().window

@Environment(EnvType.CLIENT)
class KutilsClient : ClientModInitializer {

    override fun onInitializeClient() {

        val open: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "Open kutils Menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "kutils"
        ))

        val getInfo: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "Get Info for the item you are holding",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_INSERT,
            "kutils"
        ))

        val openCalc: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "Open Calculator (experimental WIP)",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_MOD_CAPS_LOCK,
            "kutils"
        ))

//        val calcAndCopy: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding(
//            "Close Calculator and copy the result",
//            InputUtil.Type.KEYSYM,
//            GLFW.GLFW_KEY_ENTER,
//            "kutils"
//        ))

        // Use once, minimize performance impact
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (open.wasPressed()) {
                UScreen.displayScreen(c.gui()) // Needs to actually save the settings xd
//                UChat.chat("Opened kutils Menu")
                log.info("kutils config opened")
            }

            while (getInfo.wasPressed()) {
                val inv = client.player?.inventory
                    ?: return@register

                val nbt = inv.mainHandStack?.components
                    ?: NbtCompound()
//                UChat.chat("You are holding $nbt")
//                log.info("Damage tint color: ${c.damageTintColor}" +
//                        "\nDamage tint switch: ${c.shouldTintDamage}" +
//                        "\nTime changer: ${c.shouldChangeTime}" +
//                        "\nUser time: ${c.userTime}"
//                )
                    if (client.currentScreen !is bazaarUI) {
                        bazaarUI.reset()  // Reset the UI state
                        UScreen.displayScreen(bazaarUI)
                    }
            }

            while (openCalc.wasPressed()) {
                calculatorState.prevScreen = UScreen.Companion.currentScreen
                calculatorState.calcScreen = CalcScreen()
                calculatorState.prevScreen = UScreen.Companion.currentScreen
                UScreen.displayScreen(calculatorState.calcScreen)
            }

//            while (calcAndCopy.wasPressed()) {
//                calculatorState.calcScreen?.calculateAndCopy()
//                UScreen.displayScreen(calculatorState.prevScreen)
//            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            dispatcher.register(ClientCommandManager.literal("kutils").executes { context ->
                var client = MinecraftClient.getInstance()
                client.setScreenAndRender(c.gui())
//                openConfig()
                log.info("kutils config opened by command")
                0 // need this to return something
            })
        }

        if (c.shouldTintDamage) {
            log.info("damage tint enabled")
            log.info("color: ${c.damageTintColor}")
        }
//        log.info(rc.tintConfigFile.toString())

        log.info("kutils fully loaded")
    }
}

class CalculatorState {
    var prevScreen: Screen? = null
    var calcScreen: CalcScreen? = null
}

fun openConfig() {
    UScreen.displayScreen(c.gui())
}