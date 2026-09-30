package com.example.gpgrocery.ui.sale

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation.toRoute
import com.example.gpgrocery.ui.navigation.NewSaleRoute
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.model.Payment
import com.example.gpgrocery.domain.Barcodes
import com.example.gpgrocery.domain.CartLine
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.domain.Quantity
import com.example.gpgrocery.ui.components.CategoryTile
import com.example.gpgrocery.ui.components.CircleIconButton
import com.example.gpgrocery.ui.components.LabeledField
import com.example.gpgrocery.ui.components.LocalSnackbar
import com.example.gpgrocery.ui.components.PrimaryButton
import com.example.gpgrocery.ui.components.QuantityStepper
import com.example.gpgrocery.ui.components.SearchField
import com.example.gpgrocery.ui.components.SegmentedControl
import com.example.gpgrocery.ui.components.TopBar
import com.example.gpgrocery.ui.components.quantityWithUnit
import com.example.gpgrocery.ui.components.rememberBarcodeScanner
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.components.unitPrice
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun NewSaleScreen(onClose: () -> Unit, onSold: (saleId: Long) -> Unit) {
    val viewModel = crateViewModel {
        NewSaleViewModel(it, createSavedStateHandle().toRoute<NewSaleRoute>().productId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val weighing by viewModel.weighing.collectAsStateWithLifecycle()
    val charging by viewModel.charging.collectAsStateWithLifecycle()
    val colors = Crate.colors
    val snackbar = LocalSnackbar.current
    var confirmingClose by rememberSaveable { mutableStateOf(false) }
    // Ringing a basket up twice costs more than one extra tap, so ask before dropping it.
    val close: () -> Unit = {
        if (state.lines.isEmpty()) {
            onClose()
        } else {
            confirmingClose = true
        }
    }
    BackHandler(enabled = state.lines.isNotEmpty()) { confirmingClose = true }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val addedTemplate = stringResource(R.string.sale_added, "%s")
    val unknownTemplate = stringResource(R.string.scan_not_found, "%s")
    val scan = rememberBarcodeScanner { code ->
        viewModel.scanned(
            code,
            onAdded = { name -> scope.launch { snackbar.showSnackbar(addedTemplate.format(name)) } },
            onUnknown = { unknown -> scope.launch { snackbar.showSnackbar(unknownTemplate.format(Barcodes.pretty(unknown))) } },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
    ) {
        TopBar(
            title = stringResource(R.string.sale_title),
            navigationIcon = CrateIcons.Close,
            navigationDescription = stringResource(R.string.sale_close),
            onNavigate = close,
        ) {
            CircleIconButton(CrateIcons.Scan, stringResource(R.string.action_scan), scan, containerColor = colors.primary, contentColor = colors.onPrimary)
        }
        SearchField(
            query = state.query,
            onQueryChange = viewModel::search,
            placeholder = stringResource(R.string.sale_search),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Box(Modifier.weight(1f)) {
            if (state.query.isBlank()) {
                FrequentGrid(state.frequent, state.inCart, viewModel::add)
            } else {
                ResultList(state.query, state.results, state.inCart) { product ->
                    // Found it: back to the frequent items and the cart, ready for the next one.
                    viewModel.add(product)
                    viewModel.search("")
                    focusManager.clearFocus()
                }
            }
        }
        CartPanel(
            state = state,
            charging = charging,
            onIncrease = viewModel::increase,
            onDecrease = viewModel::decrease,
            onRemove = viewModel::remove,
            onClear = viewModel::clear,
            onPay = viewModel::pay,
            onCashGiven = viewModel::cashGiven,
            onCharge = { viewModel.charge(onSold) },
        )
    }

    if (confirmingClose) {
        AlertDialog(
            onDismissRequest = { confirmingClose = false },
            title = { Text(stringResource(R.string.sale_discard_title)) },
            text = { Text(stringResource(R.string.sale_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingClose = false
                    onClose()
                }) { Text(stringResource(R.string.sale_discard), color = colors.onDanger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClose = false }) { Text(stringResource(R.string.sale_keep)) }
            },
            containerColor = colors.surface,
        )
    }

    weighing?.let { product ->
        WeightDialog(
            product = product,
            onDismiss = viewModel::cancelWeighing,
            onAdd = { weight -> viewModel.addWeighed(product, weight) },
        )
    }
}

@Composable
private fun FrequentGrid(products: List<ProductEntity>, inCart: Map<Long, Long>, onAdd: (ProductEntity) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.sale_frequent).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Crate.colors.inkMuted,
                modifier = Modifier.semantics { heading() },
            )
        }
        items(products, key = { it.id }) { product ->
            ProductTile(product, inCart[product.id], onClick = { onAdd(product) })
        }
    }
}

