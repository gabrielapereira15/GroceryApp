package com.example.gpgrocery.ui.activity

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.RestockEntity
import com.example.gpgrocery.data.db.SaleEntity
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.EmptyState
import com.example.gpgrocery.ui.components.GroupLabel
import com.example.gpgrocery.ui.components.RowDivider
import com.example.gpgrocery.ui.components.ScreenHeader
import com.example.gpgrocery.ui.components.SegmentedControl
import com.example.gpgrocery.ui.components.StatCard
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class ActivityFilter { ALL, SALES, RESTOCKS }

sealed interface ActivityItem {
    val at: Long

    data class Sold(val sale: SaleEntity) : ActivityItem {
        override val at get() = sale.createdAt
    }

    data class Delivered(val restock: RestockEntity) : ActivityItem {
        override val at get() = restock.receivedAt
    }
}

data class ActivityDay(val date: LocalDate, val items: List<ActivityItem>)

data class ActivityState(
    val loading: Boolean = true,
    val filter: ActivityFilter = ActivityFilter.ALL,
    val days: List<ActivityDay> = emptyList(),
    val salesTodayCents: Long = 0,
    val restockedTodayCents: Long = 0,
    val today: LocalDate = LocalDate.now(),
)

class ActivityViewModel(container: AppContainer) : ViewModel() {
    private val zone: ZoneId = container.clock.zone
    private val today = LocalDate.now(container.clock)
    private val from = today.minusDays(29).atStartOfDay(zone).toInstant().toEpochMilli()
    private val to = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    private val filter = MutableStateFlow(ActivityFilter.ALL)

    val state: StateFlow<ActivityState> = combine(
        container.sales.salesBetween(from, to),
        container.restocks.restocksBetween(from, to),
        filter,
    ) { sales, restocks, chosen ->
        val items = buildList<ActivityItem> {
            if (chosen != ActivityFilter.RESTOCKS) sales.forEach { add(ActivityItem.Sold(it)) }
            if (chosen != ActivityFilter.SALES) restocks.forEach { add(ActivityItem.Delivered(it)) }
        }.sortedByDescending { it.at }
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        ActivityState(
            loading = false,
            filter = chosen,
            days = items.groupBy { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }
                .map { (date, dayItems) -> ActivityDay(date, dayItems) },
            salesTodayCents = sales.filter { it.createdAt >= startOfToday }.sumOf { it.totalCents },
            restockedTodayCents = restocks.filter { it.receivedAt >= startOfToday }.sumOf { it.totalCostCents },
            today = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityState())

    fun show(chosen: ActivityFilter) {
        filter.value = chosen
    }
}

@Composable
fun ActivityScreen(onOpenSale: (Long) -> Unit, onOpenDelivery: (Long) -> Unit) {
    val viewModel = crateViewModel { ActivityViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.activity_title), subtitle = stringResource(R.string.activity_subtitle)) }
        item {
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.activity_all),
                    stringResource(R.string.activity_sales),
                    stringResource(R.string.activity_restocks),
                ),
                selectedIndex = state.filter.ordinal,
                onSelect = { viewModel.show(ActivityFilter.entries[it]) },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(stringResource(R.string.activity_sales_today), Money.format(state.salesTodayCents), Modifier.weight(1f))
                StatCard(stringResource(R.string.activity_restocked_today), Money.format(state.restockedTodayCents), Modifier.weight(1f))
            }
        }
        if (!state.loading && state.days.isEmpty()) {
            item {
                EmptyState(
                    icon = CrateIcons.Receipt,
                    title = stringResource(R.string.activity_empty_title),
                    body = stringResource(R.string.activity_empty_body),
                )
            }
        }
        state.days.forEach { day ->
            item(key = "label-${day.date}") { GroupLabel(dayLabel(day.date, state.today)) }
            item(key = "day-${day.date}") {
                CrateCard {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                        day.items.forEachIndexed { index, item ->
                            when (item) {
                                is ActivityItem.Sold -> SaleRow(item.sale) { onOpenSale(item.sale.id) }
                                is ActivityItem.Delivered -> RestockRow(item.restock) { onOpenDelivery(item.restock.id) }
                            }
                            if (index < day.items.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: SaleEntity, onClick: () -> Unit) {
    val colors = Crate.colors
    val tile = colors.category(Category.PRODUCE)
    ActivityRow(
        icon = CrateIcons.Receipt,
        tileColor = tile.container,
        iconColor = tile.content,
        title = stringResource(R.string.activity_sale, sale.number.toInt()),
        meta = pluralStringResource(R.plurals.activity_sale_meta, sale.itemCount, sale.itemCount, stringResource(sale.payment.label), timeLabel(sale.createdAt)),
        amount = Money.format(sale.totalCents),
        amountColor = colors.ink,
        onClick = onClick,
    )
}

@Composable
private fun RestockRow(restock: RestockEntity, onClick: () -> Unit) {
    val colors = Crate.colors
    val tile = colors.category(Category.DRINKS)
    ActivityRow(
        icon = CrateIcons.Truck,
        tileColor = tile.container,
        iconColor = tile.content,
        title = restock.supplierName,
        meta = pluralStringResource(R.plurals.activity_restock_meta, restock.lineCount, restock.lineCount, timeLabel(restock.receivedAt)),
        amount = Money.format(restock.totalCostCents),
        amountColor = tile.content,
        onClick = onClick,
    )
}

@Composable
private fun ActivityRow(
    icon: ImageVector,
    tileColor: Color,
    iconColor: Color,
    title: String,
    meta: String,
    amount: String,
    amountColor: Color,
    onClick: () -> Unit,
) {
    val colors = Crate.colors
    Surface(onClick = onClick, color = colors.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(13.dp), color = tileColor, modifier = Modifier.size(42.dp)) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.ink, maxLines = 1)
                Text(meta, style = MaterialTheme.typography.bodySmall.tabularFigures(), color = colors.inkMuted, maxLines = 1)
            }
            Text(amount, style = MaterialTheme.typography.titleSmall.tabularFigures(), color = amountColor)
        }
    }
}

@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return when (date) {
        today -> stringResource(R.string.day_today)
        today.minusDays(1) -> stringResource(R.string.day_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    }
}

@Composable
fun timeLabel(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}
