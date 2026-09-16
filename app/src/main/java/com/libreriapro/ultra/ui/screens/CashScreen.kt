package com.libreriapro.ultra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
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
import com.libreriapro.ultra.domain.Dates
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.DarkTextField
import com.libreriapro.ultra.ui.components.EmptyState
import com.libreriapro.ultra.ui.components.KeyValueRow
import com.libreriapro.ultra.ui.components.MetricCard
import com.libreriapro.ultra.ui.components.SectionTitle
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft

/** Cash desk: takings of the day, manual in/out movements and the expected cash. */
@Composable
fun CashScreen(
    state: UiState,
    onAddMovement: (Boolean, String, Double) -> Unit,
) {
    val symbol = state.settings.currencySymbol
    var concept by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Caja", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("Arqueo del día en curso", color = Soft, fontSize = 12.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    icon = Icons.Filled.Payments,
                    title = "Ventas del día",
                    value = Fmt.money(state.todayTotal, symbol),
                    detail = "${state.todaySalesCount} tickets",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Esperado en caja",
                    value = Fmt.money(state.expectedCash, symbol),
                    modifier = Modifier.weight(1f),
                    accent = Green,
                )
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("Ventas en efectivo", Fmt.money(state.dayCashSales, symbol))
                    KeyValueRow(
                        "Otras formas de pago",
                        Fmt.money(state.dayCardSales + state.dayTransferSales, symbol),
                    )
                    KeyValueRow("Entradas de caja", Fmt.money(state.cashIn, symbol), valueColor = Green)
                    KeyValueRow("Salidas de caja", Fmt.money(state.cashOut, symbol), valueColor = Red)
                    KeyValueRow(
                        label = "Efectivo esperado",
                        value = Fmt.money(state.expectedCash, symbol),
                        valueColor = Green,
                        emphasis = true,
                    )
                }
            }
        }
        item { SectionTitle("Registrar movimiento") }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !income,
                            onClick = { income = false },
                            label = { Text("Salida") },
                        )
                        FilterChip(
                            selected = income,
                            onClick = { income = true },
                            label = { Text("Entrada") },
                        )
                    }
                    DarkTextField(
                        value = concept,
                        onValueChange = { concept = it },
                        label = "Concepto (cambio inicial, pago de servicios...)",
                    )
                    DarkTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Monto ($symbol)",
                        keyboardType = KeyboardType.Decimal,
                    )
                    Button(
                        onClick = {
                            val value = Fmt.parseAmount(amount)
                            if (value != null && concept.isNotBlank()) {
                                onAddMovement(income, concept, value)
                                concept = ""
                                amount = ""
                            }
                        },
                        enabled = concept.isNotBlank() && Fmt.parseAmount(amount) != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Registrar movimiento") }
                }
            }
        }
        item { SectionTitle("Movimientos de hoy", trailing = state.cashMovements.size.toString()) }
        if (state.cashMovements.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Sin movimientos manuales",
                    detail = "Las ventas en efectivo ya se reflejan en el esperado",
                )
            }
        } else {
            items(state.cashMovements, key = { it.id }) { movement ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(movement.concept, fontWeight = FontWeight.SemiBold)
                            Text(
                                Dates.dateTime(movement.dateMillis),
                                color = Soft,
                                fontSize = 11.sp,
                            )
                        }
                        Text(
                            (if (movement.isIncome) "+" else "-") + Fmt.money(movement.amount, symbol),
                            fontWeight = FontWeight.Bold,
                            color = if (movement.isIncome) Green else Red,
                        )
                    }
                }
            }
        }
    }
}