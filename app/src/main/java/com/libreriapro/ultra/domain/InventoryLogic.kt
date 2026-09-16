package com.libreriapro.ultra.domain

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Barcode driven inventory rules. Everything here is pure Kotlin (no Android, no
 * database) so it can be unit tested and reused by every screen.
 */
object Inventory {

    /** Hardware scanners often pad the code with spaces, so always normalise it. */
    fun normalizeBarcode(raw: String): String = raw.trim()

    fun findByBarcode(products: List<Product>, rawBarcode: String): Product? {
        val barcode = normalizeBarcode(rawBarcode)
        if (barcode.isEmpty()) return null
        return products.firstOrNull { it.barcode.equals(barcode, ignoreCase = true) }
    }

    /** Stock after a sale: never goes below zero. */
    fun applySale(products: List<Product>, lines: List<SaleLine>): List<Product> =
        products.map { product ->
            val sold = lines.filter { it.productId == product.id }.sumOf { it.qty }
            if (sold > 0) product.copy(stock = (product.stock - sold).coerceAtLeast(0)) else product
        }

    /** Stock after receiving goods: every purchased line adds to the existing amount. */
    fun applyPurchase(products: List<Product>, lines: List<PurchaseLine>): List<Product> =
        products.map { product ->
            val received = lines.filter { it.productId == product.id }.sumOf { it.qty }
            if (received > 0) product.copy(stock = product.stock + received) else product
        }

    /**
     * Merges a scanned product into the purchase draft. Scanning the same barcode
     * again only increases the quantity, it never duplicates the line.
     */
    fun addToPurchase(
        lines: List<PurchaseLine>,
        product: Product,
        qty: Int = 1,
        unitCost: Double = product.buyPrice,
    ): List<PurchaseLine> {
        if (qty <= 0) return lines
        val existing = lines.firstOrNull { it.productId == product.id }
        return if (existing == null) {
            lines + PurchaseLine(product.id, product.name, product.barcode, unitCost, qty)
        } else {
            lines.map {
                if (it.productId == product.id) it.copy(qty = it.qty + qty, unitCost = unitCost) else it
            }
        }
    }
}

/** POS cart operations. */
object Cart {

    fun total(lines: List<SaleLine>): Double = lines.sumOf { it.subtotal }

    fun itemCount(lines: List<SaleLine>): Int = lines.sumOf { it.qty }

    fun quantityOf(lines: List<SaleLine>, productId: Long): Int =
        lines.firstOrNull { it.productId == productId }?.qty ?: 0

    /**
     * Adds [qty] units of [product]. Never exceeds the available stock, and scanning
     * the same barcode twice only increases the quantity.
     */
    fun add(lines: List<SaleLine>, product: Product, qty: Int = 1): List<SaleLine> {
        val available = product.stock.coerceAtLeast(0)
        if (available == 0) return lines
        val current = quantityOf(lines, product.id)
        val target = (current + qty).coerceIn(1, available)
        val existing = lines.firstOrNull { it.productId == product.id }
        return if (existing == null) {
            lines + SaleLine(product.id, product.name, product.barcode, product.sellPrice, product.buyPrice, target)
        } else {
            lines.map {
                if (it.productId == product.id) {
                    it.copy(qty = target, unitPrice = product.sellPrice, unitCost = product.buyPrice)
                } else {
                    it
                }
            }
        }
    }

    /** Absolute quantity, used by the +/- buttons of the cart. */
    fun setQty(lines: List<SaleLine>, product: Product, qty: Int): List<SaleLine> {
        val clamped = qty.coerceIn(0, product.stock.coerceAtLeast(0))
        if (clamped == 0) return remove(lines, product.id)
        val existing = lines.firstOrNull { it.productId == product.id }
        return if (existing == null) {
            lines + SaleLine(product.id, product.name, product.barcode, product.sellPrice, product.buyPrice, clamped)
        } else {
            lines.map { if (it.productId == product.id) it.copy(qty = clamped) else it }
        }
    }

    fun remove(lines: List<SaleLine>, productId: Long): List<SaleLine> =
        lines.filterNot { it.productId == productId }
}
/** Aggregates used by the dashboard and the reports screen. */
object SalesMath {

    fun total(sales: List<Sale>): Double = sales.sumOf { it.total }

    fun profit(sales: List<Sale>): Double = sales.sumOf { it.profit }

    fun itemCount(sales: List<Sale>): Int = sales.sumOf { it.itemCount }

    fun averageTicket(sales: List<Sale>): Double =
        if (sales.isEmpty()) 0.0 else total(sales) / sales.size

    fun byPaymentMethod(sales: List<Sale>, method: PaymentMethod): Double =
        total(sales.filter { it.paymentMethod == method })

    fun bestSellers(sales: List<Sale>, limit: Int = 5): List<Pair<String, Int>> =
        sales.flatMap { it.lines }
            .groupBy { it.name }
            .map { (name, lines) -> name to lines.sumOf { it.qty } }
            .sortedByDescending { it.second }
            .take(limit)

    /** Totals per day used by the dashboard chart, including days without sales. */
    fun dailyTotals(sales: List<Sale>, days: List<LocalDate>): List<DailySales> =
        days.map { day ->
            val from = Dates.startOfDay(day)
            val to = Dates.endOfDay(day)
            DailySales(
                label = Dates.dayLabel(day),
                dateMillis = from,
                total = total(sales.filter { it.dateMillis in from..to }),
            )
        }
}

/** Date helpers shared by the screens (minSdk 26, so java.time is available). */
object Dates {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun startOfToday(now: Long = System.currentTimeMillis()): Long =
        startOfDay(instantOf(now).atZone(zone).toLocalDate())

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDay(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun today(): LocalDate = LocalDate.now(zone)

    fun lastDays(count: Int): List<LocalDate> {
        val today = today()
        return (count - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    fun instantOf(millis: Long): Instant = Instant.ofEpochMilli(millis)

    fun time(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    fun dateTime(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

    fun dayLabel(date: LocalDate): String =
        SimpleDateFormat("EEE", Locale.getDefault()).format(Date(startOfDay(date)))
}