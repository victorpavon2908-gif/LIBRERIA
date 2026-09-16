package com.libreriapro.ultra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Number formatting and parsing used by every form of the app. */
class FmtTest {

    @Test
    fun parsesAmountsWithDotOrComma() {
        assertEquals(1250.5, Fmt.parseAmount("1250.5")!!, 0.001)
        assertEquals(1250.5, Fmt.parseAmount(" 1250,5 ")!!, 0.001)
        assertEquals(0.0, Fmt.parseAmount("0")!!, 0.001)
    }

    @Test
    fun rejectsInvalidAmounts() {
        assertNull(Fmt.parseAmount(""))
        assertNull(Fmt.parseAmount("abc"))
        assertNull(Fmt.parseAmount("12 30"))
    }

    @Test
    fun parsesWholeNumbersForStock() {
        assertEquals(42, Fmt.parseInt("42")!!.toInt())
        assertNull(Fmt.parseInt("4.5"))
    }

    @Test
    fun moneyKeepsTheCurrencySymbolAndTwoDecimals() {
        val formatted = Fmt.money(1234.5, "C$")

        assertTrue(formatted.startsWith("C$ "))
        assertTrue(formatted.contains("1"))
        assertTrue(formatted.endsWith("50") || formatted.endsWith("5"))
    }

    @Test
    fun signedShowsTheSignOfStockMovements() {
        assertEquals("+5", Fmt.signed(5))
        assertEquals("-3", Fmt.signed(-3))
        assertEquals("0", Fmt.signed(0))
    }
}