@Composable
private fun ProductTile(product: ProductEntity, quantity: Long?, onClick: () -> Unit) {
    val colors = Crate.colors
    val inSale = quantity != null
    val addLabel = stringResource(R.string.sale_add_product, product.name)
    val inCartLabel = quantity?.let { stringResource(R.string.sale_in_cart, quantityWithUnit(it, product.soldBy)) }
    Box {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .semantics { contentDescription = listOfNotNull(addLabel, inCartLabel).joinToString(". ") },
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(1.5.dp, if (inSale) colors.primary else colors.line),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                CategoryTile(product.category, size = 30.dp, corner = 10.dp, iconSize = 18.dp, photoPath = product.photoPath)
                Column {
                    Text(product.name, style = MaterialTheme.typography.labelMedium, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(unitPrice(product.priceCents, product.soldBy), style = MaterialTheme.typography.bodySmall.tabularFigures(), color = colors.inkMuted, maxLines = 1)
                }
            }
        }
        if (quantity != null) {
            Text(
                Quantity.number(quantity, product.soldBy).removeSuffix(".00"),
                style = MaterialTheme.typography.labelSmall.tabularFigures(),
                color = colors.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-6).dp)
                    .background(colors.primary, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun ResultList(query: String, results: List<ProductEntity>, inCart: Map<Long, Long>, onAdd: (ProductEntity) -> Unit) {
    val colors = Crate.colors
    if (results.isEmpty()) {
        Text(
            stringResource(R.string.sale_no_results, query),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(24.dp),
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(results, key = { it.id }) { product ->
            val quantity = inCart[product.id]
            Surface(
                onClick = { onAdd(product) },
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, if (quantity != null) colors.primary else colors.line),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryTile(product.category, size = 40.dp, corner = 12.dp, iconSize = 20.dp, photoPath = product.photoPath)
                    Column(Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(unitPrice(product.priceCents, product.soldBy), style = MaterialTheme.typography.bodySmall.tabularFigures(), color = colors.inkMuted)
                    }
                    if (quantity != null) {
                        Text(quantityWithUnit(quantity, product.soldBy), style = MaterialTheme.typography.titleSmall.tabularFigures(), color = colors.primary)
                    }
                    Icon(CrateIcons.Plus, contentDescription = null, tint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun CartPanel(
    state: SaleState,
    charging: Boolean,
    onIncrease: (Long) -> Unit,
    onDecrease: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onPay: (Payment) -> Unit,
    onCashGiven: (String) -> Unit,
    onCharge: () -> Unit,
) {
    val colors = Crate.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), clip = false),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = colors.surface,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(5.dp)
                    .background(colors.fieldLine, RoundedCornerShape(999.dp)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.sale_current),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    pluralStringResource(R.plurals.sale_item_count, state.totals.itemCount, state.totals.itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f),
                )
                if (state.lines.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.sale_clear), color = colors.onDanger, style = MaterialTheme.typography.labelLarge) }
                }
            }
            if (state.lines.isEmpty()) {
                Text(stringResource(R.string.sale_empty_hint), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            } else {
                LazyColumn(Modifier.heightIn(max = 204.dp)) {
                    items(state.lines, key = { it.productId }) { line ->
                        CartRow(line, { onIncrease(line.productId) }, { onDecrease(line.productId) }, { onRemove(line.productId) })
                    }
                }
                Totals(state)
                SegmentedControl(
                    options = Payment.entries.map { stringResource(it.label) },
                    selectedIndex = state.payment.ordinal,
                    onSelect = { onPay(Payment.entries[it]) },
                    height = 44.dp,
                )
                if (state.payment == Payment.CASH) {
                    LabeledField(
                        label = stringResource(R.string.sale_cash_given),
                        value = state.cashGiven,
                        onValueChange = onCashGiven,
                        prefix = "$",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                        supporting = state.change?.let { stringResource(R.string.sale_change_due, Money.format(it)) }
                            ?: state.cashGivenCents?.let { stringResource(R.string.sale_cash_short, Money.format(state.totals.totalCents - it)) },
                    )
                }
            }
            PrimaryButton(
                stringResource(R.string.sale_charge, Money.format(state.totals.totalCents)),
                onClick = onCharge,
                enabled = state.canCharge,
                loading = charging,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CartRow(line: CartLine, onIncrease: () -> Unit, onDecrease: () -> Unit, onRemove: () -> Unit) {
    val colors = Crate.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(line.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (line.taxable) {
                    Text(" · " + stringResource(R.string.tax_hst), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                }
            }
            val each = unitPrice(line.unitPriceCents, line.soldBy)
            Text(
                if (line.soldBy.isWeighed) stringResource(R.string.sale_weighed_line, quantityWithUnit(line.quantityMilli, line.soldBy), each)
                else stringResource(R.string.sale_each, each),
                style = MaterialTheme.typography.bodySmall.tabularFigures(),
                color = colors.inkMuted,
            )
        }
        if (line.soldBy.isWeighed) {
            IconButton(onClick = onRemove) {
                Icon(CrateIcons.Close, contentDescription = stringResource(R.string.sale_remove, line.name), tint = colors.inkMuted, modifier = Modifier.size(18.dp))
            }
        } else {
            QuantityStepper(
                amount = Quantity.number(line.quantityMilli, line.soldBy),
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                decreaseDescription = stringResource(R.string.sale_one_less, line.name),
                increaseDescription = stringResource(R.string.sale_one_more, line.name),
            )
        }
        Text(
            Money.format(line.lineTotalCents),
            style = MaterialTheme.typography.titleSmall.tabularFigures(),
            color = colors.ink,
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun Totals(state: SaleState) {
    val colors = Crate.colors
    val rate = BigDecimal.valueOf(state.taxRateBasisPoints.toLong(), 2).stripTrailingZeros().toPlainString()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Spacer(Modifier.height(4.dp))
        TotalRow(stringResource(R.string.sale_subtotal), Money.format(state.totals.subtotalCents), strong = false)
        TotalRow(stringResource(R.string.sale_tax, rate), Money.format(state.totals.taxCents), strong = false)
        TotalRow(stringResource(R.string.sale_total), Money.format(state.totals.totalCents), strong = true)
    }
}

@Composable
private fun TotalRow(label: String, value: String, strong: Boolean) {
    val colors = Crate.colors
    val style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(label, style = style, color = if (strong) colors.ink else colors.inkMuted, modifier = Modifier.weight(1f))
        Text(value, style = style.tabularFigures(), color = if (strong) colors.ink else colors.inkMuted)
    }
}

@Composable
private fun WeightDialog(product: ProductEntity, onDismiss: () -> Unit, onAdd: (Long) -> Unit) {
    val colors = Crate.colors
    var text by rememberSaveable(product.id) { mutableStateOf("") }
    val weight = Quantity.parse(text, product.soldBy)
    val unit = product.soldBy.unit?.let { stringResource(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sale_weigh_title, product.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(unitPrice(product.priceCents, product.soldBy), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                LabeledField(
                    label = stringResource(R.string.sale_weigh_field),
                    value = text,
                    onValueChange = { text = it },
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                    trailing = unit?.let { { Text(it, color = colors.inkMuted) } },
                    supporting = weight?.takeIf { it > 0 }?.let {
                        Money.format(com.example.gpgrocery.domain.Pricing.lineTotal(product.priceCents, it))
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { weight?.let(onAdd) }, enabled = weight != null && weight > 0) {
                Text(stringResource(R.string.sale_weigh_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = colors.surface,
    )
}
