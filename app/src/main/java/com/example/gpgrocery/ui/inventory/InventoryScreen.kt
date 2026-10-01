package com.example.gpgrocery.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.domain.Barcodes
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.ui.components.CategoryTile
import com.example.gpgrocery.ui.components.CircleIconButton
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.EmptyState
import com.example.gpgrocery.ui.components.FilterPill
import com.example.gpgrocery.ui.components.LocalSnackbar
import com.example.gpgrocery.ui.components.RowDivider
import com.example.gpgrocery.ui.components.ScreenHeader
import com.example.gpgrocery.ui.components.SearchField
import com.example.gpgrocery.ui.components.StatusDot
import com.example.gpgrocery.ui.components.StockPill
import com.example.gpgrocery.ui.components.rememberBarcodeScanner
import com.example.gpgrocery.ui.components.stockLabel
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.components.unitPrice
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(onOpenProduct: (Long) -> Unit, onAddProduct: (barcode: String?) -> Unit) {
    val viewModel = crateViewModel { InventoryViewModel(it) }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = Crate.colors
    var counting by remember { mutableStateOf<ProductEntity?>(null) }
    var unknownBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    val scan = rememberBarcodeScanner { code ->
        viewModel.lookup(code) { id -> if (id != null) onOpenProduct(id) else unknownBarcode = code }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = top + 16.dp, bottom = 24.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ScreenHeader(
                    title = stringResource(R.string.inventory_title),
                    subtitle = pluralStringResource(R.plurals.inventory_summary, state.total, state.total, Money.format(state.stockValueCents)),
                ) {
                    CircleIconButton(CrateIcons.Scan, stringResource(R.string.action_scan), scan)
                    CircleIconButton(
                        CrateIcons.Plus,
                        stringResource(R.string.inventory_add),
                        onClick = { onAddProduct(null) },
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    )
                }
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::search,
                    placeholder = stringResource(R.string.inventory_search),
                )
            }
        }
        item {
            FilterRow(state, viewModel::show)
        }
        when {
            state.loading -> Unit
            state.total == 0 -> item {
                EmptyState(
                    icon = CrateIcons.Inventory,
                    title = stringResource(R.string.inventory_empty_title),
                    body = stringResource(R.string.inventory_empty_body),
                    action = stringResource(R.string.inventory_add),
                    onAction = { onAddProduct(null) },
                )
            }
            state.products.isEmpty() -> item {
                EmptyState(
                    icon = CrateIcons.Search,
                    title = stringResource(R.string.inventory_no_match_title),
                    body = stringResource(R.string.inventory_no_match_body),
                )
            }
            else -> itemsIndexed(state.products, key = { _, product -> product.id }) { index, product ->
                val last = index == state.products.lastIndex
                val shape: Shape = when {
                    state.products.size == 1 -> RoundedCornerShape(22.dp)
                    index == 0 -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    last -> RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                ProductRow(
                    product = product,
                    shape = shape,
                    showDivider = !last,
                    onOpen = { onOpenProduct(product.id) },
                    onCount = { counting = product },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .animateItem(),
                )
            }
        }
    }

    counting?.let { product ->
        StockCountSheet(
            product = product,
            onDismiss = { counting = null },
            onSave = { counted ->
                viewModel.saveCount(product, counted) {
                    scope.launch { snackbar.showSnackbar(resources.getString(R.string.count_saved, product.name)) }
                }
                counting = null
            },
        )
    }

    unknownBarcode?.let { code ->
        AlertDialog(
            onDismissRequest = { unknownBarcode = null },
            title = { Text(stringResource(R.string.inventory_not_found_title)) },
            text = { Text(stringResource(R.string.inventory_not_found_body, Barcodes.pretty(code))) },
            confirmButton = {
                TextButton(onClick = { unknownBarcode = null; onAddProduct(code) }) { Text(stringResource(R.string.inventory_not_found_add)) }
            },
            dismissButton = { TextButton(onClick = { unknownBarcode = null }) { Text(stringResource(R.string.action_cancel)) } },
            containerColor = colors.surface,
        )
    }
}

@Composable
private fun FilterRow(state: InventoryState, onSelect: (StockFilter) -> Unit) {
    val colors = Crate.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterPill(stringResource(R.string.inventory_filter_all, state.total), state.filter == StockFilter.All, { onSelect(StockFilter.All) })
        }
        item {
            FilterPill(
                stringResource(R.string.inventory_filter_low, state.lowCount),
                state.filter == StockFilter.Low,
                { onSelect(StockFilter.Low) },
                leading = { StatusDot(colors.warningDot) },
            )
        }
        item {
            FilterPill(
                stringResource(R.string.inventory_filter_out, state.outCount),
                state.filter == StockFilter.Out,
                { onSelect(StockFilter.Out) },
                leading = { StatusDot(colors.dangerDot) },
            )
        }
        state.aisles.forEach { aisle ->
            item(key = aisle.name) {
                val selected = state.filter == StockFilter.Aisle(aisle)
                FilterPill(stringResource(aisle.label), selected, { onSelect(if (selected) StockFilter.All else StockFilter.Aisle(aisle)) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductRow(
    product: ProductEntity,
    shape: Shape,
    showDivider: Boolean,
    onOpen: () -> Unit,
    onCount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Crate.colors
    // Swiping a row opens the count sheet and springs back; nothing is ever dismissed.
    val swipe = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.3f })
    val scope = rememberCoroutineScope()
    val countLabel = stringResource(R.string.inventory_adjust)
    CrateCard(modifier = modifier, shape = shape as? RoundedCornerShape ?: RoundedCornerShape(0.dp), borderColor = colors.surface) {
        SwipeToDismissBox(
            state = swipe,
            enableDismissFromStartToEnd = false,
            onDismiss = { direction ->
                if (direction == SwipeToDismissBoxValue.EndToStart) onCount()
                scope.launch { swipe.reset() }
            },
            backgroundContent = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colors.primary)
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(CrateIcons.StockCount, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(22.dp))
                        Text(stringResource(R.string.inventory_swipe_action), style = MaterialTheme.typography.labelSmall, color = colors.onPrimary)
                    }
                }
            },
        ) {
            Column(Modifier.background(colors.surface)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .clickable(onClick = onOpen)
                        .semantics(mergeDescendants = true) {
                            customActions = listOf(CustomAccessibilityAction(countLabel) { onCount(); true })
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryTile(product.category, photoPath = product.photoPath)
                    Column(Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(product.category.label), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(unitPrice(product.priceCents, product.soldBy), style = MaterialTheme.typography.titleSmall.tabularFigures(), color = colors.ink)
                        StockPill(product.status, stockLabel(product.stockMilli, product.soldBy, product.status))
                    }
                }
                if (showDivider) RowDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
