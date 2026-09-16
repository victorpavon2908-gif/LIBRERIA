package com.libreriapro.ultra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.domain.Product
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.EmptyState
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.CardElevated
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Purple
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft

/**
 * Inventory management: quick search, barcode scan, create, edit, delete and manual
 * stock corrections. Every change is persisted immediately in the local database.
 */
@Composable
fun InventoryScreen(
    state: UiState,
    onScan: () -> Unit,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAdjustStock: (Product, Int) -> Unit,
    onClearPendingBarcode: () -> Unit,
    onClearPendingProduct: () -> Unit,
) {
    val symbol = state.settings.currencySymbol
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf<Product?>(null) }
    var toDelete by remember { mutableStateOf<Product?>(null) }

    // A scan on this screen opens the matching product, or the new-product form when
    // the code is still unknown.
    LaunchedEffect(state.pendingBarcode, state.pendingProductId) {
        val code = state.pendingBarcode
        val productId = state.pendingProductId
        when {
            code != null -> {
                draft = Product(name = "", barcode = code, minStock = state.settings.defaultMinStock)
                onClearPendingBarcode()
            }

            productId != null -> {
                state.products.firstOrNull { it.id == productId }?.let { draft = it }
                onClearPendingProduct()
            }
        }
    }

    val filtered = state.products.filter { product ->
        product.matches(query) && (category == null || product.category == category)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Inventario", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${state.products.size} productos · costo ${Fmt.money(state.inventoryValue, symbol)}",
                    color = Soft,
                    fontSize = 12.sp,
                )
            }
            FilledIconButton(onClick = onScan) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código")
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { draft = Product(name = "", minStock = state.settings.defaultMinStock) },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo producto")
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Buscar por nombre, código o categoría") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                unfocusedBorderColor = CardElevated,
            ),
        )
        if (state.categories.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = category == null,
                        onClick = { category = null },
                        label = { Text("Todas") },
                    )
                }
                items(state.categories) { name ->
                    FilterChip(
                        selected = category == name,
                        onClick = { category = if (category == name) null else name },
                        label = { Text(name) },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inventory2,
                title = "Sin resultados",
                detail = "Ajusta la búsqueda o registra un producto nuevo",
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { product ->
                    ProductRow(
                        product = product,
                        symbol = symbol,
                        onEdit = { draft = product },
                        onDelete = { toDelete = product },
                        onAdjust = { delta -> onAdjustStock(product, delta) },
                    )
                }
            }
        }
    }

    draft?.let { editing ->
        ProductEditorDialog(
            product = editing,
            products = state.products,
            categories = state.categories,
            defaultMinStock = state.settings.defaultMinStock,
            symbol = symbol,
            onDismiss = { draft = null },
            onSave = { product ->
                onSaveProduct(product)
                draft = null
            },
        )
    }

    toDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminar producto") },
            text = {
                Text(
                    "Se quitará \"${product.name}\" del inventario. El historial de ventas " +
                        "y el kardex se conservan.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProduct(product)
                        toDelete = null
                    },
                ) { Text("Eliminar", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

/** One inventory line with the manual -/+ stock control and the edit/delete actions. */
@Composable
private fun ProductRow(
    product: Product,
    symbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text(
                    if (product.barcode.isBlank()) "Sin código de barras" else "Código ${product.barcode}",
                    color = Soft,
                    fontSize = 11.sp,
                )
                Text(
                    "Compra ${Fmt.money(product.buyPrice, symbol)} · Venta ${Fmt.money(product.sellPrice, symbol)}" +
                        if (product.category.isBlank()) "" else " · ${product.category}",
                    color = Soft,
                    fontSize = 11.sp,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    product.stock.toString(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (product.lowStock) Red else Green,
                )
                Text("mín ${product.minStock}", color = Soft, fontSize = 10.sp)
            }
            IconButton(onClick = { onAdjust(-1) }) {
                Icon(Icons.Filled.Remove, contentDescription = "Restar una unidad", tint = Soft)
            }
            IconButton(onClick = { onAdjust(1) }) {
                Icon(Icons.Filled.Add, contentDescription = "Sumar una unidad", tint = Purple)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar producto", tint = Soft)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar producto", tint = Red)
            }
        }
    }
}