package com.libreriapro.ultra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.libreriapro.ultra.ui.AddTarget
import com.libreriapro.ultra.domain.Dates
import com.libreriapro.ultra.domain.Sale
import com.libreriapro.ultra.ui.Fmt
import com.libreriapro.ultra.ui.LibreriaViewModel
import com.libreriapro.ultra.ui.MainDestinations
import com.libreriapro.ultra.ui.Routes
import com.libreriapro.ultra.ui.ScanTarget
import com.libreriapro.ultra.ui.ScanTarget.SALE
import com.libreriapro.ultra.ui.UiState
import com.libreriapro.ultra.ui.components.LocalCompactLayout
import com.libreriapro.ultra.ui.components.KeyValueRow
import com.libreriapro.ultra.ui.scanner.BarcodeScannerDialog
import com.libreriapro.ultra.ui.screens.CashScreen
import com.libreriapro.ultra.ui.screens.CheckoutScreen
import com.libreriapro.ultra.ui.screens.HomeScreen
import com.libreriapro.ultra.ui.screens.InventoryScreen
import com.libreriapro.ultra.ui.screens.KardexScreen
import com.libreriapro.ultra.ui.screens.MoreScreen
import com.libreriapro.ultra.ui.screens.PurchaseScreen
import com.libreriapro.ultra.ui.screens.ReportsScreen
import com.libreriapro.ultra.ui.screens.SalesScreen
import com.libreriapro.ultra.ui.screens.SettingsScreen
import com.libreriapro.ultra.ui.theme.Bg
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.LibreriaTheme
import com.libreriapro.ultra.ui.theme.NavBg
import com.libreriapro.ultra.ui.theme.Soft

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibreriaTheme {
                LibreriaApp()
            }
        }
    }
}

/**
 * Application shell: responsive scaffold (bottom bar on phones, navigation rail on
 * tablets), the barcode scanner dialog and the snackbar with the result of every
 * operation.
 */
