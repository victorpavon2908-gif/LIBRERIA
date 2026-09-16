package com.libreriapro.ultra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.domain.DailySales
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.Routes
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.AlertCard
import com.libreriapro.ultra.ui.components.EmptyState
import com.libreriapro.ultra.ui.components.LocalCompactLayout
import com.libreriapro.ultra.ui.components.MetricCard
import com.libreriapro.ultra.ui.components.QuickAction
import com.libreriapro.ultra.ui.components.SectionTitle
import com.libreriapro.ultra.ui.theme.Blue
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Purple
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft

/** Dashboard with the real numbers of the day, the week and the stock alerts. */
@Composable
fun HomeScreen(state: UiState, onNavigate: (String) -> Unit, onScan: () -> Unit) {
    val compact = LocalCompactLayout.current
    val symbol = state.settings.currencySymbol
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (compact) 16.dp else 26.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Hola, ${state.settings.cashierName}",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text("Bienvenido a ${state.settings.businessName}", color = Soft, fontSize = 13.sp)
        }
        item { HeroSalesCard(state) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    icon = Icons.Filled.Inventory2,
                    title = "Productos",
                    value = state.products.size.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Filled.Warning,
                    title = "Stock bajo",
                    value = state.lowStock.size.toString(),
                    modifier = Modifier.weight(1f),
                    accent = if (state.lowStock.isEmpty()) Green else Red,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    icon = Icons.Filled.AddBusiness,
                    title = "Compras 7 días",
                    value = Fmt.money(state.weekPurchaseTotal, symbol),
                    modifier = Modifier.weight(1f),
                    accent = Blue,
                )
                MetricCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Utilidad hoy",
                    value = Fmt.money(state.todayProfit, symbol),
                    modifier = Modifier.weight(1f),
                    accent = Green,
                )
            }
        }
        item { SectionTitle("Acciones rápidas") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(
                    icon = Icons.Filled.QrCodeScanner,
                    label = "Escanear",
                    accent = Blue,
                    modifier = Modifier.weight(1f),
                ) { onScan() }
                QuickAction(
                    icon = Icons.Filled.ShoppingCart,
                    label = "Venta",
                    accent = Purple,
                    modifier = Modifier.weight(1f),
                ) { onNavigate(Routes.SALES) }
                QuickAction(
                    icon = Icons.Filled.AddBusiness,
                    label = "Compra",
                    accent = Green,
                    modifier = Modifier.weight(1f),
                ) { onNavigate(Routes.PURCHASE) }
            }
        }
        item {
            SectionTitle(
                text = "Ventas de la semana",
                trailing = Fmt.money(state.weekSalesTotal, symbol),
            )
        }
        item { WeeklyBars(state.dailyTotals) }
        item { SectionTitle("Alertas de stock", trailing = state.lowStock.size.toString()) }
        if (state.lowStock.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "Todo en orden",
                    detail = "Ningún producto está por debajo del mínimo",
                )
            }
        } else {
            items(state.lowStock.take(6)) { product ->
                AlertCard(
                    title = product.name,
                    detail = "Quedan ${product.stock} unidades · mínimo ${product.minStock}",
                )
            }
        }
    }
}

/** Gradient card with today's takings. */
@Composable
private fun HeroSalesCard(state: UiState) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(
            Modifier
                .background(Brush.linearGradient(listOf(Purple, Blue)))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Ventas de hoy", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Text(
                    Fmt.money(state.todayTotal, state.settings.currencySymbol),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "${state.todaySalesCount} tickets · ${state.todayItems} artículos · " +
                        "utilidad ${Fmt.money(state.todayProfit, state.settings.currencySymbol)}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.height(34.dp),
            )
        }
    }
}

/** Simple bar chart with the totals of the last seven days. */
@Composable
private fun WeeklyBars(days: List<DailySales>) {
    val max = days.maxOfOrNull { it.total }?.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    if (day.total > 0) Fmt.decimal(day.total) else "-",
                    color = Soft,
                    fontSize = 9.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((day.total / max).toFloat().coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(Purple, Blue))),
                )
                Spacer(Modifier.height(6.dp))
                Text(day.label.replaceFirstChar { it.uppercase() }, color = Soft, fontSize = 11.sp)
            }
        }
    }
}