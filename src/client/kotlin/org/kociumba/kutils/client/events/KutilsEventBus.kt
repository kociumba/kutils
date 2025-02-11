package org.kociumba.kutils.client.events

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Use to implement a listener type to use on the event bus
 */
abstract class KutilsEvent

/**
 * The even bus using KutilsEvent type classes as events
 */
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

    fun subscribeOnce(listener: (T) -> Unit) {
        val hasRun = AtomicBoolean(false)

        val onceOnlyListener: (T) -> Unit = { event ->
            if (hasRun.compareAndSet(false, true)) {
                listener(event)
            }
        }
        subscribe(onceOnlyListener)
    }
}