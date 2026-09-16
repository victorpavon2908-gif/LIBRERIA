package com.libreriapro.ultra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Business rules of the shop: barcode lookup, stock movements of sales and purchases,
 * and the cart maths. These rules are pure Kotlin, so they run as JVM unit tests.
 */
class InventoryLogicTest {

    private val notebook = Product(1L, "Cuaderno universitario", "7501234567893", "Cuadernos", 10, 3, 38.0, 55.0)
    private val pencil = Product(2L, "Lápiz HB", "7509873214567", "Escritura", 100, 20, 4.0, 12.0)
    private val catalogue = listOf(notebook, pencil)

    @Test
    fun barcodeLookupIgnoresSurroundingSpaces() {
        val found = Inventory.findByBarcode(catalogue, "  7501234567893 ")
        assertEquals(notebook, found)
    }

    @Test
    fun barcodeLookupReturnsNullWhenCodeIsUnknown() {
        assertNull(Inventory.findByBarcode(catalogue, "0000000000000"))
        assertNull(Inventory.findByBarcode(catalogue, "   "))
    }

    @Test
    fun scanningTheSamePurchaseCodeOnlyAddsQuantity() {
        val first = Inventory.addToPurchase(emptyList(), notebook, 2, 40.0)
        val second = Inventory.addToPurchase(first, notebook, 3, 42.0)

        assertEquals(1, second.size)
        assertEquals(5, second.first().qty)
        assertEquals(notebook.id, second.first().productId)
        assertEquals(42.0, second.first().unitCost, 0.001)
    }

    @Test
    fun purchaseRejectsInvalidQuantityAndCost() {
        assertTrue(Inventory.addToPurchase(emptyList(), notebook, 0, 40.0).isEmpty())
        assertTrue(Inventory.addToPurchase(emptyList(), notebook, 2, -1.0).isEmpty())
    }

    @Test
    fun purchaseAddsToTheExistingStock() {
        val receipt = listOf(
            PurchaseLine(notebook.id, notebook.name, notebook.barcode, 40.0, 5),
            PurchaseLine(pencil.id, pencil.name, pencil.barcode, 4.0, 10),
        )
        val updated = Inventory.applyPurchase(catalogue, receipt)

        assertEquals(15, updated.first { it.id == notebook.id }.stock)
        assertEquals(110, updated.first { it.id == pencil.id }.stock)
    }

    @Test
    fun saleDecreasesStockAndNeverGoesBelowZero() {
        val lines = listOf(
            SaleLine(notebook.id, notebook.name, notebook.barcode, 55.0, 38.0, 4),
            SaleLine(pencil.id, pencil.name, pencil.barcode, 12.0, 4.0, 250),
        )
        val updated = Inventory.applySale(catalogue, lines)

        assertEquals(6, updated.first { it.id == notebook.id }.stock)
        assertEquals(0, updated.first { it.id == pencil.id }.stock)
    }

    @Test
    fun cartNeverSellsMoreUnitsThanAvailable() {
        // Scanning the same code twice only accumulates quantities.
        var cart = Cart.add(emptyList(), notebook, 3)
        cart = Cart.add(cart, notebook, 4)

        assertEquals(7, Cart.quantityOf(cart, notebook.id))
        assertEquals(7, Cart.itemCount(cart))

        // Asking for more units than the shelf holds is capped at the available stock.
        cart = Cart.add(cart, notebook, 100)

        assertEquals(notebook.stock, Cart.quantityOf(cart, notebook.id))
        assertEquals(notebook.stock, Cart.itemCount(cart))
    }

    @Test
    fun cartIgnoresNonPositiveAdditions() {
        val cart = Cart.add(emptyList(), notebook, 2)
        val unchanged = Cart.add(cart, notebook, -5)

        assertEquals(cart, unchanged)
    }

    @Test
    fun cartIgnoresProductsWithoutStock() {
        val emptyProduct = notebook.copy(stock = 0)
        val cart = Cart.add(emptyList(), emptyProduct, 1)

        assertTrue(cart.isEmpty())
    }

