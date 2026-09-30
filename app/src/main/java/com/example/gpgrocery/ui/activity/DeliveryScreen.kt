package com.example.gpgrocery.ui.activity

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.RestockWithLines
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.TopBar
import com.example.gpgrocery.ui.components.quantityWithUnit
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.components.unitPrice
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class DeliveryViewModel(container: AppContainer, restockId: Long) : ViewModel() {
    val delivery: StateFlow<Pair<Boolean, RestockWithLines?>> = container.restocks.restock(restockId)
        .map { true to it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false to null)
}

/** A delivery as it was received: what came, at what cost. */
@Composable
fun DeliveryScreen(restockId: Long, onBack: () -> Unit) {
    val viewModel = crateViewModel(key = "delivery-$restockId") { DeliveryViewModel(it, restockId) }
    val loaded by viewModel.delivery.collectAsStateWithLifecycle()
    val (ready, delivery) = loaded
    val colors = Crate.colors
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopBar(
            title = stringResource(R.string.delivery_title),
            navigationIcon = CrateIcons.ChevronLeft,
            navigationDescription = stringResource(R.string.action_back),
            onNavigate = onBack,
        )
        if (delivery == null) {
            if (ready) Text(stringResource(R.string.delivery_missing), color = colors.inkMuted, modifier = Modifier.padding(20.dp))
            return@Column
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            CrateCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(delivery.restock.supplierName, style = MaterialTheme.typography.titleLarge, color = colors.ink)
                    val received = Instant.ofEpochMilli(delivery.restock.receivedAt).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
                    Text(
                        listOfNotNull(delivery.restock.invoiceNumber?.let { stringResource(R.string.delivery_invoice, it) }, received).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                    )
                    delivery.lines.forEach { line ->
                        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                            Column(Modifier.weight(1f)) {
                                Text(line.name, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                                Text(
                                    "${quantityWithUnit(line.quantityMilli, line.soldBy)} × ${unitPrice(line.unitCostCents, line.soldBy)}",
                                    style = MaterialTheme.typography.bodySmall.tabularFigures(),
                                    color = colors.inkMuted,
                                )
                            }
                            Text(Money.format(line.lineTotalCents), style = MaterialTheme.typography.bodyMedium.tabularFigures(), color = colors.ink)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.delivery_total), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                        Text(Money.format(delivery.restock.totalCostCents), style = MaterialTheme.typography.titleMedium.tabularFigures(), color = colors.ink)
                    }
                }
            }
        }
    }
}
