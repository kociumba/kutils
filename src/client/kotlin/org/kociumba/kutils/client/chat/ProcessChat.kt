package org.kociumba.kutils.client.chat

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.gui.hud.ChatHudLine
import org.kociumba.kutils.client.chatHud
import org.kociumba.kutils.client.events.ChatMessageEvent
import org.kociumba.kutils.mixin.client.ChatHudAccessor
import org.kociumba.kutils.log

val ChatHud.messages: MutableList<ChatHudLine>
    get() = (this as ChatHudAccessor).getMessages_kutils()

val urlRegex = "https://[^. ]+\\.[^ ]+(\\.?( |$))".toRegex()

fun registerChatMessageHandler() {
    ChatMessageEvent.subscribe { event ->
//        log.info("New chat message received: ${event.message}")
        chatHud?.messages?.forEach { chatLine ->
            // Assuming chatLine contains a `content` property as a String
            val lineLinks = detectLinks(chatLine.content.string)
            // Handle links from `lineLinks` if needed
//            log.info("Links detected in chat line: $lineLinks")
        }


    }
}

fun detectLinks(message: String): List<String> {
    return urlRegex.findAll(message).map { it.value }.toList()
}