@Composable
fun LibreriaApp(viewModel: LibreriaViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 720.dp
        CompositionLocalProvider(LocalCompactLayout provides compact) {
            Scaffold(
                containerColor = Bg,
                topBar = { LibreriaTopBar(state) },
                bottomBar = {
                    if (compact) LibreriaBottomBar(currentRoute, navController)
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    if (!compact) LibreriaRail(currentRoute, navController)
                    LibreriaNavHost(
                        navController = navController,
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (state.scanTarget != null) {
        BarcodeScannerDialog(
            onDismiss = { viewModel.endScan() },
            onBarcode = { code -> viewModel.onBarcodeScanned(code) },
            status = state.scanFeedback,
            title = when (state.scanTarget) {
                SALE -> "Escanear para la venta"
                ScanTarget.PURCHASE -> "Escanear para la compra"
                ScanTarget.INVENTORY -> "Buscar producto"
                null -> "Escanear código de barras"
            },
        )
    }

    state.lastTicket?.let { ticket ->
        ReceiptDialog(
            ticket = ticket,
            symbol = state.settings.currencySymbol,
            onDismiss = { viewModel.consumeTicket() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibreriaTopBar(state: UiState) {
    TopAppBar(
        title = {
            Column {
                Text(
                    state.settings.businessName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "LIBRERÍA · ${state.products.size} productos · ${state.lowStock.size} alertas",
                    fontSize = 10.sp,
                    color = Soft,
                    letterSpacing = 1.sp,
                )
            }
        },
        actions = {
            Text(
                Fmt.money(state.todayTotal, state.settings.currencySymbol),
                fontSize = 13.sp,
                color = Green,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Bg,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
    )
}

@Composable
private fun LibreriaBottomBar(currentRoute: String, navController: NavHostController) {
    NavigationBar(containerColor = NavBg) {
        MainDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { navigateTo(navController, destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label, fontSize = 10.sp) },
            )
        }
    }
}

@Composable
private fun LibreriaRail(currentRoute: String, navController: NavHostController) {
    NavigationRail(containerColor = NavBg) {
        Spacer(Modifier.height(14.dp))
        MainDestinations.forEach { destination ->
            NavigationRailItem(
                selected = currentRoute == destination.route,
                onClick = { navigateTo(navController, destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label, fontSize = 10.sp) },
            )
        }
    }
}

/** Moves between sections keeping one single entry per destination in the back stack. */
private fun navigateTo(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun LibreriaNavHost(
    navController: NavHostController,
    state: UiState,
    viewModel: LibreriaViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                state = state,
                onNavigate = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onScan = {
                    navController.navigate(Routes.SALES) { launchSingleTop = true }
                    viewModel.beginScan(SALE)
                },
            )
        }
        composable(Routes.INVENTORY) {
            InventoryScreen(
                state = state,
                onScan = { viewModel.beginScan(ScanTarget.INVENTORY) },
                onSaveProduct = { product -> viewModel.saveProduct(product) },
                onDeleteProduct = { product -> viewModel.deleteProduct(product) },
                onAdjustStock = { product, delta -> viewModel.adjustStock(product, delta) },
                onClearPendingBarcode = { viewModel.clearPendingBarcode() },
                onClearPendingProduct = { viewModel.clearPendingProduct() },
            )
        }
        composable(Routes.SALES) {
            SalesScreen(
                state = state,
                onScan = { viewModel.beginScan(SALE) },
                onNavigate = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onAdd = { product -> viewModel.addToCart(product) },
                onIncrease = { productId -> viewModel.incrementCartLine(productId, 1) },
                onDecrease = { productId -> viewModel.incrementCartLine(productId, -1) },
                onRemove = { productId -> viewModel.removeFromCart(productId) },
                onClearCart = { viewModel.clearCart() },
            )
        }
        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                state = state,
                onConfirm = { method ->
                    viewModel.checkout(method)
                    navController.navigate(Routes.SALES) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PURCHASE) {
            PurchaseScreen(
                state = state,
                onScan = { viewModel.beginScan(ScanTarget.PURCHASE) },
                onAdd = { product -> viewModel.addToPurchase(product) },
                onQty = { productId, qty -> viewModel.setPurchaseQty(productId, qty) },
                onCost = { productId, cost -> viewModel.setPurchaseCost(productId, cost) },
                onRemove = { productId -> viewModel.removeFromPurchase(productId) },
                onSupplier = { supplier -> viewModel.setSupplier(supplier) },
                onConfirm = { viewModel.confirmPurchase() },
                onCancel = { viewModel.clearPurchase() },
                onSaveNewProduct = { product -> viewModel.saveProduct(product, AddTarget.PURCHASE) },
                onClearPendingBarcode = { viewModel.clearPendingBarcode() },
            )
        }
        composable(Routes.REPORTS) {
            ReportsScreen(state)
        }
        composable(Routes.CASH) {
            CashScreen(
                state = state,
                onAddMovement = { income, concept, amount ->
                    viewModel.addCashMovement(income, concept, amount)
                },
            )
        }
        composable(Routes.KARDEX) {
            KardexScreen(state)
        }
        composable(Routes.MORE) {
            MoreScreen(
                state = state,
                onNavigate = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = state,
                onSave = { settings -> viewModel.updateSettings(settings) },
            )
        }
    }
}

/** Ticket summary shown right after a sale is confirmed. */
@Composable
private fun ReceiptDialog(ticket: Sale, symbol: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Venta #${ticket.id} registrada", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(Dates.dateTime(ticket.dateMillis), color = Soft, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                ticket.lines.forEach { line ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) {
                        Text(
                            "${line.qty} × ${line.name}",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                        )
                        Text(Fmt.money(line.subtotal, symbol), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                KeyValueRow("Artículos", ticket.itemCount.toString())
                Spacer(Modifier.height(4.dp))
                KeyValueRow("Método de pago", ticket.paymentMethod.label)
                Spacer(Modifier.height(4.dp))
                KeyValueRow(
                    label = "Total",
                    value = Fmt.money(ticket.total, symbol),
                    valueColor = Green,
                    emphasis = true,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}