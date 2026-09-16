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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.domain.PaymentMethod
import com.libreriapro.ultra.domain.SalesMath
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.EmptyState
import com.libreriapro.ultra.ui.components.KeyValueRow
import com.libreriapro.ultra.ui.components.LocalCompactLayout
import com.libreriapro.ultra.ui.components.MetricCard
import com.libreriapro.ultra.ui.components.SectionTitle
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Purple
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft

/** Real reports: sales, profit, payment mix, best sellers and inventory value. */
@Composable
fun ReportsScreen(state: UiState) {
    val compact = LocalCompactLayout.current
    val symbol = state.settings.currencySymbol
    var weekMode by remember { mutableStateOf(false) }
    val sales = if (weekMode) state.salesWeek else state.salesToday
    val periodLabel = if (weekMode) "Últimos 7 días" else "Hoy"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (compact) 16.dp else 26.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Reportes", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("Resumen real de ventas, utilidad e inventario", color = Soft, fontSize = 12.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !weekMode,
                    onClick = { weekMode = false },
                    label = { Text("Hoy") },
                )
                FilterChip(
                    selected = weekMode,
                    onClick = { weekMode = true },
                    label = { Text("7 días") },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    icon = Icons.Filled.Payments,
                    title = "Ventas",
                    value = Fmt.money(SalesMath.total(sales), symbol),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Filled.BarChart,
                    title = "Tickets",
                    value = sales.size.toString(),
                    detail = "Promedio ${Fmt.money(SalesMath.averageTicket(sales), symbol)}",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Artículos",
                    value = SalesMath.itemCount(sales).toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Utilidad",
                    value = Fmt.money(SalesMath.profit(sales), symbol),
                    modifier = Modifier.weight(1f),
                    accent = Green,
                )
            }
        }
        item { SectionTitle("Métodos de pago", trailing = periodLabel) }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow(
                        "Efectivo",
                        Fmt.money(SalesMath.byPaymentMethod(sales, PaymentMethod.CASH), symbol),
                    )
                    KeyValueRow(
                        "Tarjeta",
                        Fmt.money(SalesMath.byPaymentMethod(sales, PaymentMethod.CARD), symbol),
                    )
                    KeyValueRow(
                        "Transferencia",
                        Fmt.money(SalesMath.byPaymentMethod(sales, PaymentMethod.TRANSFER), symbol),
                    )
                }
            }
        }
        item { SectionTitle("Productos más vendidos", trailing = periodLabel) }
        val bestSellers = SalesMath.bestSellers(sales, 6)
        if (bestSellers.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Todavía no hay ventas",
                    detail = "Registra una venta para ver el ranking",
                )
            }
        } else {
            items(bestSellers) { (name, qty) ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("$qty uds", fontWeight = FontWeight.Bold, color = Purple)
                    }
                }
            }
        }
        item { SectionTitle("Inventario") }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("Valor a costo", Fmt.money(state.inventoryValue, symbol))
                    KeyValueRow("Valor a venta", Fmt.money(state.inventoryRetailValue, symbol))
                    KeyValueRow(
                        "Margen potencial",
                        Fmt.money(state.inventoryRetailValue - state.inventoryValue, symbol),
                        valueColor = Green,
                    )
                    KeyValueRow("Productos registrados", state.products.size.toString())
                    KeyValueRow(
                        "Productos con stock bajo",
                        state.lowStock.size.toString(),
                        valueColor = if (state.lowStock.isEmpty()) Green else Red,
                    )
                }
            }
        }
        item { SectionTitle("Compras", trailing = "Últimos 7 días") }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("Compras registradas", state.purchasesWeek.size.toString())
                    KeyValueRow(
                        "Total invertido",
                        Fmt.money(state.weekPurchaseTotal, symbol),
                        emphasis = true,
                    )
                    val balance = state.weekSalesTotal - state.weekPurchaseTotal
                    KeyValueRow(
                        "Balance de la semana",
                        Fmt.money(balance, symbol),
                        valueColor = if (balance >= 0) Green else Red,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
            ) {
                Row(Modifier.padding(16.dp)) {
                    Text(
                        "Estos reportes se calculan directamente de la base de datos local: " +
                            "reflejan cada venta, compra y ajuste registrado.",
                        color = Soft,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}