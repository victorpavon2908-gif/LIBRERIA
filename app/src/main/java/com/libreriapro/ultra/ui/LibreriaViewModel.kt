package com.libreriapro.ultra.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libreriapro.ultra.data.LibreriaDatabase
import com.libreriapro.ultra.data.LibreriaRepository
import com.libreriapro.ultra.data.ProductSaveResult
import com.libreriapro.ultra.data.SettingsStore
import com.libreriapro.ultra.domain.Cart
import com.libreriapro.ultra.domain.CashMovement
import com.libreriapro.ultra.domain.Dates
import com.libreriapro.ultra.domain.Inventory
import com.libreriapro.ultra.domain.PaymentMethod
import com.libreriapro.ultra.domain.Product
import com.libreriapro.ultra.domain.Purchase
import com.libreriapro.ultra.domain.PurchaseLine
import com.libreriapro.ultra.domain.Sale
import com.libreriapro.ultra.domain.SaleLine
import com.libreriapro.ultra.domain.SalesMath
import com.libreriapro.ultra.domain.StockMovement
import com.libreriapro.ultra.domain.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Screen that requested the barcode scanner, so a scan can be routed correctly. */
enum class ScanTarget { INVENTORY, SALE, PURCHASE }

/** Where a freshly created product must be added after being saved. */
enum class AddTarget { NONE, CART, PURCHASE }

/** Single source of truth of the whole UI. */
data class UiState(
    val loading: Boolean = true,
    val settings: StoreSettings = StoreSettings(),
    val products: List<Product> = emptyList(),
    val cart: List<SaleLine> = emptyList(),
    val purchaseDraft: List<PurchaseLine> = emptyList(),
    val supplier: String = "",
    val salesToday: List<Sale> = emptyList(),
    val salesWeek: List<Sale> = emptyList(),
    val purchasesWeek: List<Purchase> = emptyList(),
    val movements: List<StockMovement> = emptyList(),
    val cashMovements: List<CashMovement> = emptyList(),
    val categories: List<String> = emptyList(),
    val lastTicket: Sale? = null,
    val message: String? = null,
    val pendingBarcode: String? = null,
    val pendingProductId: Long? = null,
    val scanTarget: ScanTarget? = null,
    /** Last result of the barcode scanner, shown inside the scanner dialog itself. */
    val scanFeedback: String? = null,
) {
    val lowStock: List<Product> get() = products.filter { it.lowStock }
    val cartTotal: Double get() = Cart.total(cart)
    val cartCount: Int get() = Cart.itemCount(cart)
    val purchaseTotal: Double get() = purchaseDraft.sumOf { it.subtotal }
    val purchaseCount: Int get() = purchaseDraft.sumOf { it.qty }
    val todayTotal: Double get() = SalesMath.total(salesToday)
    val todayProfit: Double get() = SalesMath.profit(salesToday)
    val todayItems: Int get() = SalesMath.itemCount(salesToday)
    val todaySalesCount: Int get() = salesToday.size
    val inventoryValue: Double get() = products.sumOf { it.stock * it.buyPrice }
    val inventoryRetailValue: Double get() = products.sumOf { it.stock * it.sellPrice }
    val cashIn: Double get() = cashMovements.filter { it.isIncome }.sumOf { it.amount }
    val cashOut: Double get() = cashMovements.filter { !it.isIncome }.sumOf { it.amount }
    val dayCashSales: Double get() = SalesMath.byPaymentMethod(salesToday, PaymentMethod.CASH)
    val dayCardSales: Double get() = SalesMath.byPaymentMethod(salesToday, PaymentMethod.CARD)
    val dayTransferSales: Double get() = SalesMath.byPaymentMethod(salesToday, PaymentMethod.TRANSFER)
    val expectedCash: Double get() = dayCashSales + cashIn - cashOut
    val weekSalesTotal: Double get() = SalesMath.total(salesWeek)
    val weekPurchaseTotal: Double get() = purchasesWeek.sumOf { it.total }
    val weekProfit: Double get() = SalesMath.profit(salesWeek)
    val dailyTotals: List<com.libreriapro.ultra.domain.DailySales>
        get() = SalesMath.dailyTotals(salesWeek, Dates.lastDays(7))
}

class LibreriaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibreriaRepository(LibreriaDatabase.get(application))
    private val settingsStore = SettingsStore(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(settings = settingsStore.load()) }
        refresh()
    }

    private data class Snapshot(
        val products: List<Product>,
        val salesToday: List<Sale>,
        val salesWeek: List<Sale>,
        val purchasesWeek: List<Purchase>,
        val movements: List<StockMovement>,
        val cashMovements: List<CashMovement>,
        val categories: List<String>,
    )

    /** Reloads everything that can change and keeps the open cart consistent. */
    fun refresh() {
        viewModelScope.launch {
            val todayStart = Dates.startOfToday()
            val weekStart = Dates.startOfDay(Dates.today().minusDays(6))
            val snapshot = withContext(Dispatchers.IO) {
                Snapshot(
                    products = repository.products(),
                    salesToday = repository.salesBetween(todayStart),
                    salesWeek = repository.salesBetween(weekStart),
                    purchasesWeek = repository.purchasesBetween(weekStart),
                    movements = repository.movements(150),
                    cashMovements = repository.cashMovements(todayStart),
                    categories = repository.categories(),
                )
            }
            _state.update { current ->
                val productsById = snapshot.products.associateBy { it.id }
                val cart = current.cart.mapNotNull { line ->
                    val product = productsById[line.productId] ?: return@mapNotNull null
                    val qty = line.qty.coerceAtMost(product.stock.coerceAtLeast(0))
                    if (qty <= 0) {
                        null
                    } else {
                        line.copy(
                            name = product.name,
                            unitPrice = product.sellPrice,
                            unitCost = product.buyPrice,
                            qty = qty,
                        )
                    }
                }
                val draft = current.purchaseDraft.mapNotNull { line ->
                    productsById[line.productId]?.let { product -> line.copy(name = product.name) }
                }
                current.copy(
                    loading = false,
                    products = snapshot.products,
                    cart = cart,
                    purchaseDraft = draft,
                    salesToday = snapshot.salesToday,
                    salesWeek = snapshot.salesWeek,
                    purchasesWeek = snapshot.purchasesWeek,
                    movements = snapshot.movements,
                    cashMovements = snapshot.cashMovements,
                    categories = snapshot.categories,
                )
            }
        }
    }

    // ------------------------------------------------------------------ inventory

    /**
     * Creates or updates a product. When the barcode already exists the shop data is
     * updated instead of creating a duplicate.
     */
    fun saveProduct(draft: Product, addTo: AddTarget = AddTarget.NONE) {
        if (draft.name.isBlank()) {
            message("El nombre del producto es obligatorio")
            return
        }
        if (draft.sellPrice < 0 || draft.buyPrice < 0 || draft.stock < 0 || draft.minStock < 0) {
            message("Los precios y las existencias no pueden ser negativos")
            return
        }
        viewModelScope.launch {
            val (result, id) = withContext(Dispatchers.IO) { repository.saveProduct(draft) }
            val saved = withContext(Dispatchers.IO) { repository.productById(id) }
            val text = when (result) {
                ProductSaveResult.INSERTED -> "Producto registrado: ${draft.name}"
                ProductSaveResult.UPDATED -> "Producto actualizado: ${draft.name}"
                ProductSaveResult.MERGED_BY_BARCODE ->
                    "El código ${draft.barcode} ya existía: se actualizó el producto sin duplicarlo"
                ProductSaveResult.MISSING_NAME -> "El nombre del producto es obligatorio"
            }
            _state.update { it.copy(message = text, pendingBarcode = null) }
            if (saved != null && addTo != AddTarget.NONE) {
                when (addTo) {
                    AddTarget.CART -> addToCart(saved)
                    AddTarget.PURCHASE -> addToPurchase(saved)
                    AddTarget.NONE -> Unit
                }
            }
            refresh()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteProduct(product.id) }
            _state.update {
                it.copy(
                    cart = Cart.remove(it.cart, product.id),
                    purchaseDraft = it.purchaseDraft.filterNot { line -> line.productId == product.id },
                    message = "Producto eliminado: ${product.name}",
                )
            }
            refresh()
        }
    }

    /** Manual stock correction registered in the kardex (physical count, breakage...). */
    fun adjustStock(product: Product, delta: Int) {
        if (delta == 0) return
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) { repository.adjustStock(product.id, delta, "Ajuste manual") }
            _state.update {
                it.copy(message = "Stock de ${product.name} actualizado a ${updated?.stock ?: product.stock}")
            }
            refresh()
        }
    }

    fun clearPendingBarcode() {
        _state.update { it.copy(pendingBarcode = null) }
    }

    fun clearPendingProduct() {
        _state.update { it.copy(pendingProductId = null) }
    }

    // ----------------------------------------------------------------------- sales

    /** Adds a product to the cart. Scanning the same barcode only sums quantities. */
    fun addToCart(product: Product, qty: Int = 1) {
        val current = _state.value
        if (product.stock <= 0) {
            message("${product.name} no tiene existencias disponibles")
            return
        }
        val before = Cart.quantityOf(current.cart, product.id)
        val updated = Cart.add(current.cart, product, qty)
        val atLimit = Cart.quantityOf(updated, product.id) == product.stock && before < product.stock
        _state.update {
            if (atLimit) {
                it.copy(cart = updated, message = "Stock máximo alcanzado para ${product.name}")
            } else {
                it.copy(cart = updated)
            }
        }
    }

    fun incrementCartLine(productId: Long, delta: Int) {
        val current = _state.value
        val product = current.products.firstOrNull { it.id == productId } ?: return
        val qty = Cart.quantityOf(current.cart, productId) + delta
        _state.update { it.copy(cart = Cart.setQty(it.cart, product, qty)) }
    }

    fun setCartQty(productId: Long, qty: Int) {
        val product = _state.value.products.firstOrNull { it.id == productId } ?: return
        _state.update { it.copy(cart = Cart.setQty(it.cart, product, qty)) }
    }

    fun removeFromCart(productId: Long) {
        _state.update { it.copy(cart = Cart.remove(it.cart, productId)) }
    }

    fun clearCart() {
        _state.update { it.copy(cart = emptyList(), message = "Venta cancelada") }
    }

    /** Closes the sale: stores it, decreases stock and empties the cart. */
    fun checkout(method: PaymentMethod) {
        val lines = _state.value.cart
        if (lines.isEmpty()) {
            message("El carrito está vacío")
            return
        }
        viewModelScope.launch {
            val saleId = withContext(Dispatchers.IO) { repository.recordSale(lines, method) }
            val ticket = Sale(
                id = saleId,
                dateMillis = System.currentTimeMillis(),
                paymentMethod = method,
                total = Cart.total(lines),
                lines = lines,
            )
            _state.update {
                it.copy(
                    cart = emptyList(),
                    lastTicket = ticket,
                    message = "Venta #$saleId registrada · " +
                        "${Fmt.money(ticket.total, it.settings.currencySymbol)} en ${method.label}",
                )
            }
            refresh()
        }
    }

    // ------------------------------------------------------------------- purchases

    /** Adds a product to the goods receipt. Scanning the same code sums quantities. */
    fun addToPurchase(product: Product, qty: Int = 1, unitCost: Double = product.buyPrice) {
        if (qty <= 0) return
        val updated = Inventory.addToPurchase(_state.value.purchaseDraft, product, qty, unitCost)
        _state.update {
            it.copy(
                purchaseDraft = updated,
                message = "${product.name} · $qty unidad(es) por recibir · stock actual ${product.stock}",
            )
        }
    }

    fun setPurchaseQty(productId: Long, qty: Int) {
        _state.update { current ->
            val draft = if (qty <= 0) {
                current.purchaseDraft.filterNot { it.productId == productId }
            } else {
                current.purchaseDraft.map { if (it.productId == productId) it.copy(qty = qty) else it }
            }
            current.copy(purchaseDraft = draft)
        }
    }

    fun setPurchaseCost(productId: Long, cost: Double) {
        if (cost < 0) return
        _state.update { current ->
            current.copy(
                purchaseDraft = current.purchaseDraft.map {
                    if (it.productId == productId) it.copy(unitCost = cost) else it
                },
            )
        }
    }

    fun removeFromPurchase(productId: Long) {
        _state.update {
            it.copy(purchaseDraft = it.purchaseDraft.filterNot { line -> line.productId == productId })
        }
    }

    fun setSupplier(value: String) {
        _state.update { it.copy(supplier = value) }
    }

    fun clearPurchase() {
        _state.update { it.copy(purchaseDraft = emptyList(), supplier = "", message = "Compra cancelada") }
    }

    /** Confirms the goods receipt: increases stock and registers the purchase. */
    fun confirmPurchase() {
        val current = _state.value
        if (current.purchaseDraft.isEmpty()) {
            message("Agrega al menos un producto a la compra")
            return
        }
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                repository.recordPurchase(current.supplier, current.purchaseDraft)
            }
            val units = current.purchaseDraft.sumOf { it.qty }
            _state.update {
                it.copy(
                    purchaseDraft = emptyList(),
                    supplier = "",
                    message = "Compra #$id registrada · +$units unidades en inventario",
                )
            }
            refresh()
        }
    }

    // ------------------------------------------------------------------- cash desk

    fun addCashMovement(isIncome: Boolean, concept: String, amount: Double) {
        if (concept.isBlank() || amount <= 0) {
            message("Indica un concepto y un monto mayor que cero")
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.addCashMovement(isIncome, concept, amount) }
            _state.update {
                val kind = if (isIncome) "Entrada" else "Salida"
                it.copy(message = "$kind de caja registrada: ${Fmt.money(amount, it.settings.currencySymbol)}")
            }
            refresh()
        }
    }

    // -------------------------------------------------------------------- settings

    fun updateSettings(settings: StoreSettings) {
        if (settings.defaultMinStock < 0) {
            message("El stock mínimo no puede ser negativo")
            return
        }
        settingsStore.save(settings)
        _state.update { it.copy(settings = settings, message = "Configuración guardada") }
    }

    // ---------------------------------------------------------------------- scanner

    fun beginScan(target: ScanTarget) {
        _state.update { it.copy(scanTarget = target, scanFeedback = null) }
    }

    fun endScan() {
        _state.update { it.copy(scanTarget = null, scanFeedback = null) }
    }

    /**
     * Central barcode entry point: a scanned code adds the product to the sale, adds
     * it to the goods receipt, or asks for a new product when the code is unknown.
     */
    fun onBarcodeScanned(rawCode: String) {
        val code = Inventory.normalizeBarcode(rawCode)
        if (code.isEmpty()) return
        val target = _state.value.scanTarget
        viewModelScope.launch {
            val product = withContext(Dispatchers.IO) { repository.findByBarcode(code) }
            when {
                product == null -> {
                    val unknown = "El código $code no está registrado"
                    _state.update {
                        it.copy(
                            scanTarget = null,
                            scanFeedback = null,
                            pendingBarcode =
                                if (target == ScanTarget.INVENTORY || target == ScanTarget.PURCHASE) code else null,
                            message = if (target == ScanTarget.SALE) {
                                "$unknown · agrégalo desde Inventario"
                            } else {
                                "$unknown · completa la ficha para registrarlo"
                            },
                        )
                    }
                }

                target == ScanTarget.SALE -> {
                    // The scanner stays open so several products can be scanned in a row.
                    addToCart(product)
                    _state.update {
                        val inCart = Cart.quantityOf(it.cart, product.id)
                        it.copy(
                            scanFeedback = if (inCart > 0) {
                                "${product.name} · $inCart en el carrito · " +
                                    "${it.cartCount} artículo(s) · " +
                                    Fmt.money(it.cartTotal, it.settings.currencySymbol)
                            } else {
                                "${product.name} no tiene existencias disponibles"
                            },
                        )
                    }
                }

                target == ScanTarget.PURCHASE -> {
                    addToPurchase(product)
                    _state.update {
                        val receiving = it.purchaseDraft
                            .firstOrNull { line -> line.productId == product.id }?.qty ?: 0
                        it.copy(
                            scanFeedback = "${product.name} · $receiving por recibir · " +
                                "${it.purchaseCount} unidad(es) en la compra · stock actual ${product.stock}",
                        )
                    }
                }

                target == ScanTarget.INVENTORY -> {
                    _state.update {
                        it.copy(
                            scanTarget = null,
                            scanFeedback = null,
                            pendingProductId = product.id,
                            message = "${product.name} · stock ${product.stock} · " +
                                Fmt.money(product.sellPrice, it.settings.currencySymbol),
                        )
                    }
                }

                else -> _state.update { it.copy(scanTarget = null, scanFeedback = null) }
            }
        }
    }

    // ---------------------------------------------------------------------- helpers

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun consumeTicket() {
        _state.update { it.copy(lastTicket = null) }
    }

    private fun message(text: String) {
        _state.update { it.copy(message = text) }
    }
}