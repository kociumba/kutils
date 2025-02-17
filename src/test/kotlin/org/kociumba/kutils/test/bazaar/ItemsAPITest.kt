package org.kociumba.kutils.test.bazaar

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kociumba.kutils.client.bazaar.ItemsAPI

class ItemsAPITest {
    @Test
    fun testItemsApiFetching() {
        val items = ItemsAPI.getItems()
        assertTrue(items.success)
        assertNotNull(items.items)
        assertFalse(items.items.isEmpty())
    }
}