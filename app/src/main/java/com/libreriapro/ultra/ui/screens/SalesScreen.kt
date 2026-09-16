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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.libreriapro.ultra.domain.SaleLine
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.Routes
import com.libreriapro.ultra.ui.UiState
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
 * Point of sale. On a tablet the catalogue and the cart sit side by side; on a phone
 * they are stacked. Products can be added with the camera, with a hardware scanner or
 * by tapping them.
 */
@Composable
fun SalesScreen(
    state: UiState,
    onScan: () -> Unit,
    onNavigate: (String) -> Unit,
    onAdd: (Product) -> Unit,
    onIncrease: (Long) -> Unit,
    onDecrease: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClearCart: () -> Unit,
) {
    val compact = LocalCompactLayout.current
    var query by remember { mutableStateOf("") }
    val available = state.products.filter { it.stock > 0 && it.matches(query) }
    val goToCheckout = { onNavigate(Routes.CHECKOUT) }

    if (compact) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            SalesHeader(onScan)
            Spacer(Modifier.height(10.dp))
            SalesSearchField(query) { query = it }
            Spacer(Modifier.height(10.dp))
            ProductPicker(
                products = available,
                symbol = state.settings.currencySymbol,
                onAdd = onAdd,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(10.dp))
            CartPanel(
                state = state,
                onIncrease = onIncrease,
                onDecrease = onDecrease,
                onRemove = onRemove,
                onClear = onClearCart,
                onCheckout = goToCheckout,
                modifier = Modifier.heightIn(max = 280.dp),
            )
        }
    } else {
        Row(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(Modifier.weight(1f)) {
                SalesHeader(onScan)
                Spacer(Modifier.height(10.dp))
                SalesSearchField(query) { query = it }
                Spacer(Modifier.height(10.dp))
                ProductPicker(
                    products = available,
                    symbol = state.settings.currencySymbol,
                    onAdd = onAdd,
                    modifier = Modifier.weight(1f),
                )
            }
            CartPanel(
                state = state,
                onIncrease = onIncrease,
                onDecrease = onDecrease,
                onRemove = onRemove,
                onClear = onClearCart,
                onCheckout = goToCheckout,
                modifier = Modifier
                    .width(420.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun SalesHeader(onScan: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Nueva venta", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Escanea un código o toca un producto para agregarlo",
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
private fun SalesSearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Buscar producto por nombre, código o categoría") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple,
            unfocusedBorderColor = CardElevated,
        ),
    )
}

/** Catalogue of sellable products. */
@Composable
private fun ProductPicker(
    products: List<Product>,
    symbol: String,
    onAdd: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (products.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Search,
            title = "Sin productos disponibles",
            detail = "Registra productos en el inventario para poder venderlos",
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
                            buildString {
                                if (product.barcode.isNotBlank()) append("Código ${product.barcode} · ")
                                append("Stock ${product.stock}")
                            },
                            color = Soft,
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        Fmt.money(product.sellPrice, symbol),
                        fontWeight = FontWeight.Bold,
                        color = Green,
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { onAdd(product) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar al carrito", tint = Purple)
                    }
                }
            }
        }
    }
}

/** Cart with the running total and the checkout shortcut. */
@Composable
private fun CartPanel(
    state: UiState,
    onIncrease: (Long) -> Unit,
    onDecrease: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardElevated),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Purple)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Carrito (${state.cartCount})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (state.cart.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Vaciar", color = Soft) }
                }
            }
            if (state.cart.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Carrito vacío",
                    detail = "Escanea o toca un producto para comenzar la venta",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.cart, key = { it.productId }) { line ->
                        CartLineRow(
                            line = line,
                            onIncrease = { onIncrease(line.productId) },
                            onDecrease = { onDecrease(line.productId) },
                            onRemove = { onRemove(line.productId) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            KeyValueRow(
                label = "Total",
                value = Fmt.money(state.cartTotal, state.settings.currencySymbol),
                valueColor = Green,
                emphasis = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCheckout,
                enabled = state.cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cobrar") }
        }
    }
}

@Composable
private fun CartLineRow(
    line: SaleLine,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "${line.qty} × ${Fmt.decimal(line.unitPrice)} = ${Fmt.decimal(line.subtotal)}",
                color = Soft,
                fontSize = 11.sp,
            )
        }
        QuantityStepper(qty = line.qty, onDecrease = onDecrease, onIncrease = onIncrease)
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Quitar del carrito", tint = Red)
        }
    }
}