package org.kociumba.kutils.client.utils

import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.kociumba.kutils.client.client

val kutilsPrefix = "§r§5§o[kutils]§r "
val kutilsIndicator = MessageIndicator(0xa800a8, null, Text.literal("message from kutils"), "kutils")

fun chatError(msg: String) {
    client.inGameHud.chatHud.addMessage(Text.literal("$kutilsPrefix§c$msg§r"), null, kutilsIndicator)
}

fun chatErrorLink(msg: String, url: String) {
    var click = ClickEvent(ClickEvent.Action.OPEN_URL, url)
    client.inGameHud.chatHud.addMessage(
        Text.literal("$kutilsPrefix§c$msg§r").setStyle(Style.EMPTY.withClickEvent(click)), null, kutilsIndicator
    )
}

fun chatInfo(msg: String) {
    client.inGameHud.chatHud.addMessage(Text.literal("$kutilsPrefix$msg§r"), null, kutilsIndicator)
}

fun chatInfoLink(msg: String, url: String) {
    var click = ClickEvent(ClickEvent.Action.OPEN_URL, url)
    client.inGameHud.chatHud.addMessage(
        Text.literal("$kutilsPrefix$msg§r").setStyle(Style.EMPTY.withClickEvent(click)), null, kutilsIndicator
    )
}
