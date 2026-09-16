package com.libreriapro.ultra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.domain.PaymentMethod
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.EmptyState
import com.libreriapro.ultra.ui.components.KeyValueRow
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.CardElevated
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Soft

/** Checkout: review the sale, pick the payment method and close it. */
@Composable
fun CheckoutScreen(
    state: UiState,
    onConfirm: (PaymentMethod) -> Unit,
    onBack: () -> Unit,
) {
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    val symbol = state.settings.currencySymbol

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text("Cobro", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Revisa la venta y confirma el pago", color = Soft, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))

        if (state.cart.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ShoppingCart,
                title = "No hay venta en curso",
                detail = "Agrega productos desde la pantalla de Ventas",
            )
        } else {
            Card(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardColor),
            ) {
                LazyColumn(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.cart, key = { it.productId }) { line ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(line.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${line.qty} × ${Fmt.money(line.unitPrice, symbol)}",
                                    color = Soft,
                                    fontSize = 11.sp,
                                )
                            }
                            Text(Fmt.money(line.subtotal, symbol), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardElevated),
            ) {
                Column(Modifier.padding(16.dp)) {
                    KeyValueRow("Artículos", state.cartCount.toString())
                    Spacer(Modifier.height(6.dp))
                    KeyValueRow(
                        label = "Total a cobrar",
                        value = Fmt.money(state.cartTotal, symbol),
                        valueColor = Green,
                        emphasis = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Método de pago", color = Soft, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.entries.forEach { option ->
                            FilterChip(
                                selected = method == option,
                                onClick = { method = option },
                                label = { Text(option.label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { onConfirm(method) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Finalizar venta · ${Fmt.money(state.cartTotal, symbol)}") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("Volver a la venta", color = Soft) }
        }
    }
}