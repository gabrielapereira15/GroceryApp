package com.example.gpgrocery.ui.product

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation.toRoute
import android.net.Uri
import com.example.gpgrocery.CrateApp
import com.example.gpgrocery.R
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.ui.components.CategoryTile
import com.example.gpgrocery.ui.components.LabeledField
import com.example.gpgrocery.ui.components.LocalSnackbar
import com.example.gpgrocery.ui.components.PrimaryButton
import com.example.gpgrocery.ui.components.QuietButton
import com.example.gpgrocery.ui.components.SegmentedControl
import com.example.gpgrocery.ui.components.SwitchRow
import com.example.gpgrocery.ui.components.TopBar
import com.example.gpgrocery.ui.components.rememberBarcodeScanner
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.navigation.EditProductRoute
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun EditProductScreen(onClose: () -> Unit, onSaved: (id: Long, isNew: Boolean) -> Unit) {
    val viewModel = crateViewModel {
        val route = createSavedStateHandle().toRoute<EditProductRoute>()
        EditProductViewModel(it, route.id, route.barcode)
    }
    val form = viewModel.form
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val taxRate by viewModel.taxRateBasisPoints.collectAsStateWithLifecycle()
    val colors = Crate.colors
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val errors = form.showErrors
    val savedMessage = stringResource(R.string.edit_saved, form.name.trim())

    val close = {
        viewModel.discard()
        onClose()
    }
    BackHandler(onBack = close)

    val scan = rememberBarcodeScanner { code -> viewModel.edit { copy(barcode = code) } }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
    ) {
        TopBar(
            title = stringResource(if (viewModel.isNew) R.string.edit_new_title else R.string.edit_title),
            navigationIcon = CrateIcons.Close,
            navigationDescription = stringResource(R.string.action_close),
            onNavigate = close,
        )
        if (!viewModel.loaded) return@Column
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PhotoPicker(
                photoPath = form.photoPath,
                category = form.category,
                onPicked = viewModel::setPhoto,
                onRemove = viewModel::removePhoto,
            )
            LabeledField(
                label = stringResource(R.string.edit_name),
                value = form.name,
                onValueChange = { value -> viewModel.edit { copy(name = value) } },
                placeholder = stringResource(R.string.edit_name_hint),
                error = if (errors && form.nameMissing) stringResource(R.string.edit_error_name) else null,
            )
            LabeledField(
                label = stringResource(R.string.edit_barcode),
                value = form.barcode,
                onValueChange = { value -> viewModel.edit { copy(barcode = value) } },
                placeholder = stringResource(R.string.edit_barcode_hint),
                keyboardType = KeyboardType.Number,
                error = form.barcodeTakenBy?.let { stringResource(R.string.edit_error_barcode, it) },
                trailing = {
                    IconButton(onClick = scan) {
                        Icon(CrateIcons.Scan, contentDescription = stringResource(R.string.action_scan), tint = colors.primary)
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryPicker(form.category, { value -> viewModel.edit { copy(category = value) } }, Modifier.weight(1f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel(stringResource(R.string.edit_sold_by))
                    SegmentedControl(
                        options = SoldBy.entries.map { stringResource(it.label) },
                        selectedIndex = form.soldBy.ordinal,
                        onSelect = { index -> viewModel.edit { copy(soldBy = SoldBy.entries[index]) } },
                        height = 48.dp,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    label = stringResource(R.string.edit_price),
                    value = form.price,
                    onValueChange = { value -> viewModel.edit { copy(price = value) } },
                    prefix = "$",
                    keyboardType = KeyboardType.Decimal,
                    error = if (errors && form.priceInvalid) stringResource(R.string.edit_error_price) else null,
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = stringResource(R.string.edit_cost),
                    value = form.cost,
                    onValueChange = { value -> viewModel.edit { copy(cost = value) } },
                    prefix = "$",
                    keyboardType = KeyboardType.Decimal,
                    error = if (errors && form.costInvalid) stringResource(R.string.edit_error_cost) else null,
                    modifier = Modifier.weight(1f),
                )
            }
            form.marginPercent?.let { margin ->
                val perUnit = Money.format(form.priceCents!! - form.costCents!!)
                Text(
                    stringResource(R.string.edit_margin, BigDecimal.valueOf(margin).setScale(1, RoundingMode.HALF_UP).toPlainString(), perUnit),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (margin >= 0) colors.primaryStrong else colors.onDanger,
                    modifier = Modifier.padding(top = 0.dp),
                )
            }
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surface, border = BorderStroke(1.dp, colors.line)) {
                SwitchRow(
                    title = stringResource(R.string.edit_taxable, BigDecimal.valueOf(taxRate.toLong(), 2).stripTrailingZeros().toPlainString()),
                    body = stringResource(R.string.edit_taxable_body),
                    checked = form.taxable,
                    onCheckedChange = { value -> viewModel.edit { copy(taxable = value) } },
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
            val unit = form.soldBy.unit?.let { stringResource(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    label = stringResource(R.string.edit_stock),
                    value = form.stock,
                    onValueChange = { value -> viewModel.edit { copy(stock = value) } },
                    placeholder = "0",
                    keyboardType = if (form.soldBy.isWeighed) KeyboardType.Decimal else KeyboardType.Number,
                    trailing = unit?.let { { Text(it, color = colors.inkMuted) } },
                    error = if (errors && form.stockInvalid) stringResource(R.string.edit_error_quantity) else null,
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = stringResource(R.string.edit_alert),
                    value = form.alertBelow,
                    onValueChange = { value -> viewModel.edit { copy(alertBelow = value) } },
                    placeholder = ProductForm.DEFAULT_ALERT,
                    keyboardType = if (form.soldBy.isWeighed) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    trailing = unit?.let { { Text(it, color = colors.inkMuted) } },
                    error = if (errors && form.alertInvalid) stringResource(R.string.edit_error_quantity) else null,
                    modifier = Modifier.weight(1f),
                )
            }
            SupplierPicker(
                selectedId = form.supplierId,
                suppliers = suppliers.map { it.id to it.name },
                onSelect = { id -> viewModel.edit { copy(supplierId = id) } },
            )
        }
        PrimaryButton(
            stringResource(R.string.edit_save),
            onClick = {
                viewModel.save { id, _ ->
                    onSaved(id, viewModel.isNew)
                    scope.launch { snackbar.showSnackbar(savedMessage) }
                }
            },
            loading = viewModel.saving,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Crate.colors.inkSoft)
}

@Composable
private fun PhotoPicker(photoPath: String?, category: Category, onPicked: (Uri) -> Unit, onRemove: () -> Unit) {
    val colors = Crate.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val photos = remember { (context.applicationContext as CrateApp).container.photos }
    var pendingCamera by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val target = pendingCamera
        if (taken && target != null) onPicked(target)
        pendingCamera = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.5.dp, colors.outlineAccent),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (photoPath != null) {
                CategoryTile(category, size = 72.dp, corner = 18.dp, photoPath = photoPath)
            } else {
                Box(
                    Modifier
                        .size(72.dp)
                        .background(colors.primarySoft, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(CrateIcons.Camera, contentDescription = null, tint = colors.onPrimarySoft, modifier = Modifier.size(30.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (photoPath == null) R.string.edit_photo_add else R.string.edit_photo_change),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.ink,
                )
                Row {
                    QuietButton(stringResource(R.string.edit_photo_camera), {
                        val target = photos.newCameraTarget()
                        pendingCamera = target
                        camera.launch(target)
                    })
                    QuietButton(stringResource(R.string.edit_photo_gallery), {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                }
                if (photoPath != null) QuietButton(stringResource(R.string.edit_photo_remove), onRemove, color = colors.onDanger)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(selected: Category, onSelect: (Category) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.edit_category))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(selected.label),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                shape = RoundedCornerShape(14.dp),
                colors = pickerColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Crate.colors.surface) {
                Category.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(stringResource(category.label)) },
                        leadingIcon = { CategoryTile(category, size = 28.dp, corner = 8.dp, iconSize = 16.dp) },
                        onClick = { onSelect(category); expanded = false },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierPicker(selectedId: Long?, suppliers: List<Pair<Long, String>>, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val none = stringResource(R.string.edit_supplier_none)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.edit_supplier))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = suppliers.firstOrNull { it.first == selectedId }?.second ?: none,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                shape = RoundedCornerShape(14.dp),
                colors = pickerColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Crate.colors.surface) {
                DropdownMenuItem(text = { Text(none) }, onClick = { onSelect(null); expanded = false })
                suppliers.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun pickerColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Crate.colors.surface,
    unfocusedContainerColor = Crate.colors.surface,
    focusedBorderColor = Crate.colors.primary,
    unfocusedBorderColor = Crate.colors.fieldLine,
    focusedTextColor = Crate.colors.ink,
    unfocusedTextColor = Crate.colors.ink,
)
