package com.libreriapro.ultra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.domain.Product
import com.libreriapro.ultra.domain.PurchaseLine
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.DarkTextField
import com.libreriapro.ultra.ui.components.EmptyState
import com.libreriapro.ultra.ui.components.KeyValueRow
import com.libreriapro.ultra.ui.components.LocalCompactLayout
import com.libreriapro.ultra.ui.components.QuantityStepper
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.CardElevated
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Purple
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft

/**
 * Goods receipt. A scanned barcode adds the product to the receipt and, once the
 * purchase is confirmed, the stock of every line is increased by its quantity.
 * An unknown barcode opens the product form pre-filled, so a new item can be
 * registered without leaving the purchase.
 */
@Composable
fun PurchaseScreen(
    state: UiState,
    onScan: () -> Unit,
    onAdd: (Product) -> Unit,
    onQty: (Long, Int) -> Unit,
    onCost: (Long, Double) -> Unit,
    onRemove: (Long) -> Unit,
    onSupplier: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onSaveNewProduct: (Product) -> Unit,
    onClearPendingBarcode: () -> Unit,
) {
    val compact = LocalCompactLayout.current
    val symbol = state.settings.currencySymbol
    var query by remember { mutableStateOf("") }
    var newProduct by remember { mutableStateOf<Product?>(null) }

    // A scanned code that is not in the catalogue yet opens a pre-filled form; saving
    // it registers the product and adds it straight to the receipt.
    LaunchedEffect(state.pendingBarcode) {
        val code = state.pendingBarcode
        if (code != null) {
            newProduct = Product(name = "", barcode = code, minStock = state.settings.defaultMinStock)
            onClearPendingBarcode()
        }
    }

    val catalogue = state.products.sortedWith(
        compareByDescending<Product> { line -> state.purchaseDraft.any { it.productId == line.id } }
            .thenBy { it.name.lowercase() },
    ).filter { it.matches(query) }

    val cataloguePane: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            PurchaseHeader(onScan)
            Spacer(Modifier.height(10.dp))
            DarkTextField(
                value = state.supplier,
                onValueChange = onSupplier,
                label = "Proveedor",
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar producto para agregarlo") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = CardElevated,
                ),
            )
            Spacer(Modifier.height(10.dp))
            PurchaseCatalogue(
                products = catalogue,
                symbol = symbol,
                onAdd = onAdd,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (compact) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            cataloguePane(Modifier.weight(1f))
            Spacer(Modifier.height(10.dp))
            PurchaseDraftPanel(
                state = state,
                symbol = symbol,
                onQty = onQty,
                onCost = onCost,
                onRemove = onRemove,
                onConfirm = onConfirm,
                onCancel = onCancel,
                modifier = Modifier.heightIn(max = 300.dp),
            )
        }
    } else {
        Row(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            cataloguePane(Modifier.weight(1f))
            PurchaseDraftPanel(
                state = state,
                symbol = symbol,
                onQty = onQty,
                onCost = onCost,
                onRemove = onRemove,
                onConfirm = onConfirm,
                onCancel = onCancel,
                modifier = Modifier
                    .width(440.dp)
                    .fillMaxHeight(),
            )
        }
    }

    newProduct?.let { draftProduct ->
        ProductEditorDialog(
            product = draftProduct,
            products = state.products,
            categories = state.categories,
            defaultMinStock = state.settings.defaultMinStock,
            symbol = symbol,
            onDismiss = { newProduct = null },
            onSave = { product ->
                onSaveNewProduct(product)
                newProduct = null
            },
        )
    }
}

@Composable
private fun PurchaseHeader(onScan: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Nueva compra", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "El código de barras controla la entrada de mercancía",
                color = Soft,
                fontSize = 12.sp,
            )
        }
        FilledIconButton(onClick = onScan) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear producto")
        }
    }
}

@Composable
private fun PurchaseCatalogue(
    products: List<Product>,
    symbol: String,
    onAdd: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (products.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.AddBusiness,
            title = "Sin productos",
            detail = "Escanea el código o registra un producto nuevo",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(products, key = { it.id }) { product ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold)
                        Text(
                            "Stock ${product.stock} · costo ${Fmt.money(product.buyPrice, symbol)}",
                            color = Soft,
                            fontSize = 11.sp,
                        )
                    }
                    IconButton(onClick = { onAdd(product) }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Agregar a la compra",
                            tint = Green,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseDraftPanel(
    state: UiState,
    symbol: String,
    onQty: (Long, Int) -> Unit,
    onCost: (Long, Double) -> Unit,
    onRemove: (Long) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardElevated),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AddBusiness, contentDescription = null, tint = Green)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Por recibir (${state.purchaseCount})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (state.purchaseDraft.isNotEmpty()) {
                    TextButton(onClick = onCancel) { Text("Cancelar", color = Soft) }
                }
            }
            if (state.purchaseDraft.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.AddBusiness,
                    title = "Sin productos",
                    detail = "Cada lectura del escáner suma una unidad",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.purchaseDraft, key = { it.productId }) { line ->
                        PurchaseLineRow(
                            line = line,
                            symbol = symbol,
                            onQty = { qty -> onQty(line.productId, qty) },
                            onCost = { cost -> onCost(line.productId, cost) },
                            onRemove = { onRemove(line.productId) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            KeyValueRow(
                label = "Total de la compra",
                value = Fmt.money(state.purchaseTotal, symbol),
                valueColor = Green,
                emphasis = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onConfirm,
                enabled = state.purchaseDraft.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Confirmar y sumar al inventario") }
        }
    }
}

@Composable
private fun PurchaseLineRow(
    line: PurchaseLine,
    symbol: String,
    onQty: (Int) -> Unit,
    onCost: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    var costText by remember(line.productId) { mutableStateOf(Fmt.decimal(line.unitCost)) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "Subtotal ${Fmt.money(line.subtotal, symbol)}",
                    color = Soft,
                    fontSize = 11.sp,
                )
            }
            QuantityStepper(
                qty = line.qty,
                onDecrease = { onQty(line.qty - 1) },
                onIncrease = { onQty(line.qty + 1) },
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Quitar de la compra", tint = Red)
            }
        }
        DarkTextField(
            value = costText,
            onValueChange = { text ->
                costText = text
                Fmt.parseAmount(text)?.let(onCost)
            },
            label = "Costo unitario ($symbol)",
            keyboardType = KeyboardType.Decimal,
        )
    }
}