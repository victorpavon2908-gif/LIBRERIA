package com.libreriapro.ultra.data

import android.content.ContentValues
import android.database.Cursor
import com.libreriapro.ultra.domain.CashMovement
import com.libreriapro.ultra.domain.Inventory
import com.libreriapro.ultra.domain.MovementType
import com.libreriapro.ultra.domain.PaymentMethod
import com.libreriapro.ultra.domain.Product
import com.libreriapro.ultra.domain.Purchase
import com.libreriapro.ultra.domain.PurchaseLine
import com.libreriapro.ultra.domain.Sale
import com.libreriapro.ultra.domain.SaleLine
import com.libreriapro.ultra.domain.StockMovement

enum class ProductSaveResult { INSERTED, UPDATED, MERGED_BY_BARCODE, MISSING_NAME }

class LibreriaRepository(private val helper: LibreriaDatabase) {
    private val read get() = helper.readableDatabase
    private val write get() = helper.writableDatabase

    fun products(): List<Product> {
        val result = mutableListOf<Product>()
        read.rawQuery(
            "SELECT id, name, barcode, category, stock, min_stock, buy_price, sell_price FROM products ORDER BY name COLLATE NOCASE ASC",
            null,
        ).use { cursor -> while (cursor.moveToNext()) result += cursor.toProduct() }
        return result
    }

    fun productById(id: Long): Product? = read.rawQuery(
        "SELECT id, name, barcode, category, stock, min_stock, buy_price, sell_price FROM products WHERE id = ?",
        arrayOf(id.toString()),
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toProduct() else null }

    fun findByBarcode(rawBarcode: String): Product? {
        val barcode = Inventory.normalizeBarcode(rawBarcode)
        if (barcode.isEmpty()) return null
        return read.rawQuery(
            "SELECT id, name, barcode, category, stock, min_stock, buy_price, sell_price FROM products WHERE barcode = ? COLLATE NOCASE LIMIT 1",
            arrayOf(barcode),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toProduct() else null }
    }

    fun categories(): List<String> {
        val result = mutableListOf<String>()
        read.rawQuery(
            "SELECT DISTINCT category FROM products WHERE category <> '' ORDER BY category COLLATE NOCASE ASC",
            null,
        ).use { cursor -> while (cursor.moveToNext()) result += cursor.getString(0) }
        return result
    }

    fun saveProduct(product: Product): Pair<ProductSaveResult, Long> {
        if (product.name.isBlank()) return ProductSaveResult.MISSING_NAME to 0L
        requireProductValues(product)
        val barcode = Inventory.normalizeBarcode(product.barcode)
        val existingByBarcode = if (barcode.isNotEmpty()) findByBarcode(barcode) else null
        val targetId = when {
            product.id > 0 -> product.id
            existingByBarcode != null -> existingByBarcode.id
            else -> 0L
        }
        val isMerge = product.id <= 0 && existingByBarcode != null
        val stockBefore = if (targetId > 0) productById(targetId)?.stock ?: 0 else 0
        val values = product.copy(barcode = barcode).toValues()
        return if (targetId > 0) {
            values.remove("id")
            if (isMerge) values.remove("stock")
            write.update("products", values, "id = ?", arrayOf(targetId.toString()))
            if (!isMerge) recordMovement(targetId, product.name, MovementType.ADJUSTMENT, product.stock - stockBefore, product.stock, "Edición de ficha")
            (if (isMerge) ProductSaveResult.MERGED_BY_BARCODE else ProductSaveResult.UPDATED) to targetId
        } else {
            val id = write.insertOrThrow("products", null, values)
            recordMovement(id, product.name, MovementType.CREATION, product.stock, product.stock, "Producto registrado")
            ProductSaveResult.INSERTED to id
        }
    }

    fun deleteProduct(id: Long): Boolean {
        val product = productById(id) ?: return false
        write.beginTransaction()
        try {
            write.delete("products", "id = ?", arrayOf(id.toString()))
            recordMovement(id, product.name, MovementType.DELETION, -product.stock, 0, "Producto eliminado")
            write.setTransactionSuccessful()
        } finally { write.endTransaction() }
        return true
    }

    fun adjustStock(productId: Long, delta: Int, reference: String): Product? {
        val product = productById(productId) ?: return null
        val after = (product.stock + delta).coerceAtLeast(0)
        write.beginTransaction()
        try {
            write.update("products", ContentValues().apply { put("stock", after) }, "id = ?", arrayOf(productId.toString()))
            recordMovement(productId, product.name, MovementType.ADJUSTMENT, after - product.stock, after, reference)
            write.setTransactionSuccessful()
        } finally { write.endTransaction() }
        return product.copy(stock = after)
    }

