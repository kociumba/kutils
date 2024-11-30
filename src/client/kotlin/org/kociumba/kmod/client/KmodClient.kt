package org.kociumba.kmod.client

import gg.essential.universal.UChat
import gg.essential.universal.UScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.nbt.NbtCompound
import org.kociumba.kmod.log
import org.lwjgl.glfw.GLFW

var c: ConfigGUI = ConfigGUI()

@Environment(EnvType.CLIENT)
class KmodClient : ClientModInitializer {

    override fun onInitializeClient() {
        val open: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "Open Kmod Menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "Kmod"
        ))

        val getInfo: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "Get Info for the item you are holding",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_INSERT,
            "Kmod"
        ))

        // Use once, minimize performance impact
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (open.wasPressed()) {
                UScreen.displayScreen(c.gui()) // Needs to actually save the settings xd
//                UChat.chat("Opened Kmod Menu")
                log.info("kmod config opened")
            }

            while (getInfo.wasPressed()) {
                val inv = client.player?.inventory
                    ?: return@register

                val nbt = inv.mainHandStack?.components
                    ?: NbtCompound()
//                UChat.chat("You are holding $nbt")
                UChat.chat("Damage tint color: ${c.damageTintColor}" +
                        "\nDamage tint switch: ${c.shouldTintDamage}" +
                        "\nTime changer: ${c.shouldChangeTime}" +
                        "\nUser time: ${c.userTime}"
                )
            }

        }

        if (c.shouldTintDamage) {
            log.info("damage tint enabled")
            log.info("color: ${c.damageTintColor}")
        }
//        log.info(rc.tintConfigFile.toString())

        log.info("kmod fully loaded")
    }
}