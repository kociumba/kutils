package org.kociumba.kutils.client.bazaar

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.kociumba.kutils.log
import kotlin.math.sign

/**
  * more or less a direct port of [Skydriver](https://www.github.com/kociumba/skydriver) math
  */
@Environment(EnvType.CLIENT)
object BazaarMath {
    private val w1 = 0.1428571429 // Weight for Price Spread
    private val w2 = 0.1428571429 // Weight for Volume Imbalance
    private val w3 = 0.1428571429 // Weight for Order Imbalance
    private val w4 = 0.1428571429 // Weight for Moving Week Trend
    private val w5 = 0.1428571429 // Weight for Top Order Book Pressure
    private val w6 = 0.1428571429 // Weight for Volume Factor
    private val w7 = 0.1428571429 // Weight for Profit Margin Factor

    data class PredictionResult(val prediction: Double, val confidence: Double)

    fun getPrediction(product: Product, s: SmoothingTypes): PredictionResult {
        val ps = BazaarSmoothing.ApplySmoothing(BazaarProfitability.PriceSpread(product), s)
        val vi = BazaarSmoothing.ApplySmoothing(BazaarProfitability.VolumeImbalance(product), s)
        val oi = BazaarSmoothing.ApplySmoothing(BazaarProfitability.OrderImbalance(product), s)
        val mwt = BazaarSmoothing.ApplySmoothing(BazaarProfitability.MovingWeekTrend(product), s)
        val tobp = BazaarSmoothing.ApplySmoothing(BazaarProfitability.TopOrderBookPressure(product), s)
        val vf = BazaarSmoothing.ApplySmoothing(BazaarProfitability.VolumeFactor(product), s)
        val pmf = BazaarSmoothing.ApplySmoothing(BazaarProfitability.ProfitMarginFactor(product), s)

        val prediction = w1 * ps +
                w2 * vi +
                w3 * oi +
                w4 * mwt +
                w5 * tobp +
                w6 * vf +
                w7 * pmf

        // Calculate confidence
        var sameSignCount = 0
        val factors = listOf(ps, vi, oi, mwt, tobp, vf, pmf)
        for (factor in factors) {
            if (prediction.toDouble().sign == factor.toDouble().sign) {
                sameSignCount++
            }
        }
        val confidence = (sameSignCount.toDouble() / factors.size) * 100

        // disabled couse it makes the debug log too big xd
//        log.debug("""
//            Prediction calculated
//            smoothingFunction: $s
//            prediction: $prediction
//            confidence: $confidence
//            priceSpread: $ps
//            volumeImbalance: $vi
//            orderImbalance: $oi
//            movingWeekTrend: $mwt
//            topOrderBookPressure: $tobp
//            volumeFactor: $vf
//            profitMarginFactor: $pmf
//        """.trimIndent())

        return PredictionResult(prediction, confidence)
    }
}

object BazaarSmoothing {
    fun SigmoidSmooth(x: Double, k: Double): Double {
        return 200 / (1 + kotlin.math.exp(-k * x)) - 100
    }

    fun TanhSmooth(x: Double, k: Double): Double {
        return 100 * kotlin.math.tanh(k * x)
    }

    fun SaturatingSmooth(x: Double, k: Double): Double {
        return 100 * x / kotlin.math.sqrt(1 + k * x * x)
        // return x / kotlin.math.sqrt(1 + k * x * x)
    }

    fun PiecewiseSmooth(x: Double, n: Double): Double {
        if (x > 0) {
            return x / Math.pow(1 + Math.pow(x/100, n), 1/n)
        }
        return -x / Math.pow(1 + Math.pow(-x/100, n), 1/n)
    }

    // ApplySmoothing applies the selected smoothing function
    // TODO: adjust steepness based on observed data
    fun ApplySmoothing(x: Double, smoothingType: SmoothingTypes): Double {
        return when (smoothingType) {
            SmoothingTypes.SIGMOID -> SigmoidSmooth(x, 0.1)
            SmoothingTypes.TANH -> TanhSmooth(x, 0.1)
            SmoothingTypes.SATURATING -> SaturatingSmooth(x, 0.01)
            SmoothingTypes.PIECEWISE -> PiecewiseSmooth(x, 2.0)
            else -> x // No smoothing
        }
    }
}

/**
 * profitability prediction math
 */
object BazaarProfitability {
    // PriceSpread calculates the percentage spread between buy and sell prices
    fun PriceSpread(product: Product): Double {
        if (product.quick_status.buyPrice == 0.0 || product.quick_status.sellPrice == 0.0) {
            return 0.0
        }
        return (product.quick_status.buyPrice - product.quick_status.sellPrice) / product.quick_status.sellPrice * 100
    }

    // VolumeImbalance calculates the imbalance between buy and sell volumes
    fun VolumeImbalance(product: Product): Double {
        if (product.quick_status.buyVolume == 0 || product.quick_status.sellVolume == 0) {
            return 0.0
        }
        return ((product.quick_status.buyVolume - product.quick_status.sellVolume) / (product.quick_status.buyVolume + product.quick_status.sellVolume) * 100).toDouble()
    }

    // OrderImbalance calculates the imbalance between buy and sell orders
    fun OrderImbalance(product: Product): Double {
        if (product.quick_status.buyOrders == 0 || product.quick_status.sellOrders == 0) {
            return 0.0
        }
        return ((product.quick_status.buyOrders - product.quick_status.sellOrders) / (product.quick_status.buyOrders + product.quick_status.sellOrders) * 100).toDouble()
    }

    // MovingWeekTrend calculates the trend based on the past week's activity
    //
    // handle division by 0 couse it's causing errors
    fun MovingWeekTrend(product: Product): Double {
        if (product.quick_status.buyMovingWeek == 0.toLong() || product.quick_status.sellMovingWeek == 0.toLong()) {
            return 0.0
        }
        return ((product.quick_status.buyMovingWeek - product.quick_status.sellMovingWeek) / (product.quick_status.buyMovingWeek + product.quick_status.sellMovingWeek) * 100).toDouble()
    }

    // TopOrderBookPressure calculates the pressure from the visible orders
    fun TopOrderBookPressure(product: Product): Double {
        var buyPressure = 0.0
        var sellPressure = 0.0
        product.buy_summary.forEach { buy ->
            buyPressure += buy.amount.toDouble() * buy.pricePerUnit
        }
        product.sell_summary.forEach { sell ->
            sellPressure += sell.amount.toDouble() * sell.pricePerUnit
        }
        return (buyPressure - sellPressure) / (buyPressure + sellPressure) * 100
    }

    // VolumeFactor calculates a factor based on weekly buy and sell volumes
    fun VolumeFactor(product: Product): Double {
        return (product.quick_status.buyMovingWeek + product.quick_status.sellMovingWeek).toDouble()
    }

    // ProfitMarginFactor calculates a factor based on the profit margin as a percentage of the sell price
    fun ProfitMarginFactor(product: Product): Double {
        val profitMargin = product.quick_status.buyPrice - product.quick_status.sellPrice
        val profitMarginPercentage = profitMargin / product.quick_status.sellPrice

        // Linear interpolation between low and high thresholds
        return profitMarginPercentage
    }
}