    private fun Cursor.toProduct(): Product = Product(
        id = getLong(0), name = getString(1), barcode = getString(2), category = getString(3),
        stock = getInt(4), minStock = getInt(5), buyPrice = getDouble(6), sellPrice = getDouble(7),
    )

    private fun requireProductValues(product: Product) {
        require(product.stock >= 0 && product.minStock >= 0) { "Existencias inválidas" }
        require(product.buyPrice.isFinite() && product.buyPrice >= 0.0) { "Precio de compra inválido" }
        require(product.sellPrice.isFinite() && product.sellPrice >= 0.0) { "Precio de venta inválido" }
    }

    private fun applyStockDelta(productId: Long, delta: Int): Int {
        write.execSQL("UPDATE products SET stock = MAX(stock + ?, 0) WHERE id = ?", arrayOf<Any>(delta, productId))
        return read.rawQuery("SELECT stock FROM products WHERE id = ?", arrayOf(productId.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
    }

    private fun currentStock(productId: Long): Int = productById(productId)?.stock
        ?: throw IllegalStateException("Producto $productId no existe")

    private fun recordMovement(productId: Long, productName: String, type: MovementType, qtyChange: Int, stockAfter: Int, reference: String, dateMillis: Long = System.currentTimeMillis()) {
        write.insertOrThrow("movements", null, ContentValues().apply {
            put("date_millis", dateMillis); put("product_id", productId); put("product_name", productName)
            put("type", type.name); put("qty_change", qtyChange); put("stock_after", stockAfter); put("reference", reference)
        })
    }

    fun recordSale(lines: List<SaleLine>, method: PaymentMethod, dateMillis: Long = System.currentTimeMillis()): Long {
        require(lines.isNotEmpty()) { "El carrito está vacío" }
        require(lines.all { it.qty > 0 && it.unitPrice.isFinite() && it.unitPrice >= 0 && it.unitCost.isFinite() && it.unitCost >= 0 }) { "La venta contiene datos inválidos" }
        val requested = lines.groupingBy { it.productId }.fold(0) { acc, line -> acc + line.qty }
        write.beginTransaction()
        try {
            requested.forEach { (productId, qty) ->
                val stock = currentStock(productId)
                require(stock >= qty) { "Stock insuficiente para el producto $productId" }
            }
            val total = lines.sumOf { it.subtotal }
            require(total.isFinite() && total >= 0.0) { "Total de venta inválido" }
            val saleId = write.insertOrThrow("sales", null, ContentValues().apply {
                put("date_millis", dateMillis); put("payment_method", method.name); put("total", total)
            })
            lines.forEach { line ->
                write.insertOrThrow("sale_items", null, ContentValues().apply {
                    put("sale_id", saleId); put("product_id", line.productId); put("name", line.name); put("barcode", line.barcode)
                    put("unit_price", line.unitPrice); put("unit_cost", line.unitCost); put("qty", line.qty)
                })
            }
            requested.forEach { (productId, qty) ->
                val product = productById(productId) ?: throw IllegalStateException("Producto $productId no existe")
                val after = applyStockDelta(productId, -qty)
                recordMovement(productId, product.name, MovementType.SALE, -qty, after, "Venta #$saleId")
            }
            write.setTransactionSuccessful()
            return saleId
        } finally { write.endTransaction() }
    }

    fun salesBetween(fromMillis: Long, toMillis: Long = Long.MAX_VALUE): List<Sale> {
        val headers = mutableListOf<Sale>()
        read.rawQuery("SELECT id, date_millis, payment_method, total FROM sales WHERE date_millis >= ? AND date_millis <= ? ORDER BY date_millis DESC, id DESC", arrayOf(fromMillis.toString(), toMillis.toString())).use { cursor ->
            while (cursor.moveToNext()) headers += Sale(cursor.getLong(0), cursor.getLong(1), PaymentMethod.fromDb(cursor.getString(2)), cursor.getDouble(3))
        }
        return headers.map { it.copy(lines = saleLines(it.id)) }
    }

    private fun saleLines(saleId: Long): List<SaleLine> {
        val lines = mutableListOf<SaleLine>()
        read.rawQuery("SELECT product_id, name, barcode, unit_price, unit_cost, qty FROM sale_items WHERE sale_id = ? ORDER BY id ASC", arrayOf(saleId.toString())).use { cursor ->
            while (cursor.moveToNext()) lines += SaleLine(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getDouble(4), cursor.getInt(5))
        }
        return lines
    }

    fun purchasesBetween(fromMillis: Long, toMillis: Long = Long.MAX_VALUE): List<Purchase> {
        val headers = mutableListOf<Purchase>()
        read.rawQuery("SELECT id, date_millis, supplier, total FROM purchases WHERE date_millis >= ? AND date_millis <= ? ORDER BY date_millis DESC, id DESC", arrayOf(fromMillis.toString(), toMillis.toString())).use { cursor ->
            while (cursor.moveToNext()) headers += Purchase(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getDouble(3))
        }
        return headers.map { it.copy(lines = purchaseLines(it.id)) }
    }

    private fun purchaseLines(purchaseId: Long): List<PurchaseLine> {
        val lines = mutableListOf<PurchaseLine>()
        read.rawQuery("SELECT product_id, name, barcode, unit_cost, qty FROM purchase_items WHERE purchase_id = ? ORDER BY id ASC", arrayOf(purchaseId.toString())).use { cursor ->
            while (cursor.moveToNext()) lines += PurchaseLine(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getInt(4))
        }
        return lines
    }

    fun recordPurchase(supplier: String, lines: List<PurchaseLine>, dateMillis: Long = System.currentTimeMillis()): Long {
        require(lines.isNotEmpty()) { "La compra está vacía" }
        require(lines.all { it.qty > 0 && it.unitCost.isFinite() && it.unitCost >= 0 }) { "La compra contiene datos inválidos" }
        val total = lines.sumOf { it.subtotal }
        require(total.isFinite() && total >= 0.0) { "Total de compra inválido" }
        write.beginTransaction()
        try {
            lines.forEach { require(productById(it.productId) != null) { "Producto ${it.productId} no existe" } }
            val purchaseId = write.insertOrThrow("purchases", null, ContentValues().apply {
                put("date_millis", dateMillis); put("supplier", supplier.trim()); put("total", total)
            })
            lines.forEach { line ->
                write.insertOrThrow("purchase_items", null, ContentValues().apply {
                    put("purchase_id", purchaseId); put("product_id", line.productId); put("name", line.name); put("barcode", line.barcode)
                    put("unit_cost", line.unitCost); put("qty", line.qty)
                })
                val after = applyStockDelta(line.productId, line.qty)
                write.update("products", ContentValues().apply { put("buy_price", line.unitCost) }, "id = ?", arrayOf(line.productId.toString()))
                recordMovement(line.productId, line.name, MovementType.PURCHASE, line.qty, after, "Compra #$purchaseId")
            }
            write.setTransactionSuccessful()
            return purchaseId
        } finally { write.endTransaction() }
    }

    fun movements(limit: Int = 300): List<StockMovement> {
        val result = mutableListOf<StockMovement>()
        val safeLimit = limit.coerceAtLeast(0)
        read.rawQuery("SELECT id, date_millis, product_id, product_name, type, qty_change, stock_after, reference FROM movements ORDER BY date_millis DESC, id DESC LIMIT ?", arrayOf(safeLimit.toString())).use { cursor -> while (cursor.moveToNext()) result += cursor.toMovement() }
        return result
    }

    fun movementsOf(productId: Long, limit: Int = 100): List<StockMovement> {
        val result = mutableListOf<StockMovement>()
        val safeLimit = limit.coerceAtLeast(0)
        read.rawQuery("SELECT id, date_millis, product_id, product_name, type, qty_change, stock_after, reference FROM movements WHERE product_id = ? ORDER BY date_millis DESC, id DESC LIMIT ?", arrayOf(productId.toString(), safeLimit.toString())).use { cursor -> while (cursor.moveToNext()) result += cursor.toMovement() }
        return result
    }

    private fun Cursor.toMovement(): StockMovement = StockMovement(getLong(0), getLong(1), getLong(2), getString(3), MovementType.fromDb(getString(4)), getInt(5), getInt(6), getString(7))

    fun cashMovements(fromMillis: Long, toMillis: Long = Long.MAX_VALUE): List<CashMovement> {
        val result = mutableListOf<CashMovement>()
        read.rawQuery("SELECT id, date_millis, is_income, concept, amount FROM cash_movements WHERE date_millis >= ? AND date_millis <= ? ORDER BY date_millis DESC, id DESC", arrayOf(fromMillis.toString(), toMillis.toString())).use { cursor ->
            while (cursor.moveToNext()) result += CashMovement(cursor.getLong(0), cursor.getLong(1), cursor.getInt(2) == 1, cursor.getString(3), cursor.getDouble(4))
        }
        return result
    }

    fun addCashMovement(isIncome: Boolean, concept: String, amount: Double, dateMillis: Long = System.currentTimeMillis()): Long {
        require(concept.isNotBlank()) { "El concepto es obligatorio" }
        require(amount.isFinite() && amount > 0.0) { "El monto debe ser mayor que cero" }
        return write.insertOrThrow("cash_movements", null, ContentValues().apply {
            put("date_millis", dateMillis); put("is_income", if (isIncome) 1 else 0); put("concept", concept.trim()); put("amount", amount)
        })
    }
}
