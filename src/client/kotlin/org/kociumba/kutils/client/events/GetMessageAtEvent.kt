package org.kociumba.kutils.client.events

import net.minecraft.text.OrderedText
import net.minecraft.text.Text

data class GetMessageAtEvent(
    val content: Text?,
    val x: Double,
    val y: Double
) : KutilsEvent() {
    companion object : KutilsEventBus<GetMessageAtEvent>()
}