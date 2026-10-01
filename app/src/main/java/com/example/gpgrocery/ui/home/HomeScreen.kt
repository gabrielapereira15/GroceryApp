package com.example.gpgrocery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.settings.StoreSettings
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.ui.components.CategoryTile
import com.example.gpgrocery.ui.components.CircleIconButton
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.EmptyState
import com.example.gpgrocery.ui.components.RowDivider
import com.example.gpgrocery.ui.components.SectionHeader
import com.example.gpgrocery.ui.components.Sparkline
import com.example.gpgrocery.ui.components.quantityWithUnit
import com.example.gpgrocery.ui.components.rememberBarcodeScanner
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import java.time.LocalTime
import java.time.format.TextStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    settings: StoreSettings,
    onOpenProduct: (Long) -> Unit,
    onAddProduct: () -> Unit,
    onNewSale: () -> Unit,
    onRestock: () -> Unit,
    onStockCount: () -> Unit,
    onInsights: () -> Unit,
    onSettings: () -> Unit,
    onScanned: (productId: Long?, barcode: String) -> Unit,
) {
    val viewModel = crateViewModel { HomeViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scan = rememberBarcodeScanner { code -> viewModel.lookup(code) { id -> onScanned(id, code) } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Header(
                settings = settings,
                onSettings = onSettings,
                onLock = viewModel::lock,
            )
        }
        if (!state.loading && state.productCount == 0) {
            item {
                CrateCard {
                    EmptyState(
                        icon = CrateIcons.Inventory,
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body),
                        action = stringResource(R.string.home_empty_action),
                        onAction = onAddProduct,
                    )
                }
            }
            return@LazyColumn
        }
        item { SalesHero(state) }
        item {
            QuickActions(
                onAddProduct = onAddProduct,
                onScan = scan,
                onRestock = onRestock,
                onStockCount = onStockCount,
            )
        }
        if (state.lowStock.isNotEmpty()) {
            item { LowStockCard(state.lowStock, onRestock) }
        }
        item {
            SectionHeader(
                title = stringResource(R.string.home_best_sellers),
                action = stringResource(R.string.home_see_all),
                onAction = onInsights,
            )
        }
        item {
            CrateCard {
                if (state.bestSellers.isEmpty()) {
                    EmptyState(
                        icon = CrateIcons.Receipt,
                        title = stringResource(R.string.home_no_sales_title),
                        body = stringResource(R.string.home_no_sales_body),
                        action = stringResource(R.string.action_new_sale),
                        onAction = onNewSale,
                    )
                } else {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        state.bestSellers.forEachIndexed { index, seller ->
                            BestSellerRow(seller, onClick = seller.productId?.let { id -> { onOpenProduct(id) } })
                            if (index < state.bestSellers.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(settings: StoreSettings, onSettings: () -> Unit, onLock: () -> Unit) {
    val colors = Crate.colors
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> R.string.home_greeting_morning
        hour < 18 -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_evening
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val settingsLabel = stringResource(R.string.home_open_settings)
        Box(
            Modifier
                .size(44.dp)
                .background(colors.primary, CircleShape)
                .clickable(role = Role.Button, onClickLabel = settingsLabel, onClick = onSettings)
                .semantics { contentDescription = settingsLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(initials(settings.ownerName), style = MaterialTheme.typography.titleSmall, color = colors.onPrimary)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(greeting, settings.ownerName), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1)
            Text(
                settings.storeName,
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (settings.hasPin) {
            CircleIconButton(CrateIcons.Lock, stringResource(R.string.home_lock), onLock)
        }
    }
}

private fun initials(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
private fun SalesHero(state: HomeState) {
    val colors = Crate.colors
    val dayName = state.comparedDay.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale)
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.hero, RoundedCornerShape(26.dp))
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_today_sales),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onHero.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            state.changePercent?.let { change -> ChangeChip(change, dayName) }
        }
        Text(
            Money.format(state.todayCents),
            style = MaterialTheme.typography.displayLarge.tabularFigures(),
            color = colors.onHero,
        )
        Sparkline(
            values = state.sparkline,
            lineColor = colors.heroLine,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 2.dp),
        )
        Row(Modifier.fillMaxWidth()) {
            HeroFigure(state.salesCount.toString(), pluralStringResource(R.plurals.home_sales_count, state.salesCount), Modifier.weight(1f))
            HeroFigure(state.itemsSold.toString(), pluralStringResource(R.plurals.home_items_count, state.itemsSold), Modifier.weight(1f))
            HeroFigure(Money.format(state.averageBasketCents), stringResource(R.string.home_avg_basket), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChangeChip(change: Double, dayName: String) {
    val colors = Crate.colors
    val rounded = abs(change).roundToInt().toString()
    val up = change >= 0
    val description = stringResource(if (up) R.string.home_change_description_up else R.string.home_change_description_down, rounded, dayName)
    Row(
        Modifier
            .background(colors.heroChip, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(if (up) CrateIcons.TrendUp else CrateIcons.TrendDown, contentDescription = null, tint = colors.onHero, modifier = Modifier.size(14.dp))
        Text(
            stringResource(if (up) R.string.home_change_up else R.string.home_change_down, rounded, dayName),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onHero,
        )
    }
}

@Composable
private fun HeroFigure(value: String, label: String, modifier: Modifier) {
    val colors = Crate.colors
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.titleMedium.tabularFigures(), color = colors.onHero)
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onHero.copy(alpha = 0.8f))
    }
}

@Composable
private fun QuickActions(onAddProduct: () -> Unit, onScan: () -> Unit, onRestock: () -> Unit, onStockCount: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAction(CrateIcons.Plus, stringResource(R.string.home_quick_add), onAddProduct, Modifier.weight(1f))
        QuickAction(CrateIcons.Scan, stringResource(R.string.home_quick_scan), onScan, Modifier.weight(1f))
        QuickAction(CrateIcons.Truck, stringResource(R.string.home_quick_restock), onRestock, Modifier.weight(1f))
        QuickAction(CrateIcons.StockCount, stringResource(R.string.home_quick_count), onStockCount, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = Crate.colors
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(colors.surface, RoundedCornerShape(18.dp))
                .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.ink, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun LowStockCard(products: List<ProductEntity>, onRestock: () -> Unit) {
    val colors = Crate.colors
    val names = when (products.size) {
        1 -> products[0].name
        2 -> stringResource(R.string.home_low_names_two, products[0].name, products[1].name)
        else -> pluralStringResource(R.plurals.home_low_names_more, products.size - 2, products[0].name, products[1].name, products.size - 2)
    }
    CrateCard(onClick = onRestock, color = colors.warningCard, borderColor = colors.warningCardLine) {
        Row(
            Modifier.padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(colors.warningTile, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(CrateIcons.Alert, contentDescription = null, tint = colors.onWarning, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f)) {
                Text(
                    pluralStringResource(R.plurals.home_low_title, products.size, products.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.ink,
                )
                Text(names, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(stringResource(R.string.home_low_action), style = MaterialTheme.typography.titleSmall, color = colors.onWarning)
        }
    }
}

@Composable
private fun BestSellerRow(seller: BestSeller, onClick: (() -> Unit)?) {
    val colors = Crate.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryTile(seller.category, size = 40.dp, corner = 12.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(seller.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.home_sold, quantityWithUnit(seller.quantityMilli, seller.soldBy)),
                style = MaterialTheme.typography.bodySmall.tabularFigures(),
                color = colors.inkMuted,
            )
        }
        Text(Money.format(seller.revenueCents), style = MaterialTheme.typography.titleSmall.tabularFigures(), color = colors.ink)
    }
}
