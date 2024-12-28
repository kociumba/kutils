package org.kociumba.kutils.client.events

import java.util.concurrent.CopyOnWriteArrayList

open class KutilsEventBus<T : KutilsEvent> {
    private val listeners = CopyOnWriteArrayList<(T) -> Unit>()

    fun subscribe(listener: (T) -> Unit) {
        listeners.add(listener)
    }

    fun publish(event: T) {
        for (listener in listeners) {
            listener(event)
        }
    }
}