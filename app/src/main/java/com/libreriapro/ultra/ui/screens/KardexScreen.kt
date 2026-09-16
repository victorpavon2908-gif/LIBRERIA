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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.domain.Dates
import com.libreriapro.ultra.domain.StockMovement
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.EmptyState
import com.libreriapro.ultra.ui.components.LocalCompactLayout
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft

/** Kardex: every stock movement with the resulting balance. */
@Composable
fun KardexScreen(state: UiState) {
    val compact = LocalCompactLayout.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (compact) 16.dp else 26.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Kardex", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Historial de entradas, salidas y ajustes de existencias",
                color = Soft,
                fontSize = 12.sp,
            )
        }
        if (state.movements.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = "Sin movimientos",
                    detail = "Registra una compra, una venta o un ajuste para ver el kardex",
                )
            }
        } else {
            items(state.movements, key = { it.id }) { movement ->
                MovementCard(movement)
            }
        }
    }
}

@Composable
private fun MovementCard(movement: StockMovement) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(movement.productName, fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        append(movement.type.label)
                        append(" · ")
                        append(Dates.dateTime(movement.dateMillis))
                        if (movement.reference.isNotBlank()) {
                            append(" · ")
                            append(movement.reference)
                        }
                    },
                    color = Soft,
                    fontSize = 11.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Fmt.signed(movement.qtyChange),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (movement.qtyChange >= 0) Green else Red,
                )
                Text("saldo ${movement.stockAfter}", color = Soft, fontSize = 11.sp)
            }
        }
    }
}