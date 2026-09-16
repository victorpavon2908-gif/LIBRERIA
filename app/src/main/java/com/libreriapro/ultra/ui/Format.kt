package com.libreriapro.ultra.ui

import java.util.Locale

/** Money and quantity formatting shared by every screen. */
object Fmt {

    fun money(amount: Double, symbol: String = "C$"): String =
        "$symbol " + String.format(Locale.getDefault(), "%,.2f", amount)

    fun decimal(amount: Double): String = String.format(Locale.getDefault(), "%.2f", amount)

    fun number(value: Int): String = String.format(Locale.getDefault(), "%,d", value)

    fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

    /** Accepts "12.5" and "12,5" so any tablet keyboard can be used. */
    fun parseAmount(text: String): Double? =
        text.trim().replace(",", ".").toDoubleOrNull()

    fun parseInt(text: String): Int? = text.trim().toIntOrNull()
}