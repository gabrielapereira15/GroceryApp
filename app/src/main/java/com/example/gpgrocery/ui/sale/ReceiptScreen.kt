package com.example.gpgrocery.ui.sale

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.SaleWithLines
import com.example.gpgrocery.data.model.Payment
import com.example.gpgrocery.data.model.StockStatus
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.OutlineButton
import com.example.gpgrocery.ui.components.PrimaryButton
import com.example.gpgrocery.ui.components.QuietButton
import com.example.gpgrocery.ui.components.TopBar
import com.example.gpgrocery.ui.components.quantityWithUnit
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.components.unitPrice
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class ReceiptState(
    val loading: Boolean = true,
    val sale: SaleWithLines? = null,
    val storeName: String = "",
    /** Products in this sale that are now low or out, worth a restock. */
    val runningLow: List<ProductEntity> = emptyList(),
)

class ReceiptViewModel(container: AppContainer, saleId: Long) : ViewModel() {
    val state: StateFlow<ReceiptState> = combine(
        container.sales.sale(saleId),
        container.products.products,
        container.settings.settings,
    ) { sale, products, settings ->
        val soldIds = sale?.lines?.mapNotNull { it.productId }.orEmpty().toSet()
        ReceiptState(
            loading = false,
            sale = sale,
            storeName = settings.storeName,
            runningLow = products.filter { it.id in soldIds && it.status != StockStatus.IN_STOCK },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptState())
}

@Composable
fun ReceiptScreen(
    saleId: Long,
    justSold: Boolean,
    onBack: () -> Unit,
    onNewSale: () -> Unit,
    onRestock: (productId: Long) -> Unit,
) {
    val viewModel = crateViewModel(key = "receipt-$saleId") { ReceiptViewModel(it, saleId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = Crate.colors
    val sale = state.sale
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.receipt_share_title)
    val receiptText = sale?.let { receiptText(it, state.storeName) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (justSold) {
            Spacer(Modifier.statusBarsPadding().height(20.dp))
        } else {
            TopBar(
                title = sale?.let { stringResource(R.string.receipt_title, it.sale.number) } ?: "",
                navigationIcon = CrateIcons.ChevronLeft,
                navigationDescription = stringResource(R.string.action_back),
                onNavigate = onBack,
            )
        }
        if (sale == null) {
            if (!state.loading) Text(stringResource(R.string.receipt_missing), color = colors.inkMuted, modifier = Modifier.padding(20.dp))
            return@Column
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (justSold) SuccessHeader(sale)
            ReceiptCard(sale, state.storeName)
            state.runningLow.firstOrNull()?.let { product -> RestockNudge(product) { onRestock(product.id) } }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineButton(
                    stringResource(R.string.receipt_share),
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, receiptText)
                        }
                        context.startActivity(Intent.createChooser(send, shareTitle))
                    },
                    modifier = Modifier.weight(1f),
                    icon = CrateIcons.Share,
                )
                PrimaryButton(stringResource(R.string.receipt_new_sale), onNewSale, Modifier.weight(1f))
            }
            if (justSold) {
                QuietButton(stringResource(R.string.receipt_done), onBack, Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun SuccessHeader(sale: SaleWithLines) {
    val colors = Crate.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(88.dp)
                .background(colors.primarySoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(60.dp)
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(CrateIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(32.dp)) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.receipt_complete),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        val total = Money.format(sale.sale.totalCents)
        Text(
            if (sale.sale.payment == Payment.CASH && sale.sale.cashGivenCents != null) {
                stringResource(R.string.receipt_paid_cash, total, Money.format(sale.sale.cashGivenCents - sale.sale.totalCents))
            } else {
                stringResource(R.string.receipt_paid_card, total)
            },
            style = MaterialTheme.typography.bodyLarge.tabularFigures(),
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.receipt_number, sale.sale.number.toInt(), timeOf(sale.sale.createdAt)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
    }
}

@Composable
private fun ReceiptCard(sale: SaleWithLines, storeName: String) {
    val colors = Crate.colors
    CrateCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(storeName, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(dateTimeOf(sale.sale.createdAt), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            sale.lines.forEach { line ->
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                        val each = unitPrice(line.unitPriceCents, line.soldBy)
                        Text(
                            if (line.soldBy.isWeighed) stringResource(R.string.sale_weighed_line, quantityWithUnit(line.quantityMilli, line.soldBy), each)
                            else "${quantityWithUnit(line.quantityMilli, line.soldBy)} × $each",
                            style = MaterialTheme.typography.bodySmall.tabularFigures(),
                            color = colors.inkMuted,
                        )
                    }
                    Text(
                        Money.format(line.lineTotalCents) + if (line.taxable) " " + stringResource(R.string.receipt_taxed_mark) else "",
                        style = MaterialTheme.typography.bodyMedium.tabularFigures(),
                        color = colors.ink,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            val rate = BigDecimal.valueOf(sale.sale.taxRateBasisPoints.toLong(), 2).stripTrailingZeros().toPlainString()
            ReceiptTotal(stringResource(R.string.sale_subtotal), Money.format(sale.sale.subtotalCents))
            ReceiptTotal(stringResource(R.string.sale_tax, rate), Money.format(sale.sale.taxCents))
            ReceiptTotal(stringResource(R.string.sale_total), Money.format(sale.sale.totalCents), strong = true)
            ReceiptTotal(stringResource(R.string.receipt_payment), stringResource(sale.sale.payment.label))
            sale.sale.cashGivenCents?.let { cash ->
                ReceiptTotal(stringResource(R.string.receipt_cash_given), Money.format(cash))
                ReceiptTotal(stringResource(R.string.receipt_change), Money.format(cash - sale.sale.totalCents))
            }
            if (sale.lines.any { it.taxable }) {
                Text(stringResource(R.string.receipt_taxed_note), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
        }
    }
}

@Composable
private fun ReceiptTotal(label: String, value: String, strong: Boolean = false) {
    val colors = Crate.colors
    val style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(label, style = style, color = if (strong) colors.ink else colors.inkMuted, modifier = Modifier.weight(1f))
        Text(value, style = style.tabularFigures(), color = colors.ink)
    }
}

@Composable
private fun RestockNudge(product: ProductEntity, onRestock: () -> Unit) {
    val colors = Crate.colors
    CrateCard(onClick = onRestock, color = colors.warningCard, borderColor = colors.warningCardLine) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(CrateIcons.Alert, contentDescription = null, tint = colors.onWarning, modifier = Modifier.size(22.dp))
            Text(
                if (product.status == StockStatus.OUT) stringResource(R.string.receipt_out, product.name)
                else stringResource(R.string.receipt_low, product.name, quantityWithUnit(product.stockMilli, product.soldBy)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.receipt_add_restock), style = MaterialTheme.typography.titleSmall, color = colors.onWarning)
        }
    }
}

/** The receipt as plain text, for texting or emailing it to the customer. */
@Composable
private fun receiptText(sale: SaleWithLines, storeName: String): String {
    val rate = BigDecimal.valueOf(sale.sale.taxRateBasisPoints.toLong(), 2).stripTrailingZeros().toPlainString()
    val header = listOf(storeName, stringResource(R.string.receipt_title, sale.sale.number), dateTimeOf(sale.sale.createdAt), "")
    val lines = sale.lines.map { line ->
        val quantity = quantityWithUnit(line.quantityMilli, line.soldBy)
        val mark = if (line.taxable) " " + stringResource(R.string.receipt_taxed_mark) else ""
        "${line.name}  $quantity  ${Money.format(line.lineTotalCents)}$mark"
    }
    val totals = listOf(
        "",
        "${stringResource(R.string.sale_subtotal)}  ${Money.format(sale.sale.subtotalCents)}",
        "${stringResource(R.string.sale_tax, rate)}  ${Money.format(sale.sale.taxCents)}",
        "${stringResource(R.string.sale_total)}  ${Money.format(sale.sale.totalCents)}",
        "${stringResource(R.string.receipt_payment)}  ${stringResource(sale.sale.payment.label)}",
    )
    val cash = sale.sale.cashGivenCents?.let { given ->
        listOf(
            "${stringResource(R.string.receipt_cash_given)}  ${Money.format(given)}",
            "${stringResource(R.string.receipt_change)}  ${Money.format(given - sale.sale.totalCents)}",
        )
    }.orEmpty()
    return (header + lines + totals + cash + listOf("", stringResource(R.string.receipt_thanks))).joinToString("\n")
}

@Composable
private fun timeOf(millis: Long): String {
    val locale = LocalLocale.current.platformLocale
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}

@Composable
private fun dateTimeOf(millis: Long): String {
    val locale = LocalLocale.current.platformLocale
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
}
