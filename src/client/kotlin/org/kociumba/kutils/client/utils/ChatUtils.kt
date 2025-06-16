package org.kociumba.kutils.client.utils

import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.kociumba.kutils.client.client
import java.net.URI

val kutilsPrefix = "§r§5§o[kutils]§r "
val kutilsIndicator = MessageIndicator(0xa800a8, null, Text.literal("message from kutils"), "kutils")

fun chatError(msg: String) {
    client.inGameHud.chatHud.addMessage(Text.literal("$kutilsPrefix§c$msg§r"), null, kutilsIndicator)
}

fun chatErrorLink(msg: String, url: String) {
    var click = ClickEvent.OpenUrl(URI(url))
    client.inGameHud.chatHud.addMessage(
        Text.literal("$kutilsPrefix§c$msg§r").setStyle(Style.EMPTY.withClickEvent(click)), null, kutilsIndicator
    )
}

fun chatInfo(msg: String) {
    client.inGameHud.chatHud.addMessage(Text.literal("$kutilsPrefix$msg§r"), null, kutilsIndicator)
}

fun chatInfoCommand(msg: String, command: String) {
    var click = ClickEvent.RunCommand(command)
    client.inGameHud.chatHud.addMessage(
        Text.literal("$kutilsPrefix$msg§r")
            .setStyle(
                Style.EMPTY.withClickEvent(click).withHoverEvent(HoverEvent.ShowText(Text.literal("runs $command")))
            ),
        null,
        kutilsIndicator
    )
}

fun chatInfoCommand(msgLines: List<String>, command: String) {
    val clickEvent = ClickEvent.RunCommand(command)
    val hoverEvent = HoverEvent.ShowText(Text.literal("Runs: $command"))

    val parent = Text.literal(kutilsPrefix)

    msgLines.forEachIndexed { index, line ->
        val component = Text.literal(line + if (index < msgLines.lastIndex) "\n" else "")
            .setStyle(
                Style.EMPTY
                    .withClickEvent(clickEvent)
                    .withHoverEvent(hoverEvent)
            )
        parent.append(component)
    }

    client.inGameHud.chatHud.addMessage(parent, null, kutilsIndicator)
}

fun chatInfoLink(msg: String, url: String) {
    var click = ClickEvent.OpenUrl(URI(url))
    client.inGameHud.chatHud.addMessage(
        Text.literal("$kutilsPrefix$msg§r").setStyle(
            Style.EMPTY.withClickEvent(click).withHoverEvent(
                HoverEvent.ShowText(
                    Text.literal("opens $url in your browser")
                )
            )
        ), null, kutilsIndicator
    )
}
