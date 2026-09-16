package com.libreriapro.ultra.domain

/**
 * Product kept in the inventory.
 *
 * [barcode] is the primary way of identifying a product: it is unique, so a scan
 * can never create a duplicate. Products without a printed barcode keep an
 * empty [barcode] and are handled by name/id.
 */
data class Product(
    val id: Long = 0L,
    val name: String,
    val barcode: String = "",
    val category: String = "",
    val stock: Int = 0,
    val minStock: Int = 0,
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
) {
    val lowStock: Boolean get() = stock <= minStock
    val margin: Double get() = sellPrice - buyPrice

    /** Free text search used by the inventory list and by every "quick search" box. */
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return name.contains(q, ignoreCase = true) ||
            barcode.contains(q, ignoreCase = true) ||
            category.contains(q, ignoreCase = true)
    }
}

/** One line of the sale currently being built (the POS cart). */
data class SaleLine(
    val productId: Long,
    val name: String,
    val barcode: String,
    val unitPrice: Double,
    val unitCost: Double = 0.0,
    val qty: Int,
) {
    val subtotal: Double get() = unitPrice * qty
    val profit: Double get() = (unitPrice - unitCost) * qty
}

enum class PaymentMethod(val label: String) {
    CASH("Efectivo"),
    CARD("Tarjeta"),
    TRANSFER("Transferencia"),
    ;

    companion object {
        fun fromDb(value: String?): PaymentMethod =
            entries.firstOrNull { it.name == value } ?: CASH
    }
}

data class Sale(
    val id: Long = 0L,
    val dateMillis: Long,
    val paymentMethod: PaymentMethod,
    val total: Double,
    val lines: List<SaleLine> = emptyList(),
) {
    val itemCount: Int get() = lines.sumOf { it.qty }
    val profit: Double get() = lines.sumOf { it.profit }
}

/** One line of the purchase currently being built (goods receipt). */
data class PurchaseLine(
    val productId: Long,
    val name: String,
    val barcode: String,
    val unitCost: Double,
    val qty: Int,
) {
    val subtotal: Double get() = unitCost * qty
}

data class Purchase(
    val id: Long = 0L,
    val dateMillis: Long,
    val supplier: String,
    val total: Double,
    val lines: List<PurchaseLine> = emptyList(),
)

enum class MovementType(val label: String) {
    CREATION("Alta"),
    PURCHASE("Compra"),
    SALE("Venta"),
    ADJUSTMENT("Ajuste"),
    DELETION("Baja"),
    ;

    companion object {
        fun fromDb(value: String?): MovementType =
            entries.firstOrNull { it.name == value } ?: ADJUSTMENT
    }
}

/** Kardex entry: every stock change is traceable. */
data class StockMovement(
    val id: Long = 0L,
    val dateMillis: Long,
    val productId: Long,
    val productName: String,
    val type: MovementType,
    val qtyChange: Int,
    val stockAfter: Int,
    val reference: String = "",
)

/** Cash register entry (money in / money out that is not a sale). */
data class CashMovement(
    val id: Long = 0L,
    val dateMillis: Long,
    val isIncome: Boolean,
    val concept: String,
    val amount: Double,
)

/** Aggregated sales of a single day, used by the dashboard charts. */
data class DailySales(
    val label: String,
    val dateMillis: Long,
    val total: Double,
)

data class StoreSettings(
    val businessName: String = "MYC Librería",
    val cashierName: String = "Víctor",
    val currencySymbol: String = "C$",
    val defaultMinStock: Int = 5,
)
