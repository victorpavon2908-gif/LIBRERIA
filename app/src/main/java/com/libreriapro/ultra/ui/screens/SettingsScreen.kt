package com.libreriapro.ultra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.domain.StoreSettings
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.DarkTextField
import com.libreriapro.ultra.ui.components.KeyValueRow
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Soft

/** Shop preferences: trade name, cashier, currency and default minimum stock. */
@Composable
fun SettingsScreen(
    state: UiState,
    onSave: (StoreSettings) -> Unit,
) {
    var businessName by remember(state.settings) { mutableStateOf(state.settings.businessName) }
    var cashierName by remember(state.settings) { mutableStateOf(state.settings.cashierName) }
    var currency by remember(state.settings) { mutableStateOf(state.settings.currencySymbol) }
    var minStock by remember(state.settings) {
        mutableStateOf(state.settings.defaultMinStock.toString())
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Configuración", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Datos que se muestran en toda la aplicación", color = Soft, fontSize = 12.sp)
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardColor),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DarkTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = "Nombre del negocio",
                )
                DarkTextField(
                    value = cashierName,
                    onValueChange = { cashierName = it },
                    label = "Nombre del cajero",
                )
                DarkTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = "Símbolo de moneda",
                )
                DarkTextField(
                    value = minStock,
                    onValueChange = { minStock = it },
                    label = "Stock mínimo por defecto",
                    keyboardType = KeyboardType.Number,
                )
                Button(
                    onClick = {
                        onSave(
                            StoreSettings(
                                businessName = businessName.trim().ifBlank { "Mi librería" },
                                cashierName = cashierName.trim().ifBlank { "Cajero" },
                                currencySymbol = currency.trim().ifBlank { "C$" },
                                defaultMinStock = Fmt.parseInt(minStock) ?: 5,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar configuración") }
            }
        }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardColor),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Información de la aplicación", fontWeight = FontWeight.Bold)
                KeyValueRow("Versión", "2.1.0")
                KeyValueRow("Base de datos", "SQLite local (libreria_ultra.db)")
                KeyValueRow("Productos registrados", state.products.size.toString())
                KeyValueRow("Movimientos en kardex", state.movements.size.toString())
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        "Todo se guarda en el dispositivo: no se envía información a ningún " +
                            "servidor y no se almacenan credenciales.",
                        color = Green,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}