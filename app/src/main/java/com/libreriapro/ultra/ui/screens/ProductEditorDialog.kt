package com.libreriapro.ultra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.components.DarkTextField
import com.libreriapro.ultra.ui.scanner.BarcodeScannerDialog
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Purple
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft

/**
 * Product form. The barcode is the key of the catalogue: when the typed or scanned
 * code already belongs to another product the form warns about it so the same code
 * never creates a duplicate.
 */
@Composable
fun ProductEditorDialog(
    product: Product,
    products: List<Product>,
    categories: List<String>,
    defaultMinStock: Int,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
) {
    var name by remember(product.id) { mutableStateOf(product.name) }
    var barcode by remember(product.id) { mutableStateOf(product.barcode) }
    var category by remember(product.id) { mutableStateOf(product.category) }
    var stock by remember(product.id) { mutableStateOf(product.stock.toString()) }
    var minStock by remember(product.id) {
        mutableStateOf((product.minStock.takeIf { it > 0 } ?: defaultMinStock).toString())
    }
    var buyPrice by remember(product.id) { mutableStateOf(Fmt.decimal(product.buyPrice)) }
    var sellPrice by remember(product.id) { mutableStateOf(Fmt.decimal(product.sellPrice)) }
    var scanning by remember { mutableStateOf(false) }

    val duplicate = products.firstOrNull { other ->
        other.id != product.id &&
            barcode.isNotBlank() &&
            other.barcode.equals(barcode.trim(), ignoreCase = true)
    }

    val buildProduct: () -> Product = {
        product.copy(
            name = name.trim(),
            barcode = barcode.trim(),
            category = category.trim(),
            stock = Fmt.parseInt(stock) ?: 0,
            minStock = Fmt.parseInt(minStock) ?: 0,
            buyPrice = Fmt.parseAmount(buyPrice) ?: 0.0,
            sellPrice = Fmt.parseAmount(sellPrice) ?: 0.0,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (product.id == 0L) "Nuevo producto" else "Editar producto",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DarkTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre del producto",
                )
                DarkTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = "Código de barras",
                    trailing = {
                        IconButton(onClick = { scanning = true }) {
                            Icon(
                                Icons.Filled.QrCodeScanner,
                                contentDescription = "Escanear código de barras",
                                tint = Purple,
                            )
                        }
                    },
                )
                duplicate?.let { existing ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Red,
                                    modifier = Modifier.height(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Código ya registrado", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Pertenece a \"${existing.name}\" (stock ${existing.stock}). " +
                                    "Al guardar se actualizará ese producto, no se duplicará.",
                                color = Soft,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
                DarkTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = "Categoría",
                )
                if (categories.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.take(4).forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = { Text(option, fontSize = 11.sp) },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = "Existencias",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    DarkTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = "Stock mínimo",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkTextField(
                        value = buyPrice,
                        onValueChange = { buyPrice = it },
                        label = "Precio de compra ($symbol)",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    DarkTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it },
                        label = "Precio de venta ($symbol)",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
                val margin = (Fmt.parseAmount(sellPrice) ?: 0.0) - (Fmt.parseAmount(buyPrice) ?: 0.0)
                Text(
                    "Margen por unidad: ${Fmt.money(margin, symbol)}",
                    color = if (margin >= 0) Green else Red,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(buildProduct()) }) {
                Text("Guardar", color = Purple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )

    if (scanning) {
        BarcodeScannerDialog(
            onDismiss = { scanning = false },
            onBarcode = { code ->
                barcode = code
                scanning = false
            },
            title = "Código del producto",
            hint = "Escanea la etiqueta del producto",
        )
    }
}