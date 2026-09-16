package com.libreriapro.ultra.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.ui.Routes
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.SectionTitle
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Purple
import com.libreriapro.ultra.ui.theme.Soft

private data class HubEntry(
    val route: String,
    val label: String,
    val detail: String,
    val icon: ImageVector,
)

/** Hub with the secondary modules of the shop. */
@Composable
fun MoreScreen(state: UiState, onNavigate: (String) -> Unit) {
    val entries = listOf(
        HubEntry(Routes.REPORTS, "Reportes", "Ventas, utilidad e inventario valorizado", Icons.Filled.BarChart),
        HubEntry(Routes.CASH, "Caja", "Arqueo, entradas y salidas de efectivo", Icons.Filled.Payments),
        HubEntry(Routes.KARDEX, "Kardex", "Todos los movimientos de existencias", Icons.Filled.History),
        HubEntry(Routes.SETTINGS, "Configuración", "Datos del negocio, moneda y stock mínimo", Icons.Filled.Settings),
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Más", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Herramientas del negocio", color = Soft, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(entries) { entry ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(entry.route) },
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(entry.icon, contentDescription = null, tint = Purple)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(entry.label, fontWeight = FontWeight.Bold)
                            Text(entry.detail, color = Soft, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                SectionTitle("Próximos módulos")
            }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Clientes con historial de compras", color = Soft, fontSize = 12.sp)
                        Text("Proveedores con costos por producto", color = Soft, fontSize = 12.sp)
                        Text("Usuarios y roles por cajero", color = Soft, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Inventario, escaneo de códigos, ventas, compras, caja y kardex " +
                                "ya están operativos. ${state.products.size} productos registrados.",
                            color = Green,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}