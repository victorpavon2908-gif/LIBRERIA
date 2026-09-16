package com.libreriapro.ultra.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/** Every destination of the app. */
object Routes {
    const val HOME = "inicio"
    const val INVENTORY = "inventario"
    const val SALES = "ventas"
    const val CHECKOUT = "cobro"
    const val PURCHASE = "compras"
    const val REPORTS = "reportes"
    const val CASH = "caja"
    const val KARDEX = "kardex"
    const val MORE = "mas"
    const val SETTINGS = "configuracion"
}

/** A main section of the app, shown in the bottom bar and in the tablet rail. */
data class Destination(val route: String, val label: String, val icon: ImageVector)

val MainDestinations = listOf(
    Destination(Routes.HOME, "Inicio", Icons.Filled.Home),
    Destination(Routes.INVENTORY, "Inventario", Icons.Filled.Inventory2),
    Destination(Routes.SALES, "Ventas", Icons.Filled.ShoppingCart),
    Destination(Routes.PURCHASE, "Compras", Icons.Filled.AddBusiness),
    Destination(Routes.MORE, "Más", Icons.Filled.MoreHoriz),
)