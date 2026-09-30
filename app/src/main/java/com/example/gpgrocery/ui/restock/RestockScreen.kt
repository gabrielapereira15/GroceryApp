package com.example.gpgrocery.ui.restock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation.toRoute
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.SupplierEntity
import com.example.gpgrocery.data.model.StockStatus
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.ui.components.CategoryTile
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.EmptyState
import com.example.gpgrocery.ui.components.GroupLabel
import com.example.gpgrocery.ui.components.LabeledField
import com.example.gpgrocery.ui.components.LocalSnackbar
import com.example.gpgrocery.ui.components.PrimaryButton
import com.example.gpgrocery.ui.components.QuantityStepper
import com.example.gpgrocery.ui.components.QuietButton
import com.example.gpgrocery.ui.components.TopBar
import com.example.gpgrocery.ui.components.quantityWithUnit
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.navigation.RestockRoute
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.launch

@Composable
fun RestockScreen(onBack: () -> Unit, onReceived: (restockId: Long) -> Unit) {
    val viewModel = crateViewModel {
        val route = createSavedStateHandle().toRoute<RestockRoute>()
        RestockViewModel(it, route.supplierId, route.productId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val receiving by viewModel.receiving.collectAsStateWithLifecycle()
    val colors = Crate.colors
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var pickingSupplier by rememberSaveable { mutableStateOf(false) }
    var addingProduct by rememberSaveable { mutableStateOf(false) }
    var addingSupplier by rememberSaveable { mutableStateOf(false) }
    val receivedTemplate = stringResource(R.string.restock_received_message, "%s")

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
    ) {
        TopBar(
            title = stringResource(R.string.restock_title),
            navigationIcon = CrateIcons.ChevronLeft,
            navigationDescription = stringResource(R.string.action_back),
            onNavigate = onBack,
        )
        if (state.loading) return@Column
        if (state.suppliers.isEmpty()) {
            EmptyState(
                icon = CrateIcons.Truck,
                title = stringResource(R.string.restock_no_suppliers_title),
                body = stringResource(R.string.restock_no_suppliers_body),
                action = stringResource(R.string.restock_add_supplier),
                onAction = { addingSupplier = true },
            )
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SupplierCard(state.supplier) { pickingSupplier = true } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LabeledField(
                            label = stringResource(R.string.restock_invoice),
                            value = state.invoice,
                            onValueChange = viewModel::setInvoice,
                            modifier = Modifier.weight(1f),
                        )
                        LabeledField(
                            label = stringResource(R.string.restock_received),
                            value = stringResource(R.string.restock_today),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Text(
                            stringResource(R.string.restock_suggested),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { heading() },
                        )
                        Text(stringResource(R.string.restock_suggested_note), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    }
                }
                if (state.lines.isEmpty()) {
                    item { Text(stringResource(R.string.restock_nothing), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted) }
                }
                items(state.lines, key = { it.product.id }) { line ->
                    DeliveryLineCard(
                        line = line,
                        onStep = { up -> viewModel.step(line, up) },
                        onCost = { viewModel.setCost(line.product.id, it) },
                        onRemove = { viewModel.remove(line.product.id) },
                    )
                }
                item {
                    Surface(
                        onClick = { addingProduct = true },
                        shape = RoundedCornerShape(16.dp),
                        color = colors.background,
                        border = BorderStroke(2.dp, colors.outlineAccent),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier
                                .heightIn(min = 52.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(CrateIcons.Plus, contentDescription = null, tint = colors.primary)
                            Text(" " + stringResource(R.string.restock_add_product), style = MaterialTheme.typography.labelLarge, color = colors.primary)
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = colors.line)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    Text(
                        pluralStringResource(R.plurals.restock_products, state.lines.size, state.lines.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                    )
                    Text(Money.format(state.totalCents), style = MaterialTheme.typography.headlineSmall.tabularFigures(), color = colors.ink)
                }
                PrimaryButton(
                    stringResource(R.string.restock_receive),
                    onClick = {
                        viewModel.receive { id, supplierName ->
                            onReceived(id)
                            scope.launch { snackbar.showSnackbar(receivedTemplate.format(supplierName)) }
                        }
                    },
                    enabled = state.canReceive,
                    loading = receiving,
                    icon = CrateIcons.Check,
                )
            }
        }
    }

    if (pickingSupplier) {
        SupplierSheet(
            suppliers = state.suppliers,
            selected = state.supplier,
            onPick = { viewModel.choose(it); pickingSupplier = false },
            onAddNew = { pickingSupplier = false; addingSupplier = true },
            onDismiss = { pickingSupplier = false },
        )
    }
    if (addingProduct) {
        AddProductSheet(
            supplierName = state.supplier?.name.orEmpty(),
            fromSupplier = state.fromSupplier,
            everythingElse = state.everythingElse,
            onPick = { viewModel.add(it); addingProduct = false },
            onDismiss = { addingProduct = false },
        )
    }
    if (addingSupplier) {
        AddSupplierDialog(
            onAdd = { name, days -> viewModel.addSupplier(name, days); addingSupplier = false },
            onDismiss = { addingSupplier = false },
        )
    }
}

@Composable
private fun SupplierCard(supplier: SupplierEntity?, onChange: () -> Unit) {
    val colors = Crate.colors
    CrateCard(onClick = onChange) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(colors.category(com.example.gpgrocery.data.model.Category.DRINKS).container, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(CrateIcons.Truck, contentDescription = null, tint = colors.category(com.example.gpgrocery.data.model.Category.DRINKS).content) }
            Column(Modifier.weight(1f)) {
                Text(supplier?.name ?: stringResource(R.string.restock_supplier_pick), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                supplier?.deliveryDays?.let {
                    Text(stringResource(R.string.restock_delivers, it), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
            }
            Text(stringResource(R.string.restock_supplier_change), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        }
    }
}

@Composable
private fun DeliveryLineCard(line: DeliveryLine, onStep: (Boolean) -> Unit, onCost: (String) -> Unit, onRemove: () -> Unit) {
    val colors = Crate.colors
    val product = line.product
    CrateCard {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(product.category, size = 40.dp, corner = 12.dp, iconSize = 20.dp, photoPath = product.photoPath)
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val stock = quantityWithUnit(product.stockMilli, product.soldBy)
                    Text(
                        when (product.status) {
                            StockStatus.OUT -> stringResource(R.string.restock_state_out)
                            StockStatus.LOW -> stringResource(R.string.restock_state_low, stock, quantityWithUnit(product.alertBelowMilli, product.soldBy))
                            StockStatus.IN_STOCK -> stringResource(R.string.restock_state_ok, stock)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (product.status) {
                            StockStatus.OUT -> colors.onDanger
                            StockStatus.LOW -> colors.onWarning
                            StockStatus.IN_STOCK -> colors.inkMuted
                        },
                    )
                }
                Text(Money.format(line.lineTotalCents), style = MaterialTheme.typography.titleSmall.tabularFigures(), color = colors.ink)
                IconButton(onClick = onRemove) {
                    Icon(CrateIcons.Close, contentDescription = stringResource(R.string.restock_remove, product.name), tint = colors.inkMuted, modifier = Modifier.size(18.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuantityStepper(
                    amount = line.draft.quantity.ifBlank { "0" } + (product.soldBy.unit?.let { " " + stringResource(it) } ?: ""),
                    onDecrease = { onStep(false) },
                    onIncrease = { onStep(true) },
                    decreaseDescription = stringResource(R.string.restock_fewer, product.name),
                    increaseDescription = stringResource(R.string.restock_more, product.name),
                )
                Text("×", style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                val costLabel = stringResource(R.string.restock_unit_cost)
                OutlinedTextField(
                    value = line.draft.unitCost,
                    onValueChange = onCost,
                    singleLine = true,
                    prefix = { Text("$", color = colors.inkMuted) },
                    textStyle = MaterialTheme.typography.bodyMedium.tabularFigures(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = line.unitCostCents == null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.fieldLine,
                    ),
                    modifier = Modifier
                        .width(120.dp)
                        .semantics { contentDescription = costLabel },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierSheet(
    suppliers: List<SupplierEntity>,
    selected: SupplierEntity?,
    onPick: (SupplierEntity) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Crate.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surface) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
            item { GroupLabel(stringResource(R.string.restock_supplier_pick), Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            items(suppliers, key = { it.id }) { supplier ->
                Surface(onClick = { onPick(supplier) }, color = if (supplier.id == selected?.id) colors.primarySoft else colors.surface) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(supplier.name, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                            supplier.deliveryDays?.let { Text(stringResource(R.string.restock_delivers, it), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted) }
                        }
                        if (supplier.id == selected?.id) Icon(CrateIcons.Check, contentDescription = null, tint = colors.primary)
                    }
                }
            }
            item { QuietButton(stringResource(R.string.restock_add_supplier), onAddNew, Modifier.padding(horizontal = 12.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(
    supplierName: String,
    fromSupplier: List<ProductEntity>,
    everythingElse: List<ProductEntity>,
    onPick: (ProductEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Crate.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
            item {
                Text(
                    stringResource(R.string.restock_add_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            if (fromSupplier.isNotEmpty()) {
                item { GroupLabel(stringResource(R.string.restock_add_from_supplier, supplierName), Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) }
                items(fromSupplier, key = { it.id }) { PickRow(it, onPick) }
            }
            if (everythingElse.isNotEmpty()) {
                item { GroupLabel(stringResource(R.string.restock_add_everything_else), Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) }
                items(everythingElse, key = { it.id }) { PickRow(it, onPick) }
            }
        }
    }
}

@Composable
private fun PickRow(product: ProductEntity, onPick: (ProductEntity) -> Unit) {
    val colors = Crate.colors
    Surface(onClick = { onPick(product) }, color = colors.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryTile(product.category, size = 36.dp, corner = 10.dp, iconSize = 18.dp, photoPath = product.photoPath)
            Text(product.name, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(quantityWithUnit(product.stockMilli, product.soldBy), style = MaterialTheme.typography.bodySmall.tabularFigures(), color = colors.inkMuted)
        }
    }
}

@Composable
private fun AddSupplierDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restock_add_supplier)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(stringResource(R.string.restock_supplier_name), name, { name = it })
                LabeledField(
                    stringResource(R.string.restock_supplier_days),
                    days,
                    { days = it },
                    placeholder = stringResource(R.string.restock_supplier_days_hint),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, days) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = Crate.colors.surface,
    )
}