    @Test
    fun cartSetQuantityToZeroRemovesTheLine() {
        val cart = Cart.setQty(Cart.add(emptyList(), notebook, 2), notebook, 0)

        assertTrue(cart.isEmpty())
    }

    @Test
    fun cartUpdatesPriceAndCostFromTheCurrentProduct() {
        val cart = Cart.add(emptyList(), notebook, 1)
        val repriced = Cart.add(cart, notebook.copy(sellPrice = 60.0, buyPrice = 41.0), 1)

        assertEquals(60.0, repriced.first().unitPrice, 0.001)
        assertEquals(41.0, repriced.first().unitCost, 0.001)
        assertEquals(2, repriced.first().qty)
    }

    @Test
    fun salesMathCalculatesTotalsAndProfit() {
        val sales = listOf(
            Sale(
                id = 1L,
                dateMillis = 1_000L,
                paymentMethod = PaymentMethod.CASH,
                total = 110.0,
                lines = listOf(SaleLine(notebook.id, notebook.name, notebook.barcode, 55.0, 38.0, 2)),
            ),
            Sale(
                id = 2L,
                dateMillis = 2_000L,
                paymentMethod = PaymentMethod.CARD,
                total = 24.0,
                lines = listOf(SaleLine(pencil.id, pencil.name, pencil.barcode, 12.0, 4.0, 2)),
            ),
        )

        assertEquals(134.0, SalesMath.total(sales), 0.001)
        assertEquals(50.0, SalesMath.profit(sales), 0.001)
        assertEquals(4, SalesMath.itemCount(sales))
        assertEquals(67.0, SalesMath.averageTicket(sales), 0.001)
        assertEquals(110.0, SalesMath.byPaymentMethod(sales, PaymentMethod.CASH), 0.001)
        assertEquals(24.0, SalesMath.byPaymentMethod(sales, PaymentMethod.CARD), 0.001)
        assertEquals(0.0, SalesMath.byPaymentMethod(sales, PaymentMethod.TRANSFER), 0.001)
    }

    @Test
    fun salesMathRanksTheBestSellers() {
        val sales = listOf(
            Sale(1L, 1_000L, PaymentMethod.CASH, 108.0, listOf(SaleLine(pencil.id, pencil.name, pencil.barcode, 12.0, 4.0, 9))),
            Sale(2L, 2_000L, PaymentMethod.CASH, 55.0, listOf(SaleLine(notebook.id, notebook.name, notebook.barcode, 55.0, 38.0, 1))),
            Sale(3L, 3_000L, PaymentMethod.CASH, 36.0, listOf(SaleLine(pencil.id, pencil.name, pencil.barcode, 12.0, 4.0, 3))),
        )

        val ranking = SalesMath.bestSellers(sales, 2)

        assertEquals(pencil.name to 12, ranking.first())
        assertEquals(notebook.name to 1, ranking.last())
    }

    @Test
    fun salesMathRanksZeroItemsWithoutThrowing() {
        assertTrue(SalesMath.bestSellers(emptyList(), 0).isEmpty())
    }

    @Test
    fun dailyTotalsGroupsSalesPerDay() {
        val today = Dates.today()
        val sales = listOf(
            Sale(1L, Dates.startOfDay(today) + 3_600_000L, PaymentMethod.CASH, 55.0),
            Sale(2L, Dates.startOfDay(today.minusDays(2)) + 3_600_000L, PaymentMethod.CASH, 20.0),
        )

        val totals = SalesMath.dailyTotals(sales, Dates.lastDays(7))

        assertEquals(7, totals.size)
        assertEquals(55.0, totals.last().total, 0.001)
        assertEquals(20.0, totals[4].total, 0.001)
    }

    @Test
    fun lastDaysRejectsNonPositiveCount() {
        assertTrue(Dates.lastDays(0).isEmpty())
        assertTrue(Dates.lastDays(-1).isEmpty())
    }
}