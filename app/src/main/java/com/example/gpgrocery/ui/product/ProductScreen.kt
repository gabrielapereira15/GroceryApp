package com.example.gpgrocery.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.model.StockStatus
import com.example.gpgrocery.domain.Barcodes
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.domain.Pricing
import com.example.gpgrocery.ui.components.Bar
import com.example.gpgrocery.ui.components.BarChart
import com.example.gpgrocery.ui.components.CategoryTile
import com.example.gpgrocery.ui.components.CircleIconButton
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.DetailRow
import com.example.gpgrocery.ui.components.LocalSnackbar
import com.example.gpgrocery.ui.components.OutlineButton
import com.example.gpgrocery.ui.components.PrimaryButton
import com.example.gpgrocery.ui.components.StepButton
import com.example.gpgrocery.ui.components.StockPill
import com.example.gpgrocery.ui.components.TopBar
import com.example.gpgrocery.ui.components.quantityWithUnit
import com.example.gpgrocery.ui.components.stockLabel
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.components.unitPrice
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun ProductScreen(
    productId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRestock: (supplierId: Long?) -> Unit,
    onSell: () -> Unit,
) {
    val viewModel = crateViewModel(key = "product-$productId") { ProductViewModel(it, productId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = Crate.colors
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val product = state.product

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopBar(
            title = "",
            navigationIcon = CrateIcons.ChevronLeft,
            navigationDescription = stringResource(R.string.action_back),
            onNavigate = onBack,
        ) {
            if (product != null) {
                CircleIconButton(CrateIcons.Edit, stringResource(R.string.product_edit), onEdit)
                Box {
                    CircleIconButton(CrateIcons.More, stringResource(R.string.action_more), { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = colors.surface) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), color = colors.onDanger) },
                            leadingIcon = { Icon(CrateIcons.Trash, contentDescription = null, tint = colors.onDanger) },
                            onClick = { menuOpen = false; confirmDelete = true },
                        )
                    }
                }
            }
        }

        if (product == null) {
            if (!state.loading) {
                Text(
                    stringResource(R.string.product_missing),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(20.dp),
                )
            }
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProductHeader(product)
            PriceCard(product)
            StockCard(product, onLess = { viewModel.nudge(false) }, onMore = { viewModel.nudge(true) })
            SoldCard(state)
            Column(Modifier.padding(horizontal = 4.dp)) {
                DetailRow(stringResource(R.string.product_barcode), product.barcode?.let(Barcodes::pretty) ?: stringResource(R.string.product_none))
                DetailRow(stringResource(R.string.product_supplier), state.supplier?.name ?: stringResource(R.string.product_none))
                DetailRow(
                    stringResource(R.string.product_last_restock),
                    state.lastRestock?.let {
                        stringResource(
                            R.string.product_last_restock_value,
                            shortDate(it.receivedAt),
                            quantityWithUnit(it.quantityMilli, product.soldBy),
                        )
                    } ?: stringResource(R.string.product_none),
                )
                DetailRow(stringResource(R.string.product_sold_by), stringResource(product.soldBy.label), divider = false)
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colors.line)
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlineButton(stringResource(R.string.product_restock), { onRestock(product.supplierId) }, Modifier.weight(1f), icon = CrateIcons.Truck)
            PrimaryButton(stringResource(R.string.product_sell), onSell, Modifier.weight(1f))
        }
    }

    if (confirmDelete && product != null) {
        val deletedMessage = stringResource(R.string.product_deleted, product.name)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.product_delete_title, product.name)) },
            text = { Text(stringResource(R.string.product_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete {
                        onBack()
                        scope.launch { snackbar.showSnackbar(deletedMessage) }
                    }
                }) { Text(stringResource(R.string.action_delete), color = colors.onDanger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
            containerColor = colors.surface,
        )
    }
}

