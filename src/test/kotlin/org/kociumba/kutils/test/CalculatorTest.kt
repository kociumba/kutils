package org.kociumba.kutils.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kociumba.kutils.client.Calculator

class CalculatorTest {
    @Test
    fun testCalculator() {
        val tokens = Calculator.lex("10m*7k/50s")
        val rpnTokens = Calculator.shuntingYard(tokens)
        val result = Calculator.evaluate(rpnTokens)
        assertEquals(21875000, result.toInt())
    }
}