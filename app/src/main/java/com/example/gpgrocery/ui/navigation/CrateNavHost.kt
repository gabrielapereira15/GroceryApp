package com.example.gpgrocery.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.gpgrocery.data.settings.StoreSettings
import com.example.gpgrocery.ui.activity.ActivityScreen
import com.example.gpgrocery.ui.activity.DeliveryScreen
import com.example.gpgrocery.ui.components.CrateBottomBar
import com.example.gpgrocery.ui.components.LocalSnackbar
import com.example.gpgrocery.ui.components.Tab
import com.example.gpgrocery.ui.home.HomeScreen
import com.example.gpgrocery.ui.insights.InsightsScreen
import com.example.gpgrocery.ui.inventory.InventoryScreen
import com.example.gpgrocery.ui.product.EditProductScreen
import com.example.gpgrocery.ui.product.ProductScreen
import com.example.gpgrocery.ui.restock.RestockScreen
import com.example.gpgrocery.ui.sale.NewSaleScreen
import com.example.gpgrocery.ui.sale.ReceiptScreen
import com.example.gpgrocery.ui.settings.SettingsScreen
import com.example.gpgrocery.ui.theme.Crate
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object InventoryRoute
@Serializable data object ActivityRoute
@Serializable data object InsightsRoute
@Serializable data class ProductRoute(val id: Long)
@Serializable data class EditProductRoute(val id: Long = 0, val barcode: String? = null)
@Serializable data class NewSaleRoute(val productId: Long = 0)
@Serializable data class ReceiptRoute(val saleId: Long, val justSold: Boolean = false)
@Serializable data class RestockRoute(val supplierId: Long = 0, val productId: Long = 0)
@Serializable data class DeliveryRoute(val restockId: Long)
@Serializable data object SettingsRoute

/** The store: four tabs and the task screens they open. */
@Composable
fun CrateNavHost(settings: StoreSettings) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val tab = when {
        destination == null -> Tab.HOME
        destination.hasRoute<HomeRoute>() -> Tab.HOME
        destination.hasRoute<InventoryRoute>() -> Tab.INVENTORY
        destination.hasRoute<ActivityRoute>() -> Tab.ACTIVITY
        destination.hasRoute<InsightsRoute>() -> Tab.INSIGHTS
        else -> null
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            containerColor = Crate.colors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbar, modifier = if (tab == null) Modifier.navigationBarsPadding() else Modifier) {
                    Snackbar(
                        it,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Crate.colors.ink,
                        contentColor = Crate.colors.background,
                        actionColor = Crate.colors.heroLine,
                    )
                }
            },
            bottomBar = {
                if (tab != null) {
                    CrateBottomBar(
                        current = tab,
                        onSelect = { nav.navigateToTab(it) },
                        onSell = { nav.navigate(NewSaleRoute()) },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = HomeRoute,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                enterTransition = { enter() },
                exitTransition = { fadeOut(tween(140)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { popExit() },
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        settings = settings,
                        onOpenProduct = { nav.navigate(ProductRoute(it)) },
                        onAddProduct = { nav.navigate(EditProductRoute()) },
                        onNewSale = { nav.navigate(NewSaleRoute()) },
                        onRestock = { nav.navigate(RestockRoute()) },
                        onStockCount = { nav.navigateToTab(Tab.INVENTORY) },
                        onInsights = { nav.navigateToTab(Tab.INSIGHTS) },
                        onSettings = { nav.navigate(SettingsRoute) },
                        onScanned = { productId, barcode ->
                            if (productId != null) nav.navigate(ProductRoute(productId))
                            else nav.navigate(EditProductRoute(barcode = barcode))
                        },
                    )
                }
                composable<InventoryRoute> {
                    InventoryScreen(
                        onOpenProduct = { nav.navigate(ProductRoute(it)) },
                        onAddProduct = { barcode -> nav.navigate(EditProductRoute(barcode = barcode)) },
                    )
                }
                composable<ActivityRoute> {
                    ActivityScreen(
                        onOpenSale = { nav.navigate(ReceiptRoute(it)) },
                        onOpenDelivery = { nav.navigate(DeliveryRoute(it)) },
                    )
                }
                composable<InsightsRoute> {
                    InsightsScreen(onOpenProduct = { nav.navigate(ProductRoute(it)) })
                }
                composable<ProductRoute> {
                    val route = it.toRoute<ProductRoute>()
                    ProductScreen(
                        productId = route.id,
                        onBack = { nav.popBackStack() },
                        onEdit = { nav.navigate(EditProductRoute(id = route.id)) },
                        onRestock = { supplierId -> nav.navigate(RestockRoute(supplierId = supplierId ?: 0, productId = route.id)) },
                        onSell = { nav.navigate(NewSaleRoute(productId = route.id)) },
                    )
                }
                composable<EditProductRoute> {
                    EditProductScreen(
                        onClose = { nav.popBackStack() },
                        onSaved = { id, isNew ->
                            if (isNew) {
                                nav.navigate(ProductRoute(id)) { popUpTo<EditProductRoute> { inclusive = true } }
                            } else {
                                nav.popBackStack()
                            }
                        },
                    )
                }
                composable<NewSaleRoute> {
                    NewSaleScreen(
                        onClose = { nav.popBackStack() },
                        onSold = { saleId ->
                            nav.navigate(ReceiptRoute(saleId, justSold = true)) { popUpTo<NewSaleRoute> { inclusive = true } }
                        },
                    )
                }
                composable<ReceiptRoute> {
                    val route = it.toRoute<ReceiptRoute>()
                    ReceiptScreen(
                        saleId = route.saleId,
                        justSold = route.justSold,
                        onBack = { nav.popBackStack() },
                        onNewSale = {
                            nav.navigate(NewSaleRoute()) { popUpTo<ReceiptRoute> { inclusive = true } }
                        },
                        onRestock = { productId -> nav.navigate(RestockRoute(productId = productId)) },
                    )
                }
                composable<RestockRoute> {
                    RestockScreen(
                        onBack = { nav.popBackStack() },
                        onReceived = { restockId ->
                            nav.navigate(DeliveryRoute(restockId)) { popUpTo<RestockRoute> { inclusive = true } }
                        },
                    )
                }
                composable<DeliveryRoute> {
                    DeliveryScreen(restockId = it.toRoute<DeliveryRoute>().restockId, onBack = { nav.popBackStack() })
                }
                composable<SettingsRoute> {
                    SettingsScreen(settings = settings, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<*>.enter() =
    fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) { it / 12 }

private fun AnimatedContentTransitionScope<*>.popExit() =
    fadeOut(tween(160)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220)) { it / 12 }

/** Tabs keep their own scroll position and back stack, the usual bottom-navigation behaviour. */
fun NavController.navigateToTab(tab: Tab) {
    val route: Any = when (tab) {
        Tab.HOME -> HomeRoute
        Tab.INVENTORY -> InventoryRoute
        Tab.ACTIVITY -> ActivityRoute
        Tab.INSIGHTS -> InsightsRoute
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
