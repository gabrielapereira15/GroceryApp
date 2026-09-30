package com.example.gpgrocery.ui.settings

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.R
import com.example.gpgrocery.data.model.ThemeMode
import com.example.gpgrocery.data.settings.StoreSettings
import com.example.gpgrocery.domain.Csv
import com.example.gpgrocery.domain.PinHasher
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.GroupLabel
import com.example.gpgrocery.ui.components.LabeledField
import com.example.gpgrocery.ui.components.LocalSnackbar
import com.example.gpgrocery.ui.components.PrimaryButton
import com.example.gpgrocery.ui.components.RowDivider
import com.example.gpgrocery.ui.components.SegmentedControl
import com.example.gpgrocery.ui.components.SwitchRow
import com.example.gpgrocery.ui.components.TopBar
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.onboarding.PinField
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    fun saveStore(storeName: String, ownerName: String, taxRateBasisPoints: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            container.settings.updateStore(storeName, ownerName, taxRateBasisPoints)
            onDone()
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settings.setTheme(mode) }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { container.settings.setBiometric(enabled) }
    }

    fun changePin(pin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            container.settings.changePin(pin)
            onDone()
        }
    }

    fun lock() = container.session.lock()

    fun reloadSample(onDone: () -> Unit) {
        viewModelScope.launch {
            container.reloadSampleData()
            onDone()
        }
    }

    fun eraseEverything() {
        viewModelScope.launch { container.eraseEverything() }
    }

    /** Writes a CSV into the app's cache and hands back the file, ready to share. */
    fun exportSales(context: Context, onReady: (File) -> Unit) {
        viewModelScope.launch {
            val clock = container.clock
            val today = LocalDate.now(clock)
            val from = today.minusDays(29).atStartOfDay(clock.zone).toInstant().toEpochMilli()
            val to = today.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
            val lines = container.sales.linesBetween(from, to).first()
            val numbers = container.sales.salesBetween(from, to).first().associate { it.id to it.number }
            onReady(write(context, "crate-sales-${today.format(DateTimeFormatter.BASIC_ISO_DATE)}.csv", Csv.sales(lines, numbers, clock.zone)))
        }
    }

    fun exportProducts(context: Context, onReady: (File) -> Unit) {
        viewModelScope.launch {
            val products = container.products.products.first()
            val suppliers = container.suppliers.suppliers.first().associate { it.id to it.name }
            val today = LocalDate.now(container.clock)
            onReady(write(context, "crate-products-${today.format(DateTimeFormatter.BASIC_ISO_DATE)}.csv", Csv.products(products, suppliers)))
        }
    }

    private suspend fun write(context: Context, name: String, text: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(dir, name).apply { writeText(text) }
    }
}

