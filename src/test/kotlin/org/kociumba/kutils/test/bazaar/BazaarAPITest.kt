package org.kociumba.kutils.test.bazaar

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kociumba.kutils.client.bazaar.BazaarAPI

class BazaarAPITest {
    @Test
    fun testBazaarApiFetching() {
        val bazaar = BazaarAPI.getBazaar()
        assertTrue(bazaar.success)
        assertNotNull(bazaar.products)
    }
}