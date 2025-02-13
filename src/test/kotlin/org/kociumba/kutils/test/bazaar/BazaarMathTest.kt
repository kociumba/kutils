package org.kociumba.kutils.test.bazaar

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kociumba.kutils.client.bazaar.BazaarMath
import org.kociumba.kutils.client.bazaar.BuyInfo
import org.kociumba.kutils.client.bazaar.Product
import org.kociumba.kutils.client.bazaar.QuickStatus
import org.kociumba.kutils.client.bazaar.SellInfo
import org.kociumba.kutils.client.bazaar.SmoothingTypes

class BazaarMathTest {
    @Test
    fun testGetPrediction() {
        val product = Product(
            product_id = "TEST_PRODUCT",
            sell_summary = listOf(SellInfo(100, 10.0, 5)),
            buy_summary = listOf(BuyInfo(100, 11.0, 5)),
            quick_status = QuickStatus(
                productId = "TEST_PRODUCT",
                sellPrice = 10.0,
                sellVolume = 500,
                sellMovingWeek = 10000,
                sellOrders = 50,
                buyPrice = 11.0,
                buyVolume = 600,
                buyMovingWeek = 12000,
                buyOrders = 60
            )
        )

        val smoothingTypes = listOf(
            SmoothingTypes.SIGMOID,
            SmoothingTypes.TANH,
            SmoothingTypes.SATURATING,
            SmoothingTypes.PIECEWISE
        )

        for (s in smoothingTypes) {
            val result = BazaarMath.getPrediction(product, s)
            println("Prediction: ${result.prediction}, Confidence: ${result.confidence}")
            assert(result.confidence in 0.0..100.0) { "Confidence should be between 0 and 100" }
        }
    }
}
