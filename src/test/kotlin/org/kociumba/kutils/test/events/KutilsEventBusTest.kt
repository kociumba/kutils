package org.kociumba.kutils.test.events

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kociumba.kutils.client.events.KutilsEvent
import org.kociumba.kutils.client.events.KutilsEventBus

data class TestEvent(val message: String) : KutilsEvent() {
    companion object : KutilsEventBus<TestEvent>()
}

class KutilsEventBusTest {

    @Test
    fun `basic subscribe and publish test`() {
        var eventReceived = false
        var receivedEventMessage: String? = null

        TestEvent.subscribe { event ->
            eventReceived = true
            receivedEventMessage = event.message
        }

        TestEvent.publish(TestEvent("Hello Event Bus"))

        assertTrue(eventReceived, "Listener should have received the event")
        assertEquals("Hello Event Bus", receivedEventMessage, "Listener should receive correct event message")
    }

    @Test
    fun `multiple subscribe`() {
        var listener1Called = false
        var listener2Called = false

        TestEvent.subscribe { listener1Called = true }
        TestEvent.subscribe { listener2Called = true }

        TestEvent.publish(TestEvent("Test Event"))

        assertTrue(listener1Called, "Listener 1 should have received the event")
        assertTrue(listener2Called, "Listener 2 should have received the event")
    }

    @Test
    fun `subscribeOnce test`() {
        var listenerCallCount = 0

        TestEvent.subscribeOnce { listenerCallCount++ }

        TestEvent.publish(TestEvent("Event 1"))
        TestEvent.publish(TestEvent("Event 2"))
        TestEvent.publish(TestEvent("Event 3"))

        assertEquals(1, listenerCallCount, "subscribeOnce listener should only be called once")
    }
}