@Composable
fun SettingsScreen(settings: StoreSettings, onBack: () -> Unit) {
    val viewModel = crateViewModel { SettingsViewModel(it) }
    val colors = Crate.colors
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    fun say(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    var storeName by rememberSaveable { mutableStateOf(settings.storeName) }
    var ownerName by rememberSaveable { mutableStateOf(settings.ownerName) }
    var taxText by rememberSaveable { mutableStateOf(BigDecimal.valueOf(settings.taxRateBasisPoints.toLong(), 2).stripTrailingZeros().toPlainString()) }
    val taxBasisPoints = taxText.trim().replace(',', '.').toBigDecimalOrNull()
        ?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal(25) }
        ?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toInt()
    val detailsChanged = storeName.trim() != settings.storeName || ownerName.trim() != settings.ownerName ||
        taxBasisPoints != settings.taxRateBasisPoints
    val detailsValid = storeName.isNotBlank() && ownerName.isNotBlank() && taxBasisPoints != null

    var changingPin by rememberSaveable { mutableStateOf(false) }
    var confirmReload by rememberSaveable { mutableStateOf(false) }
    var confirmErase by rememberSaveable { mutableStateOf(false) }
    val biometricReady = remember(context) {
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val savedMessage = stringResource(R.string.settings_saved)
    val pinChangedMessage = stringResource(R.string.settings_pin_changed)
    val reloadedMessage = stringResource(R.string.settings_reloaded)
    val shareTitle = stringResource(R.string.settings_export_share)
    val share: (File) -> Unit = { file ->
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, shareTitle))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
    ) {
        TopBar(
            title = stringResource(R.string.settings_title),
            navigationIcon = CrateIcons.ChevronLeft,
            navigationDescription = stringResource(R.string.action_back),
            onNavigate = onBack,
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupLabel(stringResource(R.string.settings_store))
            CrateCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LabeledField(stringResource(R.string.settings_store_name), storeName, { storeName = it })
                    LabeledField(stringResource(R.string.settings_owner), ownerName, { ownerName = it })
                    LabeledField(
                        label = stringResource(R.string.settings_tax),
                        value = taxText,
                        onValueChange = { taxText = it },
                        keyboardType = KeyboardType.Decimal,
                        trailing = { Text("%", color = colors.inkMuted) },
                        error = if (taxBasisPoints == null) stringResource(R.string.settings_tax_error) else null,
                        supporting = stringResource(R.string.settings_tax_help),
                    )
                    PrimaryButton(
                        stringResource(R.string.settings_save),
                        onClick = { viewModel.saveStore(storeName, ownerName, taxBasisPoints!!) { say(savedMessage) } },
                        enabled = detailsChanged && detailsValid,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            GroupLabel(stringResource(R.string.settings_appearance))
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.settings_theme_system),
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark),
                ),
                selectedIndex = settings.themeMode.ordinal,
                onSelect = { viewModel.setTheme(ThemeMode.entries[it]) },
            )

            GroupLabel(stringResource(R.string.settings_security))
            CrateCard {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SwitchRow(
                        title = stringResource(R.string.settings_fingerprint),
                        body = stringResource(if (biometricReady) R.string.settings_fingerprint_body else R.string.settings_fingerprint_unavailable),
                        checked = settings.biometricEnabled && biometricReady,
                        onCheckedChange = viewModel::setBiometric,
                        icon = CrateIcons.Fingerprint,
                        enabled = biometricReady,
                    )
                    RowDivider()
                    ActionRow(CrateIcons.Lock, stringResource(R.string.settings_change_pin), stringResource(R.string.settings_change_pin_body)) { changingPin = true }
                    RowDivider()
                    ActionRow(CrateIcons.Lock, stringResource(R.string.settings_lock_now), stringResource(R.string.settings_lock_now_body), onClick = viewModel::lock)
                }
            }

            GroupLabel(stringResource(R.string.settings_data))
            CrateCard {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    ActionRow(CrateIcons.Download, stringResource(R.string.settings_export_sales), stringResource(R.string.settings_export_sales_body)) {
                        viewModel.exportSales(context, share)
                    }
                    RowDivider()
                    ActionRow(CrateIcons.Download, stringResource(R.string.settings_export_products), stringResource(R.string.settings_export_products_body)) {
                        viewModel.exportProducts(context, share)
                    }
                    if (settings.sampleStore) {
                        RowDivider()
                        ActionRow(CrateIcons.Reset, stringResource(R.string.settings_reload_sample), stringResource(R.string.settings_reload_sample_body)) { confirmReload = true }
                    }
                    RowDivider()
                    ActionRow(
                        CrateIcons.Trash,
                        stringResource(R.string.settings_erase),
                        stringResource(R.string.settings_erase_body),
                        tint = colors.onDanger,
                    ) { confirmErase = true }
                }
            }

            GroupLabel(stringResource(R.string.settings_about))
            val version = remember(context) {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
            }
            Text(stringResource(R.string.settings_version, version), style = MaterialTheme.typography.titleSmall, color = colors.ink)
            Text(stringResource(R.string.settings_fonts), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }

    if (changingPin) {
        ChangePinDialog(
            onSave = { pin -> changingPin = false; viewModel.changePin(pin) { say(pinChangedMessage) } },
            onDismiss = { changingPin = false },
        )
    }
    if (confirmReload) {
        Confirm(
            title = stringResource(R.string.settings_reload_confirm_title),
            body = stringResource(R.string.settings_reload_confirm_body),
            confirm = stringResource(R.string.settings_reload_confirm),
            danger = false,
            onConfirm = { confirmReload = false; viewModel.reloadSample { say(reloadedMessage) } },
            onDismiss = { confirmReload = false },
        )
    }
    if (confirmErase) {
        Confirm(
            title = stringResource(R.string.settings_erase_confirm_title),
            body = stringResource(R.string.settings_erase_confirm_body),
            confirm = stringResource(R.string.settings_erase_confirm),
            danger = true,
            onConfirm = { confirmErase = false; viewModel.eraseEverything() },
            onDismiss = { confirmErase = false },
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, body: String, tint: Color = Crate.colors.onPrimarySoft, onClick: () -> Unit) {
    val colors = Crate.colors
    Surface(onClick = onClick, color = colors.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(if (tint == colors.onDanger) colors.dangerSoft else colors.primarySoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (tint == colors.onDanger) colors.onDanger else colors.ink)
                Text(body, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Icon(CrateIcons.ChevronRight, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ChangePinDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val valid = PinHasher.isValidPin(pin) && pin == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_change_pin), modifier = Modifier.semantics { heading() }) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PinField(stringResource(R.string.settings_new_pin), pin, { pin = it }, Modifier.weight(1f))
                PinField(
                    stringResource(R.string.settings_confirm_pin),
                    confirm,
                    { confirm = it },
                    Modifier.weight(1f),
                    error = if (confirm.length == 4 && confirm != pin) stringResource(R.string.setup_error_confirm) else null,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(pin) }, enabled = valid) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = Crate.colors.surface,
    )
}

@Composable
private fun Confirm(title: String, body: String, confirm: String, danger: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = Crate.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) colors.onDanger else colors.primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = colors.surface,
    )
}