@Composable
private fun ProductHeader(product: ProductEntity) {
    val colors = Crate.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CategoryTile(product.category, size = 92.dp, corner = 26.dp, iconSize = 44.dp, photoPath = product.photoPath)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                product.name,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag(stringResource(product.category.label))
                Tag(stringResource(if (product.taxable) R.string.tax_hst else R.string.tax_zero_rated))
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    val colors = Crate.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.ink,
        modifier = Modifier
            .background(colors.surface, RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

@Composable
private fun PriceCard(product: ProductEntity) {
    val margin = Pricing.marginPercent(product.priceCents, product.costCents)
    CrateCard {
        Row(Modifier.padding(vertical = 14.dp)) {
            Figure(stringResource(R.string.product_price), unitPrice(product.priceCents, product.soldBy), Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(44.dp).background(Crate.colors.divider))
            Figure(stringResource(R.string.product_cost), unitPrice(product.costCents, product.soldBy), Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(44.dp).background(Crate.colors.divider))
            Figure(
                stringResource(R.string.product_margin),
                margin?.let { stringResource(R.string.product_margin_value, oneDecimal(it)) } ?: stringResource(R.string.product_no_margin),
                Modifier.weight(1f),
                highlight = true,
            )
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    val colors = Crate.colors
    Column(modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.tabularFigures(),
            color = if (highlight) colors.primary else colors.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun StockCard(product: ProductEntity, onLess: () -> Unit, onMore: () -> Unit) {
    val colors = Crate.colors
    CrateCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.product_stock), style = MaterialTheme.typography.titleSmall, color = colors.ink, modifier = Modifier.weight(1f))
                StockPill(product.status, stockLabel(product.stockMilli, product.soldBy, product.status))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    val amount = quantityWithUnit(product.stockMilli, product.soldBy)
                    if (product.soldBy.isWeighed) {
                        Text(stringResource(R.string.product_on_shelf_weighed, amount), style = MaterialTheme.typography.headlineMedium.tabularFigures(), color = colors.ink)
                    } else {
                        Text(amount, style = MaterialTheme.typography.displaySmall.tabularFigures(), color = colors.ink)
                        Text(" " + stringResource(R.string.product_on_shelf_counted), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                StepButton(CrateIcons.Minus, stringResource(R.string.product_remove_one), onLess, filled = false)
                StepButton(CrateIcons.Plus, stringResource(R.string.product_add_one), onMore, filled = true)
            }
            StockGauge(product)
            Text(
                stringResource(R.string.product_alert_at, quantityWithUnit(product.alertBelowMilli, product.soldBy)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
        }
    }
}

/** How full the shelf is, measured against twice the alert level, with a tick where the alert is. */
@Composable
private fun StockGauge(product: ProductEntity) {
    val colors = Crate.colors
    val full = (product.alertBelowMilli * 2).coerceAtLeast(1)
    val fraction = (product.stockMilli.toFloat() / full).coerceIn(0f, 1f)
    val fill = when (product.status) {
        StockStatus.IN_STOCK -> colors.primary
        StockStatus.LOW -> colors.warningDot
        StockStatus.OUT -> colors.dangerDot
    }
    val description = stringResource(
        R.string.product_stock_gauge,
        quantityWithUnit(product.stockMilli, product.soldBy),
        quantityWithUnit(product.alertBelowMilli, product.soldBy),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(colors.divider, RoundedCornerShape(999.dp))
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .background(fill, RoundedCornerShape(999.dp)),
        )
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .height(8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) { Box(Modifier.width(2.dp).height(8.dp).background(colors.ink.copy(alpha = 0.35f))) }
    }
}

@Composable
private fun SoldCard(state: ProductState) {
    val colors = Crate.colors
    val product = state.product ?: return
    val locale = LocalLocale.current.platformLocale
    val bars = state.lastWeek.map {
        Bar(label = it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), value = it.quantityMilli)
    }
    val spoken = state.lastWeek.joinToString { day ->
        day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale) + " " + quantityWithUnitPlain(day.quantityMilli, product)
    }
    CrateCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.product_sold_7_days), style = MaterialTheme.typography.titleSmall, color = colors.ink, modifier = Modifier.weight(1f))
                Text(quantityWithUnit(state.soldThisWeekMilli, product.soldBy), style = MaterialTheme.typography.titleSmall.tabularFigures(), color = colors.ink)
            }
            BarChart(bars = bars, highlightIndex = bars.lastIndex, description = stringResource(R.string.product_sold_chart, spoken))
        }
    }
}

private fun quantityWithUnitPlain(milli: Long, product: ProductEntity): String =
    com.example.gpgrocery.domain.Quantity.number(milli, product.soldBy)

private fun oneDecimal(value: Double): String = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString()

@Composable
private fun shortDate(millis: Long): String {
    val locale = LocalLocale.current.platformLocale
    val date = Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}
